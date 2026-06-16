package com.menudigital.domain.analytics;

import com.menudigital.domain.tenant.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OrderAnalyticsRepository {

    long countOrders(TenantId tenantId, Instant from, Instant to);

    BigDecimal sumRevenue(TenantId tenantId, Instant from, Instant to);

    BigDecimal avgTicket(TenantId tenantId, Instant from, Instant to);

    long countDistinctSessions(TenantId tenantId, Instant from, Instant to);

    List<TopSoldItem> topSoldItems(TenantId tenantId, Instant from, Instant to, int limit);

    Map<String, Map<Integer, Long>> ordersHeatmap(TenantId tenantId, Instant from, Instant to);

    int countActiveTables(TenantId tenantId);

    Optional<Integer> peakHourToday(TenantId tenantId, Instant dayStart, Instant dayEnd);

    List<RealtimeOrderBucket> orderBuckets(TenantId tenantId, Instant from, Instant to, int bucketMinutes);

    List<DailyOrderStats> dailyStats(TenantId tenantId, LocalDate from, LocalDate to);

    record TopSoldItem(String menuItemId, String itemName, long quantitySold, BigDecimal revenue) {}

    record RealtimeOrderBucket(Instant bucketStart, long orderCount) {}

    record DailyOrderStats(LocalDate date, long orders, BigDecimal revenue, long distinctSessions) {}
}
