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

    public AnalyticsMenuResponse execute() {
        TenantId tenantId = tenantContext.getTenantId();
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        Instant todayStart = today.atStartOfDay(zone).toInstant();
        Instant tomorrowStart = today.plusDays(1).atStartOfDay(zone).toInstant();

        var itemNames = buildItemNameMap(tenantId);

        List<AnalyticsMenuResponse.TopSoldItemRow> topSold = orderAnalyticsRepository
            .topSoldItems(tenantId, todayStart, tomorrowStart, 10)
            .stream()
            .map(s -> new AnalyticsMenuResponse.TopSoldItemRow(
                s.menuItemId(),
                itemNames.getOrDefault(s.menuItemId(), s.itemName()),
                s.quantitySold(),
                s.revenue()
            ))
            .toList();

        List<AnalyticsMenuResponse.TopViewedItemRow> topViewed = aggregateReadRepository
            .queryItems(tenantId.toString())
            .stream()
            .sorted(Comparator.comparingLong(AnalyticsAggregateReadRepository.ItemAggregate::views).reversed())
            .limit(10)
            .map(i -> new AnalyticsMenuResponse.TopViewedItemRow(
                i.itemId(),
                itemNames.getOrDefault(i.itemId(), "Unknown"),
                i.views()
            ))
            .toList();

        Map<String, Long> soldByItem = topSold.stream()
            .collect(Collectors.toMap(AnalyticsMenuResponse.TopSoldItemRow::itemId, AnalyticsMenuResponse.TopSoldItemRow::quantitySold, Long::sum));

        Set<String> allItemIds = new HashSet<>();
        topViewed.forEach(v -> allItemIds.add(v.itemId()));
        soldByItem.keySet().forEach(allItemIds::add);

        Map<String, Long> viewsByItem = aggregateReadRepository.queryItems(tenantId.toString()).stream()
            .collect(Collectors.toMap(AnalyticsAggregateReadRepository.ItemAggregate::itemId, AnalyticsAggregateReadRepository.ItemAggregate::views));

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

        return new AnalyticsMenuResponse(topSold, topViewed, matrix);
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
