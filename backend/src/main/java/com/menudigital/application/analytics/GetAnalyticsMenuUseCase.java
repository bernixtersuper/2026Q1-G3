package com.menudigital.application.analytics;

import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.analytics.*;
import com.menudigital.domain.menu.MenuItem;
import com.menudigital.domain.menu.MenuRepository;
import com.menudigital.domain.tenant.TenantId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class GetAnalyticsMenuUseCase {

    @Inject
    AnalyticsAggregateReadRepository aggregateReadRepository;

    @Inject
    OrderAnalyticsRepository orderAnalyticsRepository;

    @Inject
    MenuRepository menuRepository;

    @Inject
    TenantContext tenantContext;

    private static final int TOP_SOLD_LIMIT = 10;
    private static final int MATRIX_SOLD_LIMIT = 20;

    public AnalyticsMenuResponse execute() {
        return execute(SalesPeriod.TODAY);
    }

    public AnalyticsMenuResponse execute(SalesPeriod salesPeriod) {
        TenantId tenantId = tenantContext.getTenantId();
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        InstantRange salesRange = salesRange(salesPeriod, zone, today);

        var itemNames = buildItemNameMap(tenantId);

        List<OrderAnalyticsRepository.TopSoldItem> soldRows = orderAnalyticsRepository
            .topSoldItems(tenantId, salesRange.from(), salesRange.to(), MATRIX_SOLD_LIMIT);

        List<AnalyticsMenuResponse.TopSoldItemRow> topSold = soldRows.stream()
            .limit(TOP_SOLD_LIMIT)
            .map(s -> new AnalyticsMenuResponse.TopSoldItemRow(
                s.menuItemId(),
                itemNames.getOrDefault(s.menuItemId(), s.itemName()),
                s.quantitySold(),
                s.revenue()
            ))
            .toList();

        Map<String, Long> viewsByItem = aggregateReadRepository.queryItems(tenantId.toString()).stream()
            .collect(Collectors.toMap(
                AnalyticsAggregateReadRepository.ItemAggregate::itemId,
                AnalyticsAggregateReadRepository.ItemAggregate::views,
                Long::max
            ));

        List<AnalyticsMenuResponse.TopViewedItemRow> topViewed = buildTopViewed(
            tenantId, today, itemNames, viewsByItem);

        Map<String, Long> soldByItem = soldRows.stream()
            .collect(Collectors.toMap(
                OrderAnalyticsRepository.TopSoldItem::menuItemId,
                OrderAnalyticsRepository.TopSoldItem::quantitySold,
                Long::sum
            ));

        Set<String> allItemIds = new HashSet<>();
        topViewed.forEach(v -> allItemIds.add(v.itemId()));
        soldByItem.keySet().forEach(allItemIds::add);

        List<AnalyticsMenuResponse.ViewedVsSoldRow> matrix = allItemIds.stream()
            .map(id -> new AnalyticsMenuResponse.ViewedVsSoldRow(
                id,
                itemNames.getOrDefault(id, "Unknown"),
                viewsByItem.getOrDefault(id, 0L),
                soldByItem.getOrDefault(id, 0L)
            ))
            .sorted(Comparator.comparingLong(AnalyticsMenuResponse.ViewedVsSoldRow::viewCount).reversed())
            .limit(20)
            .toList();

        return new AnalyticsMenuResponse(salesPeriod.paramValue(), topSold, topViewed, matrix);
    }

    private record InstantRange(Instant from, Instant to) {}

    private InstantRange salesRange(SalesPeriod period, ZoneId zone, LocalDate today) {
        Instant to = today.plusDays(1).atStartOfDay(zone).toInstant();
        Instant from = switch (period) {
            case TODAY -> today.atStartOfDay(zone).toInstant();
            case DAYS_30 -> today.minusDays(29).atStartOfDay(zone).toInstant();
            case ALL_TIME -> Instant.EPOCH;
        };
        return new InstantRange(from, to);
    }

    /**
     * Ranking por ítem: primero acumulado {@code ITEM#views}; si no hay, usa {@code DAY#.topItemIds}
     * del día (p. ej. tras seed de demo sin filas ITEM#).
     */
    private List<AnalyticsMenuResponse.TopViewedItemRow> buildTopViewed(
            TenantId tenantId,
            LocalDate today,
            Map<String, String> itemNames,
            Map<String, Long> viewsByItem) {

        List<AnalyticsMenuResponse.TopViewedItemRow> fromItems = viewsByItem.entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .map(e -> new AnalyticsMenuResponse.TopViewedItemRow(
                e.getKey(),
                itemNames.getOrDefault(e.getKey(), "Unknown"),
                e.getValue()))
            .toList();

        if (!fromItems.isEmpty()) {
            return fromItems;
        }

        return aggregateReadRepository.getDay(tenantId.toString(), today)
            .filter(day -> !day.topItemIds().isEmpty())
            .map(day -> day.topItemIds().stream()
                .limit(10)
                .map(id -> new AnalyticsMenuResponse.TopViewedItemRow(
                    id,
                    itemNames.getOrDefault(id, "Unknown"),
                    viewsByItem.getOrDefault(id, 0L)))
                .filter(row -> row.viewCount() > 0)
                .toList())
            .filter(rows -> !rows.isEmpty())
            .orElseGet(() -> aggregateReadRepository.getDay(tenantId.toString(), today)
                .filter(day -> !day.topItemIds().isEmpty() && day.itemViews() > 0)
                .map(day -> {
                    long estimate = Math.max(1L, day.itemViews() / day.topItemIds().size());
                    return day.topItemIds().stream()
                        .limit(10)
                        .map(id -> new AnalyticsMenuResponse.TopViewedItemRow(
                            id,
                            itemNames.getOrDefault(id, "Unknown"),
                            estimate))
                        .toList();
                })
                .orElse(List.of()));
    }

    private Map<String, String> buildItemNameMap(TenantId tenantId) {
        Map<String, String> names = new HashMap<>();
        var menu = menuRepository.findByTenantId(tenantId);
        for (var section : menu.getSections()) {
            for (MenuItem item : section.getItems()) {
                names.put(item.getId().toString(), item.getName());
            }
        }
        return names;
    }
}
