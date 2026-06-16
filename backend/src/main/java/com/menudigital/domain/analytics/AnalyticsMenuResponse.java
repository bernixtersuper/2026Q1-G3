package com.menudigital.domain.analytics;

import java.math.BigDecimal;
import java.util.List;

public record AnalyticsMenuResponse(
    List<TopSoldItemRow> topSoldItems,
    List<TopViewedItemRow> topViewedItems,
    List<ViewedVsSoldRow> viewedVsSold
) {
    public record TopSoldItemRow(
        String itemId,
        String itemName,
        long quantitySold,
        BigDecimal revenue
    ) {}

    public record TopViewedItemRow(
        String itemId,
        String itemName,
        long viewCount
    ) {}

    public record ViewedVsSoldRow(
        String itemId,
        String itemName,
        long viewCount,
        long quantitySold
    ) {}
}
