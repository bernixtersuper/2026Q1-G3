package com.menudigital.application.analytics;

import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.analytics.MenuInsightsResponse;
import com.menudigital.domain.analytics.MenuInsightsResponse.ItemPairRow;
import com.menudigital.domain.analytics.MenuInsightsResponse.MenuEngineering;
import com.menudigital.domain.analytics.MenuInsightsResponse.MenuEngineering.MenuItemClass;
import com.menudigital.domain.analytics.MenuInsightsResponse.ModifierRow;
import com.menudigital.domain.analytics.OrderAnalyticsRepository;
import com.menudigital.domain.analytics.OrderAnalyticsRepository.ItemPairCount;
import com.menudigital.domain.analytics.OrderAnalyticsRepository.ItemSalesStat;
import com.menudigital.domain.tenant.TenantId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds the owner-facing "Menu Insights" panel from PostgreSQL order data:
 * frequently-bought-together pairs, menu-engineering classification and top modifiers.
 */
@ApplicationScoped
public class GetMenuInsightsUseCase {

    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 90;
    private static final int PAIR_LIMIT = 15;
    private static final int MODIFIER_LIMIT = 15;

    @Inject
    OrderAnalyticsRepository orderAnalyticsRepository;

    @Inject
    TenantContext tenantContext;

    public MenuInsightsResponse execute(int days) {
        int clampedDays = Math.max(MIN_DAYS, Math.min(MAX_DAYS, days));
        TenantId tenantId = tenantContext.getTenantId();

        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        Instant to = today.plusDays(1).atStartOfDay(zone).toInstant();
        Instant from = today.minusDays(clampedDays - 1L).atStartOfDay(zone).toInstant();

        List<ItemSalesStat> sales = orderAnalyticsRepository.itemSalesStats(tenantId, from, to);
        Map<String, ItemSalesStat> byId = sales.stream()
            .collect(Collectors.toMap(ItemSalesStat::menuItemId, Function.identity(), (a, b) -> a));

        var basket = orderAnalyticsRepository.basketSummary(tenantId, from, to);
        long ordersAnalyzed = basket.orders();
        double avgBasketSize = ordersAnalyzed == 0
            ? 0.0
            : round2((double) basket.totalUnits() / ordersAnalyzed);

        List<ItemPairRow> pairs = buildPairs(tenantId, from, to, byId, ordersAnalyzed);
        MenuEngineering menuEngineering = buildMenuEngineering(sales);
        List<ModifierRow> modifiers = orderAnalyticsRepository.topModifiers(tenantId, from, to, MODIFIER_LIMIT)
            .stream()
            .map(m -> new ModifierRow(m.modifierName(), m.timesSelected(), m.revenue()))
            .toList();

        return new MenuInsightsResponse(
            clampedDays,
            ordersAnalyzed,
            avgBasketSize,
            basket.distinctItems(),
            pairs,
            menuEngineering,
            modifiers
        );
    }

    private List<ItemPairRow> buildPairs(TenantId tenantId, Instant from, Instant to,
                                         Map<String, ItemSalesStat> byId, long ordersAnalyzed) {
        List<ItemPairCount> raw = orderAnalyticsRepository.frequentlyBoughtTogether(tenantId, from, to, PAIR_LIMIT);
        return raw.stream()
            .map(p -> {
                ItemSalesStat a = byId.get(p.itemAId());
                ItemSalesStat b = byId.get(p.itemBId());
                long ordersWithA = a != null ? a.ordersWithItem() : 0;
                long ordersWithB = b != null ? b.ordersWithItem() : 0;

                double support = ordersAnalyzed == 0
                    ? 0.0
                    : round4((double) p.coOccurrenceCount() / ordersAnalyzed);

                // lift = P(A and B) / (P(A) * P(B)); >1 indicates a real association.
                double lift = 0.0;
                if (ordersAnalyzed > 0 && ordersWithA > 0 && ordersWithB > 0) {
                    double expected = (double) ordersWithA * ordersWithB / ordersAnalyzed;
                    lift = expected > 0 ? round2(p.coOccurrenceCount() / expected) : 0.0;
                }

                return new ItemPairRow(
                    p.itemAId(),
                    a != null ? a.itemName() : "Unknown",
                    p.itemBId(),
                    b != null ? b.itemName() : "Unknown",
                    p.coOccurrenceCount(),
                    support,
                    lift
                );
            })
            .toList();
    }

