package com.menudigital.application.analytics;

import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.analytics.MenuInsightsResponse;
import com.menudigital.domain.analytics.MenuInsightsResponse.MenuEngineering.MenuItemClass;
import com.menudigital.domain.analytics.OrderAnalyticsRepository;
import com.menudigital.domain.analytics.OrderAnalyticsRepository.BasketSummary;
import com.menudigital.domain.analytics.OrderAnalyticsRepository.ItemPairCount;
import com.menudigital.domain.analytics.OrderAnalyticsRepository.ItemSalesStat;
import com.menudigital.domain.analytics.OrderAnalyticsRepository.ModifierStat;
import com.menudigital.domain.tenant.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for the menu-insights computation: lift/support, menu-engineering
 * classification, basket KPIs and CSV rendering. Repositories are mocked; the test lives
 * in the same package so it can wire the use case's package-private @Inject fields.
 */
class GetMenuInsightsUseCaseTest {

    private OrderAnalyticsRepository repo;
    private GetMenuInsightsUseCase useCase;

    @BeforeEach
    void setUp() {
        repo = mock(OrderAnalyticsRepository.class);
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getTenantId()).thenReturn(TenantId.generate());

        useCase = new GetMenuInsightsUseCase();
        useCase.orderAnalyticsRepository = repo;
        useCase.tenantContext = tenantContext;

        // Sensible empty defaults; individual tests override what they exercise.
        when(repo.itemSalesStats(any(), any(), any())).thenReturn(List.of());
        when(repo.frequentlyBoughtTogether(any(), any(), any(), anyInt())).thenReturn(List.of());
        when(repo.topModifiers(any(), any(), any(), anyInt())).thenReturn(List.of());
        when(repo.basketSummary(any(), any(), any())).thenReturn(new BasketSummary(0, 0, 0));
    }

    @Test
    void computesBasketKpis() {
        when(repo.basketSummary(any(), any(), any())).thenReturn(new BasketSummary(10, 25, 8));

        MenuInsightsResponse r = useCase.execute(30);

        assertEquals(10, r.ordersAnalyzed());
        assertEquals(2.5, r.avgBasketSize(), 1e-9); // 25 units / 10 orders
        assertEquals(8, r.distinctItemsSold());
    }

    @Test
    void avgBasketSizeIsZeroWithNoOrders() {
        when(repo.basketSummary(any(), any(), any())).thenReturn(new BasketSummary(0, 0, 0));

        MenuInsightsResponse r = useCase.execute(30);

        assertEquals(0.0, r.avgBasketSize(), 1e-9); // no division by zero
        assertTrue(r.frequentlyBoughtTogether().isEmpty());
        assertTrue(r.menuEngineering().items().isEmpty());
    }

    @Test
    void computesSupportAndLiftForPairs() {
        when(repo.basketSummary(any(), any(), any())).thenReturn(new BasketSummary(10, 30, 2));
        when(repo.itemSalesStats(any(), any(), any())).thenReturn(List.of(
            new ItemSalesStat("A", "Pizza", 20, new BigDecimal("200.00"), 5),
            new ItemSalesStat("B", "Gaseosa", 18, new BigDecimal("54.00"), 4)
        ));
        when(repo.frequentlyBoughtTogether(any(), any(), any(), anyInt())).thenReturn(List.of(
            new ItemPairCount("A", "B", 3)
        ));

        MenuInsightsResponse r = useCase.execute(30);
        var pair = r.frequentlyBoughtTogether().get(0);

        assertEquals("Pizza", pair.itemAName());
        assertEquals("Gaseosa", pair.itemBName());
        assertEquals(3, pair.coOccurrenceCount());
        assertEquals(0.3, pair.support(), 1e-9);          // 3 / 10 orders
        // expected co-occurrence by chance = 5*4/10 = 2.0 → lift = 3 / 2.0 = 1.5
        assertEquals(1.5, pair.lift(), 1e-9);
    }

    @Test
    void classifiesMenuEngineeringQuadrants() {
        when(repo.itemSalesStats(any(), any(), any())).thenReturn(List.of(
            new ItemSalesStat("star", "Star", 100, new BigDecimal("1000.00"), 50),
            new ItemSalesStat("plow", "Plowhorse", 100, new BigDecimal("10.00"), 50),
            new ItemSalesStat("puzzle", "Puzzle", 1, new BigDecimal("1000.00"), 1),
            new ItemSalesStat("dog", "Dog", 1, new BigDecimal("10.00"), 1)
        ));

        MenuInsightsResponse r = useCase.execute(30);
        Map<String, String> byId = r.menuEngineering().items().stream()
            .collect(Collectors.toMap(MenuItemClass::itemId, MenuItemClass::classification));

        // avgQty = 50.5, avgRevenue = 505.00
        assertEquals(50.5, r.menuEngineering().avgQuantity(), 1e-9);
        assertEquals(0, r.menuEngineering().avgRevenue().compareTo(new BigDecimal("505.00")));
        assertEquals("STAR", byId.get("star"));
        assertEquals("PLOWHORSE", byId.get("plow"));
        assertEquals("PUZZLE", byId.get("puzzle"));
        assertEquals("DOG", byId.get("dog"));
    }

    @Test
    void clampsDaysToValidRange() {
        assertEquals(90, useCase.execute(1000).days());
        assertEquals(1, useCase.execute(0).days());
        assertEquals(30, useCase.execute(30).days());
    }

    @Test
    void exportCsvContainsAllSectionsAndEscapesCommas() {
        when(repo.basketSummary(any(), any(), any())).thenReturn(new BasketSummary(10, 25, 8));
        when(repo.itemSalesStats(any(), any(), any())).thenReturn(List.of(
            new ItemSalesStat("A", "Pizza", 20, new BigDecimal("200.00"), 5)
        ));
        when(repo.topModifiers(any(), any(), any(), anyInt())).thenReturn(List.of(
            new ModifierStat("Extra queso, doble", 7, new BigDecimal("10.50"))
        ));

        String csv = useCase.exportCsv(30);

        assertTrue(csv.contains("# Se piden juntos"), "missing bought-together section");
        assertTrue(csv.contains("# Ingeniería de menú"), "missing menu-engineering section");
        assertTrue(csv.contains("# Extras más pedidos"), "missing modifiers section");
        assertTrue(csv.contains("Pizza"), "missing item name");
        // a field with a comma must be quoted (RFC 4180)
        assertTrue(csv.contains("\"Extra queso, doble\""), "comma field not quoted");
    }
}
