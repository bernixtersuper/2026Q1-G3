package com.menudigital.application.service;

import com.menudigital.application.dto.AnalyticsDashboardDTO;
import com.menudigital.infrastructure.dynamodb.DynamoDBEventRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class AnalyticsService {

    @Inject
    DynamoDBEventRepository repository;

    public AnalyticsDashboardDTO getDashboardAnalytics(String tenantId, Instant from, Instant to) {
        List<Map<String, AttributeValue>> items = repository.getEventsForTenant(tenantId, from, to);

        int totalMenuViews = 0;
        int totalItemViews = 0;
        Map<String, Integer> itemCounts = new HashMap<>();
        Map<Integer, Integer> hourlyViews = new HashMap<>();

        for (Map<String, AttributeValue> item : items) {
            String evtType = item.get("eventType").s();
            String timestampStr = item.get("timestamp").s();
            Instant timestamp = Instant.parse(timestampStr);
            int hour = ZonedDateTime.ofInstant(timestamp, ZoneId.of("UTC")).getHour();

            if ("MENU_VIEW".equals(evtType)) {
                totalMenuViews++;
                hourlyViews.put(hour, hourlyViews.getOrDefault(hour, 0) + 1);
            } else if ("ITEM_VIEW".equals(evtType)) {
                totalItemViews++;
                String itemId = item.get("itemId").s();
                itemCounts.put(itemId, itemCounts.getOrDefault(itemId, 0) + 1);
            }
        }

        List<AnalyticsDashboardDTO.ItemVisit> topItems = itemCounts.entrySet().stream()
                .map(e -> new AnalyticsDashboardDTO.ItemVisit(e.getKey(), e.getValue()))
                .sorted((a, b) -> Integer.compare(b.views, a.views)) // desc
                .limit(5)
                .collect(Collectors.toList());

        AnalyticsDashboardDTO dto = new AnalyticsDashboardDTO();
        dto.totalItemViews = totalItemViews;
        dto.totalMenuViews = totalMenuViews;
        dto.topItems = topItems;
        dto.viewsByHour = hourlyViews;
        return dto;
    }
}
