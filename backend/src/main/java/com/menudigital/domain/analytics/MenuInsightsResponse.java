package com.menudigital.domain.analytics;

import java.math.BigDecimal;
import java.util.List;

/**
 * Owner-facing menu insights derived from transactional order data:
 * what gets bought together, menu engineering classification, and modifier demand.
 */
public record MenuInsightsResponse(
    int days,
    long ordersAnalyzed,
    double avgBasketSize,
    long distinctItemsSold,
    List<ItemPairRow> frequentlyBoughtTogether,
    MenuEngineering menuEngineering,
    List<ModifierRow> topModifiers
) {
    /**
     * A pair of items frequently ordered together.
     * support = share of orders containing both items.
     * lift > 1 means the two items appear together more often than chance would predict.
     */
    public record ItemPairRow(
        String itemAId,
        String itemAName,
        String itemBId,
        String itemBName,
        long coOccurrenceCount,
        double support,
        double lift
    ) {}

    /**
     * Kasavana–Smith menu engineering. Items are classified against the menu averages:
     * popularity (units sold) and profitability (revenue, used as a margin proxy).
     * STAR = popular & profitable, PLOWHORSE = popular & low profit,
     * PUZZLE = unpopular & profitable, DOG = unpopular & low profit.
     */
    public record MenuEngineering(
        double avgQuantity,
        BigDecimal avgRevenue,
        List<MenuItemClass> items
    ) {
        public record MenuItemClass(
            String itemId,
            String itemName,
            long quantitySold,
            BigDecimal revenue,
            String classification
        ) {}
    }

    public record ModifierRow(
        String modifierName,
        long timesSelected,
        BigDecimal revenue
    ) {}
}