    private MenuEngineering buildMenuEngineering(List<ItemSalesStat> sales) {
        if (sales.isEmpty()) {
            return new MenuEngineering(0.0, BigDecimal.ZERO, List.of());
        }

        double avgQuantity = sales.stream().mapToLong(ItemSalesStat::quantitySold).average().orElse(0.0);
        BigDecimal totalRevenue = sales.stream()
            .map(ItemSalesStat::revenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgRevenue = totalRevenue.divide(BigDecimal.valueOf(sales.size()), 2, RoundingMode.HALF_UP);

        List<MenuItemClass> items = sales.stream()
            .map(s -> new MenuItemClass(
                s.menuItemId(),
                s.itemName(),
                s.quantitySold(),
                s.revenue(),
                classify(s.quantitySold() >= avgQuantity, s.revenue().compareTo(avgRevenue) >= 0)
            ))
            .sorted((x, y) -> Long.compare(y.quantitySold(), x.quantitySold()))
            .toList();

        return new MenuEngineering(round2(avgQuantity), avgRevenue, items);
    }

    /** Renders the menu-insights payload as a multi-section CSV for download. */
    public String exportCsv(int days) {
        MenuInsightsResponse r = execute(days);
        StringBuilder sb = new StringBuilder();

        sb.append("# Menu Insights — últimos ").append(r.days()).append(" días\n");
        sb.append(csv("Pedidos analizados")).append(',').append(csv(String.valueOf(r.ordersAnalyzed()))).append('\n');
        sb.append(csv("Tamaño medio del pedido")).append(',').append(csv(String.valueOf(r.avgBasketSize()))).append('\n');
        sb.append(csv("Platos distintos vendidos")).append(',').append(csv(String.valueOf(r.distinctItemsSold()))).append('\n');

        sb.append("\n# Se piden juntos\n");
        sb.append("Plato A,Plato B,Pedidos juntos,Support,Lift\n");
        for (var p : r.frequentlyBoughtTogether()) {
            sb.append(csv(p.itemAName())).append(',')
              .append(csv(p.itemBName())).append(',')
              .append(p.coOccurrenceCount()).append(',')
              .append(p.support()).append(',')
              .append(p.lift()).append('\n');
        }

        sb.append("\n# Ingeniería de menú\n");
        sb.append("Plato,Clasificación,Unidades,Ingresos\n");
        for (var m : r.menuEngineering().items()) {
            sb.append(csv(m.itemName())).append(',')
              .append(m.classification()).append(',')
              .append(m.quantitySold()).append(',')
              .append(m.revenue()).append('\n');
        }

        sb.append("\n# Extras más pedidos\n");
        sb.append("Extra,Veces pedido,Ingresos extra\n");
        for (var mod : r.topModifiers()) {
            sb.append(csv(mod.modifierName())).append(',')
              .append(mod.timesSelected()).append(',')
              .append(mod.revenue()).append('\n');
        }

        return sb.toString();
    }

    /** Quote a CSV field if it contains a comma, quote or newline (RFC 4180). */
    private String csv(String value) {
        String v = value == null ? "" : value;
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
    }

    private String classify(boolean popular, boolean profitable) {
        if (popular && profitable) return "STAR";
        if (popular) return "PLOWHORSE";
        if (profitable) return "PUZZLE";
        return "DOG";
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double round4(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
