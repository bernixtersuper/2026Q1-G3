package com.menudigital.application.analytics;

import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.analytics.AnalyticsDashboardResponse;
import com.menudigital.domain.analytics.AnalyticsDashboardResponse.*;
import com.menudigital.domain.analytics.AnalyticsRepository;
import com.menudigital.domain.analytics.EventType;
import com.menudigital.domain.analytics.InteractionEvent;
import com.menudigital.domain.menu.MenuItem;
import com.menudigital.domain.menu.MenuRepository;
import com.menudigital.domain.menu.MenuSection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class GetMenuAnalyticsUseCase {
    
    @Inject
    AnalyticsRepository analyticsRepository;
    
    @Inject
    MenuRepository menuRepository;
    
    @Inject
    TenantContext tenantContext;
    
    public AnalyticsDashboardResponse execute() {
        String tenantId = tenantContext.getTenantId().toString();
        LocalDate today = LocalDate.now();
        Instant thirtyDaysAgo = today.minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant now = Instant.now();
        Instant todayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        List<InteractionEvent> allEvents = analyticsRepository.findByTenantAndPeriod(tenantId, thirtyDaysAgo, now);
        
        long totalMenuViewsLast30Days = countByEventType(allEvents, EventType.MENU_VIEW);
        
        long totalMenuViewsToday = allEvents.stream()
            .filter(e -> e.eventType() == EventType.MENU_VIEW)
            .filter(e -> e.timestamp().isAfter(todayStart))
            .count();
        
        long uniqueSessionsLast30Days = allEvents.stream()
            .map(InteractionEvent::sessionId)
            .distinct()
            .count();
        
        double avgSessionDepth = calculateAvgSessionDepth(allEvents);
        
        List<DailyViewCount> dailyViews = calculateDailyViews(allEvents, today);
        
        Map<String, Map<Integer, Long>> hourlyHeatmap = calculateHourlyHeatmap(allEvents);
        
        Map<String, Long> filterUsage = calculateFilterUsage(allEvents);
        
        int peakHourOfDay = findPeakHour(allEvents);
        String peakDayOfWeek = findPeakDayOfWeek(allEvents);
        
        List<ItemAnalytics> topItems = calculateTopItems(allEvents, tenantId, totalMenuViewsLast30Days, thirtyDaysAgo, now);
        
        List<SectionAnalytics> sectionEngagement = calculateSectionEngagement(allEvents);
        
        return new AnalyticsDashboardResponse(
            totalMenuViewsLast30Days,
            totalMenuViewsToday,
            uniqueSessionsLast30Days,
            avgSessionDepth,
            dailyViews,
            hourlyHeatmap,
            topItems,
            sectionEngagement,
            filterUsage,
            peakHourOfDay,
            peakDayOfWeek
        );
    }
    
    private long countByEventType(List<InteractionEvent> events, EventType type) {
        return events.stream().filter(e -> e.eventType() == type).count();
    }
    
    private double calculateAvgSessionDepth(List<InteractionEvent> events) {
        Map<String, Set<String>> sessionToItems = new HashMap<>();
        
        for (InteractionEvent event : events) {
            if (event.itemId() != null && !event.itemId().isBlank()) {
                sessionToItems
                    .computeIfAbsent(event.sessionId(), k -> new HashSet<>())
                    .add(event.itemId());
            }
        }
        
        if (sessionToItems.isEmpty()) {
            return 0.0;
        }
        
        double totalDepth = sessionToItems.values().stream()
            .mapToInt(Set::size)
            .sum();
        
        return Math.round(totalDepth / sessionToItems.size() * 10.0) / 10.0;
    }
    
    private List<DailyViewCount> calculateDailyViews(List<InteractionEvent> events, LocalDate today) {
        Map<LocalDate, Long> menuViewsByDate = new HashMap<>();
        Map<LocalDate, Long> itemViewsByDate = new HashMap<>();
        
        for (InteractionEvent event : events) {
            LocalDate date = event.timestamp().atZone(ZoneId.systemDefault()).toLocalDate();
            if (event.eventType() == EventType.MENU_VIEW) {
                menuViewsByDate.merge(date, 1L, Long::sum);
            } else if (event.eventType() == EventType.ITEM_VIEW) {
                itemViewsByDate.merge(date, 1L, Long::sum);
            }
        }
        
        List<DailyViewCount> result = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            result.add(new DailyViewCount(
                date,
                menuViewsByDate.getOrDefault(date, 0L),
                itemViewsByDate.getOrDefault(date, 0L)
            ));
        }
        
        return result;
    }
    
    private Map<String, Map<Integer, Long>> calculateHourlyHeatmap(List<InteractionEvent> events) {
        Map<String, Map<Integer, Long>> heatmap = new LinkedHashMap<>();
        String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
        
        for (String day : days) {
            Map<Integer, Long> hourMap = new LinkedHashMap<>();
            for (int h = 0; h < 24; h++) {
                hourMap.put(h, 0L);
            }
            heatmap.put(day, hourMap);
        }
        
        for (InteractionEvent event : events) {
            ZonedDateTime zdt = event.timestamp().atZone(ZoneId.systemDefault());
            String dayOfWeek = zdt.getDayOfWeek().name();
            int hour = zdt.getHour();
            
            heatmap.get(dayOfWeek).merge(hour, 1L, Long::sum);
        }
        
        return heatmap;
    }
    
    private Map<String, Long> calculateFilterUsage(List<InteractionEvent> events) {
        return events.stream()
            .filter(e -> e.eventType() == EventType.FILTER_USED)
            .filter(e -> e.metadata() != null && e.metadata().containsKey("filter"))
            .collect(Collectors.groupingBy(
                e -> e.metadata().get("filter"),
                Collectors.counting()
            ));
    }
    
    private int findPeakHour(List<InteractionEvent> events) {
        Map<Integer, Long> hourCounts = events.stream()
            .collect(Collectors.groupingBy(
                e -> e.timestamp().atZone(ZoneId.systemDefault()).getHour(),
                Collectors.counting()
            ));
        
        return hourCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(12);
    }
    
    private String findPeakDayOfWeek(List<InteractionEvent> events) {
        Map<DayOfWeek, Long> dayCounts = events.stream()
            .collect(Collectors.groupingBy(
                e -> e.timestamp().atZone(ZoneId.systemDefault()).getDayOfWeek(),
                Collectors.counting()
            ));
        
        return dayCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(e -> e.getKey().name())
            .orElse("SATURDAY");
    }
    
    private List<ItemAnalytics> calculateTopItems(
            List<InteractionEvent> events, 
            String tenantId,
            long totalMenuViews,
            Instant thirtyDaysAgo,
            Instant now
    ) {
        Map<String, Long> itemViewCounts = events.stream()
            .filter(e -> e.eventType() == EventType.ITEM_VIEW)
            .filter(e -> e.itemId() != null && !e.itemId().isBlank())
            .collect(Collectors.groupingBy(InteractionEvent::itemId, Collectors.counting()));
        
        List<Map.Entry<String, Long>> topItemEntries = itemViewCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .toList();
        
        if (topItemEntries.isEmpty()) {
            return List.of();
        }
        
        List<UUID> itemIds = topItemEntries.stream()
            .map(e -> UUID.fromString(e.getKey()))
            .toList();
        
        Map<String, MenuItem> itemsById = menuRepository.findItemsByIds(itemIds).stream()
            .collect(Collectors.toMap(
                item -> item.getId().toString(),
                Function.identity()
            ));
        
        Instant sevenDaysAgo = now.minus(Duration.ofDays(7));
        Instant fourteenDaysAgo = now.minus(Duration.ofDays(14));
        
        Map<String, Long> last7dCounts = events.stream()
            .filter(e -> e.eventType() == EventType.ITEM_VIEW)
            .filter(e -> e.itemId() != null)
            .filter(e -> e.timestamp().isAfter(sevenDaysAgo))
            .collect(Collectors.groupingBy(InteractionEvent::itemId, Collectors.counting()));
        
        Map<String, Long> prior7dCounts = events.stream()
            .filter(e -> e.eventType() == EventType.ITEM_VIEW)
            .filter(e -> e.itemId() != null)
            .filter(e -> e.timestamp().isAfter(fourteenDaysAgo) && e.timestamp().isBefore(sevenDaysAgo))
            .collect(Collectors.groupingBy(InteractionEvent::itemId, Collectors.counting()));
        
        return topItemEntries.stream()
            .map(entry -> {
                String itemId = entry.getKey();
                long viewCount = entry.getValue();
                MenuItem item = itemsById.get(itemId);
                String itemName = item != null ? item.getName() : "Unknown Item";
                
                double viewRate = totalMenuViews > 0 ? (double) viewCount / totalMenuViews : 0.0;
                
                long last7d = last7dCounts.getOrDefault(itemId, 0L);
                long prior7d = prior7dCounts.getOrDefault(itemId, 0L);
                boolean trending = prior7d > 0 && last7d > prior7d * 1.5;
                
                return new ItemAnalytics(itemId, itemName, viewCount, viewRate, trending);
            })
            .toList();
    }
    
    private List<SectionAnalytics> calculateSectionEngagement(List<InteractionEvent> events) {
        Map<String, Long> sectionViewCounts = events.stream()
            .filter(e -> e.eventType() == EventType.SECTION_VIEW)
            .filter(e -> e.sectionId() != null && !e.sectionId().isBlank())
            .collect(Collectors.groupingBy(InteractionEvent::sectionId, Collectors.counting()));
        
        var menu = menuRepository.findByTenantId(tenantContext.getTenantId());
        Map<String, String> sectionNames = menu.getSections().stream()
            .collect(Collectors.toMap(
                s -> s.getId().toString(),
                MenuSection::getName
            ));
        
        return sectionViewCounts.entrySet().stream()
            .map(entry -> new SectionAnalytics(
                entry.getKey(),
                sectionNames.getOrDefault(entry.getKey(), "Unknown Section"),
                entry.getValue()
            ))
            .sorted((a, b) -> Long.compare(b.viewCount(), a.viewCount()))
            .toList();
    }
}
