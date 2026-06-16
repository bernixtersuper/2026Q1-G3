package com.menudigital.domain.analytics;

import java.math.BigDecimal;
import java.util.List;

public record AnalyticsSummaryResponse(
    String period,
    long ordersToday,
    long ordersYesterday,
    BigDecimal revenueToday,
    BigDecimal avgTicket,
    long menuViewsToday,
    long menuViewsYesterday,
    Double conversionRate,
    ConversionStatus conversionStatus,
    String conversionNote,
    int activeTables,
    Integer peakHourToday
) {}
