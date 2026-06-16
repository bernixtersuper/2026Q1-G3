package com.menudigital.application.analytics;

import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.analytics.*;
import com.menudigital.domain.analytics.AnalyticsDashboardResponse.*;
import com.menudigital.domain.menu.MenuItem;
import com.menudigital.domain.menu.MenuRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @deprecated Prefer {@link GetAnalyticsSummaryUseCase}, {@link GetAnalyticsMenuUseCase},
 *             {@link GetAnalyticsOperationsUseCase}. Reads Dynamo aggregates, not raw events.
 */
@ApplicationScoped
@Deprecated
public class GetMenuAnalyticsUseCase {

    private static final String[] DAY_NAMES = {
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    };

    @Inject
    AnalyticsAggregateReadRepository aggregateReadRepository;

    @Inject
    MenuRepository menuRepository;

    @Inject
    TenantContext tenantContext;

    public AnalyticsDashboardResponse execute() {
        String tenantId = tenantContext.getTenantId().toString();
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate fromDate = today.minusDays(29);

        var days = aggregateReadRepository.queryDays(tenantId, fromDate, today);
        Map<LocalDate, AnalyticsAggregateReadRepository.DayAggregate> dayMap = days.stream()
            .collect(Collectors.toMap(AnalyticsAggregateReadRepository.DayAggregate::date, d -> d));

        long totalMenuViewsLast30Days = days.stream().mapToLong(AnalyticsAggregateReadRepository.DayAggregate::menuViews).sum();
        long totalMenuViewsToday = dayMap.getOrDefault(today, emptyDay(today)).menuViews();

        var hours = aggregateReadRepository.queryHours(
            tenantId,
            today.minusDays(7).atStartOfDay(zone).toInstant(),
            today.plusDays(1).atStartOfDay(zone).toInstant()
        );

        List<DailyViewCount> dailyViews = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            var d = dayMap.getOrDefault(date, emptyDay(date));
            dailyViews.add(new DailyViewCount(date, d.menuViews(), d.itemViews()));
        }

        Map<String, Map<Integer, Long>> hourlyHeatmap = buildViewsHeatmap(hours);
        int peakHourOfDay = findPeakHour(hours);
        String peakDayOfWeek = findPeakDay(hours);

        List<ItemAnalytics> topItems = buildTopItems(tenantId, totalMenuViewsLast30Days);

        return new AnalyticsDashboardResponse(
            totalMenuViewsLast30Days,
            totalMenuViewsToday,
            0L,
            0.0,
            dailyViews,
            hourlyHeatmap,
            topItems,
            List.of(),
            Map.of(),
            peakHourOfDay,
            peakDayOfWeek
        );
    }

    private AnalyticsAggregateReadRepository.DayAggregate emptyDay(LocalDate date) {
        return new AnalyticsAggregateReadRepository.DayAggregate(date, 0, 0, 0, null, null);
    }

    private Map<String, Map<Integer, Long>> buildViewsHeatmap(
            List<AnalyticsAggregateReadRepository.HourAggregate> hours) {
        Map<String, Map<Integer, Long>> heatmap = new LinkedHashMap<>();
        for (String day : DAY_NAMES) {
            Map<Integer, Long> hourMap = new LinkedHashMap<>();
            for (int h = 0; h < 24; h++) hourMap.put(h, 0L);
            heatmap.put(day, hourMap);
        }
        for (var hour : hours) {
            var zdt = hour.bucketStart().atZone(ZoneId.systemDefault());
            String dayName = zdt.getDayOfWeek().name();
            heatmap.get(dayName).merge(zdt.getHour(), hour.menuViews() + hour.itemViews(), Long::sum);
        }
        return heatmap;
    }

    private int findPeakHour(List<AnalyticsAggregateReadRepository.HourAggregate> hours) {
        return hours.stream()
            .collect(Collectors.groupingBy(
                h -> h.bucketStart().atZone(ZoneId.systemDefault()).getHour(),
                Collectors.summingLong(h -> h.menuViews() + h.itemViews())
            ))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(12);
    }

    private String findPeakDay(List<AnalyticsAggregateReadRepository.HourAggregate> hours) {
        return hours.stream()
            .collect(Collectors.groupingBy(
                h -> h.bucketStart().atZone(ZoneId.systemDefault()).getDayOfWeek(),
                Collectors.summingLong(h -> h.menuViews() + h.itemViews())
            ))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(e -> e.getKey().name())
            .orElse("SATURDAY");
    }

    private List<ItemAnalytics> buildTopItems(String tenantId, long totalMenuViews) {
        var items = aggregateReadRepository.queryItems(tenantId).stream()
            .sorted(Comparator.comparingLong(AnalyticsAggregateReadRepository.ItemAggregate::views).reversed())
            .limit(10)
            .toList();

        if (items.isEmpty()) return List.of();

        List<UUID> itemIds = items.stream()
            .map(i -> UUID.fromString(i.itemId()))
            .toList();
        Map<String, MenuItem> itemsById = menuRepository.findItemsByIds(itemIds).stream()
            .collect(Collectors.toMap(i -> i.getId().toString(), i -> i));

        return items.stream()
            .map(i -> {
                MenuItem item = itemsById.get(i.itemId());
                String name = item != null ? item.getName() : "Unknown Item";
                double viewRate = totalMenuViews > 0 ? (double) i.views() / totalMenuViews : 0.0;
                return new ItemAnalytics(i.itemId(), name, i.views(), viewRate, false);
            })
            .toList();
    }
}
