package com.menudigital.domain.analytics;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AnalyticsDashboardResponse(
    long totalMenuViewsLast30Days,
    long totalMenuViewsToday,
    long uniqueSessionsLast30Days,
    double avgSessionDepth,
    List<DailyViewCount> dailyViews,
    Map<String, Map<Integer, Long>> hourlyHeatmap,
    List<ItemAnalytics> topItems,
    List<SectionAnalytics> sectionEngagement,
    Map<String, Long> filterUsage,
    int peakHourOfDay,
    String peakDayOfWeek
) {
    public record DailyViewCount(LocalDate date, long menuViews, long itemViews) {}
    
    public record ItemAnalytics(
        String itemId,
        String itemName,
        long viewCount,
        double viewRate,
        boolean trending
    ) {}
    
    public record SectionAnalytics(
        String sectionId,
        String sectionName,
        long viewCount
    ) {}
}
