package com.menudigital.domain.analytics;

import java.util.Map;

public record AnalyticsOperationsResponse(
    Map<String, Map<Integer, Long>> ordersHeatmap,
    Map<String, Map<Integer, Long>> viewsHeatmap,
    Integer peakHourToday,
    int activeTables
) {}
