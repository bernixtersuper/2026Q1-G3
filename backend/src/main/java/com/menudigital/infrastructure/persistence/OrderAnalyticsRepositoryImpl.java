package com.menudigital.infrastructure.persistence;

import com.menudigital.domain.analytics.OrderAnalyticsRepository;
import com.menudigital.domain.order.OrderStatus;
import com.menudigital.domain.tenant.TenantId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class OrderAnalyticsRepositoryImpl implements OrderAnalyticsRepository {

    private static final List<OrderStatus> EXCLUDED = List.of(OrderStatus.DRAFT, OrderStatus.CANCELLED);

    @Inject
    EntityManager em;

    @Override
    public long countOrders(TenantId tenantId, Instant from, Instant to) {
        return ((Number) em.createQuery(
            "SELECT COUNT(o) FROM OrderEntity o " +
            "WHERE o.restaurantId = :rid AND o.status NOT IN :excluded " +
            "AND o.submittedAt >= :from AND o.submittedAt < :to")
            .setParameter("rid", tenantId.value())
            .setParameter("excluded", EXCLUDED)
            .setParameter("from", from)
            .setParameter("to", to)
            .getSingleResult()).longValue();
    }

    @Override
    public BigDecimal sumRevenue(TenantId tenantId, Instant from, Instant to) {
        BigDecimal result = em.createQuery(
            "SELECT COALESCE(SUM(o.subtotal), 0) FROM OrderEntity o " +
            "WHERE o.restaurantId = :rid AND o.status NOT IN :excluded " +
            "AND o.submittedAt >= :from AND o.submittedAt < :to", BigDecimal.class)
            .setParameter("rid", tenantId.value())
            .setParameter("excluded", EXCLUDED)
            .setParameter("from", from)
            .setParameter("to", to)
            .getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal avgTicket(TenantId tenantId, Instant from, Instant to) {
        BigDecimal result = em.createQuery(
            "SELECT COALESCE(AVG(o.subtotal), 0) FROM OrderEntity o " +
            "WHERE o.restaurantId = :rid AND o.status NOT IN :excluded " +
            "AND o.submittedAt >= :from AND o.submittedAt < :to", BigDecimal.class)
            .setParameter("rid", tenantId.value())
            .setParameter("excluded", EXCLUDED)
            .setParameter("from", from)
            .setParameter("to", to)
            .getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    @Override
    public long countDistinctSessions(TenantId tenantId, Instant from, Instant to) {
        return ((Number) em.createQuery(
            "SELECT COUNT(DISTINCT o.sessionId) FROM OrderEntity o " +
            "WHERE o.restaurantId = :rid AND o.status NOT IN :excluded " +
            "AND o.sessionId IS NOT NULL AND o.submittedAt >= :from AND o.submittedAt < :to")
            .setParameter("rid", tenantId.value())
            .setParameter("excluded", EXCLUDED)
            .setParameter("from", from)
            .setParameter("to", to)
            .getSingleResult()).longValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TopSoldItem> topSoldItems(TenantId tenantId, Instant from, Instant to, int limit) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT oi.menu_item_id, mi.name, SUM(oi.quantity), SUM(oi.quantity * oi.unit_price)
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            JOIN menu_items mi ON mi.id = oi.menu_item_id
            WHERE o.restaurant_id = :rid
              AND o.status NOT IN ('DRAFT', 'CANCELLED')
              AND o.submitted_at >= :from AND o.submitted_at < :to
            GROUP BY oi.menu_item_id, mi.name
            ORDER BY SUM(oi.quantity) DESC
            LIMIT :lim
            """)
            .setParameter("rid", tenantId.value())
            .setParameter("from", from)
            .setParameter("to", to)
            .setParameter("lim", limit)
            .getResultList();

        return rows.stream()
            .map(r -> new TopSoldItem(
                r[0].toString(),
                (String) r[1],
                ((Number) r[2]).longValue(),
                r[3] != null ? new BigDecimal(r[3].toString()) : BigDecimal.ZERO
            ))
            .toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Map<Integer, Long>> ordersHeatmap(TenantId tenantId, Instant from, Instant to) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT EXTRACT(DOW FROM o.submitted_at AT TIME ZONE 'UTC')::int AS dow,
                   EXTRACT(HOUR FROM o.submitted_at AT TIME ZONE 'UTC')::int AS hour,
                   COUNT(*)::bigint
            FROM orders o
            WHERE o.restaurant_id = :rid
              AND o.status NOT IN ('DRAFT', 'CANCELLED')
              AND o.submitted_at >= :from AND o.submitted_at < :to
            GROUP BY dow, hour
            """)
            .setParameter("rid", tenantId.value())
            .setParameter("from", from)
            .setParameter("to", to)
            .getResultList();

        String[] dayNames = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        Map<String, Map<Integer, Long>> heatmap = new LinkedHashMap<>();
        for (String day : dayNames) {
            heatmap.put(day, new HashMap<>());
        }

        for (Object[] row : rows) {
            int dow = ((Number) row[0]).intValue();
            int hour = ((Number) row[1]).intValue();
            long count = ((Number) row[2]).longValue();
            heatmap.get(dayNames[dow]).put(hour, count);
        }
        return heatmap;
    }

    @Override
    public int countActiveTables(TenantId tenantId) {
        return ((Number) em.createQuery(
            "SELECT COUNT(DISTINCT ts.tableId) FROM TableSessionEntity ts " +
            "WHERE ts.restaurantId = :rid AND ts.isActive = true AND ts.expiresAt > :now")
            .setParameter("rid", tenantId.value())
            .setParameter("now", Instant.now())
            .getSingleResult()).intValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Integer> peakHourToday(TenantId tenantId, Instant dayStart, Instant dayEnd) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT EXTRACT(HOUR FROM o.submitted_at AT TIME ZONE 'UTC')::int AS hour,
                   COUNT(*)::bigint AS cnt
            FROM orders o
            WHERE o.restaurant_id = :rid
              AND o.status NOT IN ('DRAFT', 'CANCELLED')
              AND o.submitted_at >= :from AND o.submitted_at < :to
            GROUP BY hour
            ORDER BY cnt DESC
            LIMIT 1
            """)
            .setParameter("rid", tenantId.value())
            .setParameter("from", dayStart)
            .setParameter("to", dayEnd)
            .getResultList();

        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(((Number) rows.get(0)[0]).intValue());
    }

    @Override
    public List<RealtimeOrderBucket> orderBuckets(TenantId tenantId, Instant from, Instant to, int bucketMinutes) {
        long bucketMs = bucketMinutes * 60_000L;
        List<RealtimeOrderBucket> buckets = new ArrayList<>();

        for (long start = from.toEpochMilli(); start < to.toEpochMilli(); start += bucketMs) {
            Instant bucketStart = Instant.ofEpochMilli(start);
            Instant bucketEnd = Instant.ofEpochMilli(Math.min(start + bucketMs, to.toEpochMilli()));
            long count = countOrders(tenantId, bucketStart, bucketEnd);
            buckets.add(new RealtimeOrderBucket(bucketStart, count));
        }
        return buckets;
    }
}
