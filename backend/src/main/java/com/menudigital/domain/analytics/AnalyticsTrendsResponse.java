package com.menudigital.domain.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AnalyticsTrendsResponse(
    int days,
    List<DailyTrendPoint> series,
    Map<String, Long> filterUsage,
    List<SectionTrendPoint> sectionEngagement,
    long totalOrders,
    BigDecimal totalRevenue
) {
    public record DailyTrendPoint(
        LocalDate date,
        long orders,
        BigDecimal revenue,
        long menuViews,
        long itemViews,
        Long uniqueMenuSessions,
        Double conversionRate,
        ConversionStatus conversionStatus
    ) {}

    public record SectionTrendPoint(
        String sectionId,
        String sectionName,
        long viewCount
    ) {}
}
