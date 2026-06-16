package com.menudigital.application.analytics;

import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.analytics.AnalyticsAggregateReadRepository;
import com.menudigital.domain.analytics.AnalyticsOperationsResponse;
import com.menudigital.domain.analytics.OrderAnalyticsRepository;
import com.menudigital.domain.tenant.TenantId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class GetAnalyticsOperationsUseCase {

    private static final String[] DAY_NAMES = {
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    @Inject
    AnalyticsAggregateReadRepository aggregateReadRepository;

    @Inject
    OrderAnalyticsRepository orderAnalyticsRepository;

    @Inject
    TenantContext tenantContext;

    public AnalyticsOperationsResponse execute() {
        TenantId tenantId = tenantContext.getTenantId();
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        Instant weekAgo = today.minusDays(7).atStartOfDay(zone).toInstant();
        Instant tomorrow = today.plusDays(1).atStartOfDay(zone).toInstant();
        Instant todayStart = today.atStartOfDay(zone).toInstant();

        Map<String, Map<Integer, Long>> ordersHeatmap = orderAnalyticsRepository.ordersHeatmap(tenantId, weekAgo, tomorrow);
        Map<String, Map<Integer, Long>> viewsHeatmap = buildViewsHeatmap(
            aggregateReadRepository.queryHours(tenantId.toString(), weekAgo, tomorrow));

        int activeTables = orderAnalyticsRepository.countActiveTables(tenantId);
        Integer peakHour = orderAnalyticsRepository.peakHourToday(tenantId, todayStart, tomorrow).orElse(null);

        return new AnalyticsOperationsResponse(ordersHeatmap, viewsHeatmap, peakHour, activeTables);
    }

    private Map<String, Map<Integer, Long>> buildViewsHeatmap(
            List<AnalyticsAggregateReadRepository.HourAggregate> hours) {
        Map<String, Map<Integer, Long>> heatmap = new LinkedHashMap<>();
        for (String day : DAY_NAMES) {
            heatmap.put(day, new HashMap<>());
        }

        for (var hour : hours) {
            var zdt = hour.bucketStart().atZone(ZoneId.systemDefault());
            String dayName = DAY_NAMES[zdt.getDayOfWeek().getValue() % 7];
            int hourOfDay = zdt.getHour();
            long total = hour.menuViews() + hour.itemViews();
            heatmap.get(dayName).merge(hourOfDay, total, Long::sum);
        }
        return heatmap;
    }
}
