package com.menudigital.infrastructure.persistence;

import com.menudigital.domain.analytics.OrderAnalyticsRepository;
import com.menudigital.domain.order.OrderStatus;
import com.menudigital.domain.tenant.TenantId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
        Number result = (Number) em.createQuery(
            "SELECT COALESCE(SUM(o.subtotal), 0) FROM OrderEntity o " +
            "WHERE o.restaurantId = :rid AND o.status NOT IN :excluded " +
            "AND o.submittedAt >= :from AND o.submittedAt < :to")
            .setParameter("rid", tenantId.value())
            .setParameter("excluded", EXCLUDED)
            .setParameter("from", from)
            .setParameter("to", to)
            .getSingleResult();
        return toBigDecimal(result);
    }

    @Override
    public BigDecimal avgTicket(TenantId tenantId, Instant from, Instant to) {
        Number result = (Number) em.createQuery(
            "SELECT COALESCE(AVG(o.subtotal), 0) FROM OrderEntity o " +
            "WHERE o.restaurantId = :rid AND o.status NOT IN :excluded " +
            "AND o.submittedAt >= :from AND o.submittedAt < :to")
            .setParameter("rid", tenantId.value())
            .setParameter("excluded", EXCLUDED)
            .setParameter("from", from)
            .setParameter("to", to)
            .getSingleResult();
        return toBigDecimal(result);
    }

    private static BigDecimal toBigDecimal(Number value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return BigDecimal.valueOf(value.doubleValue());
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
            SELECT CAST(EXTRACT(DOW FROM o.submitted_at AT TIME ZONE 'UTC') AS INTEGER) AS dow,
                   CAST(EXTRACT(HOUR FROM o.submitted_at AT TIME ZONE 'UTC') AS INTEGER) AS hour,
                   CAST(COUNT(*) AS BIGINT)
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
            SELECT CAST(EXTRACT(HOUR FROM o.submitted_at AT TIME ZONE 'UTC') AS INTEGER) AS hour,
                   CAST(COUNT(*) AS BIGINT) AS cnt
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

    @Override
    @SuppressWarnings("unchecked")
    public List<DailyOrderStats> dailyStats(TenantId tenantId, LocalDate from, LocalDate to) {
        ZoneId zone = ZoneId.systemDefault();
        Instant rangeStart = from.atStartOfDay(zone).toInstant();
        Instant rangeEnd = to.plusDays(1).atStartOfDay(zone).toInstant();

        List<Object[]> rows = em.createNativeQuery("""
            SELECT CAST(o.submitted_at AT TIME ZONE 'UTC' AS DATE) AS day,
                   CAST(COUNT(*) AS BIGINT),
                   COALESCE(SUM(o.subtotal), 0),
                   CAST(COUNT(DISTINCT o.session_id) AS BIGINT)
            FROM orders o
            WHERE o.restaurant_id = :rid
              AND o.status NOT IN ('DRAFT', 'CANCELLED')
              AND o.submitted_at >= :from AND o.submitted_at < :to
            GROUP BY day
            ORDER BY day
            """)
            .setParameter("rid", tenantId.value())
            .setParameter("from", rangeStart)
            .setParameter("to", rangeEnd)
            .getResultList();

        Map<LocalDate, DailyOrderStats> byDate = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            byDate.put(d, new DailyOrderStats(d, 0, BigDecimal.ZERO, 0));
        }

        for (Object[] row : rows) {
            LocalDate day;
            if (row[0] instanceof java.sql.Date sqlDate) {
                day = sqlDate.toLocalDate();
            } else {
                day = LocalDate.parse(row[0].toString());
            }
            long orders = ((Number) row[1]).longValue();
            BigDecimal revenue = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
            long sessions = ((Number) row[3]).longValue();
            byDate.put(day, new DailyOrderStats(day, orders, revenue, sessions));
        }

        return new ArrayList<>(byDate.values());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ItemPairCount> frequentlyBoughtTogether(TenantId tenantId, Instant from, Instant to, int limit) {
        // Self-join order_items on the same order; a < b yields each unordered pair once and
        // excludes pairing an item with itself. COUNT(DISTINCT order) is robust to duplicate rows.
        List<Object[]> rows = em.createNativeQuery("""
            SELECT a.menu_item_id, b.menu_item_id, COUNT(DISTINCT o.id)::bigint AS cooccurrence
            FROM order_items a
            JOIN order_items b ON a.order_id = b.order_id AND a.menu_item_id < b.menu_item_id
            JOIN orders o ON o.id = a.order_id
            WHERE o.restaurant_id = :rid
              AND o.status NOT IN ('DRAFT', 'CANCELLED')
              AND o.submitted_at >= :from AND o.submitted_at < :to
            GROUP BY a.menu_item_id, b.menu_item_id
            ORDER BY cooccurrence DESC
            LIMIT :lim
            """)
            .setParameter("rid", tenantId.value())
            .setParameter("from", from)
            .setParameter("to", to)
            .setParameter("lim", limit)
            .getResultList();

        return rows.stream()
            .map(r -> new ItemPairCount(
                r[0].toString(),
                r[1].toString(),
                ((Number) r[2]).longValue()
            ))
            .toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ItemSalesStat> itemSalesStats(TenantId tenantId, Instant from, Instant to) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT oi.menu_item_id, mi.name,
                   SUM(oi.quantity)::bigint AS qty,
                   SUM(oi.quantity * oi.unit_price) AS revenue,
                   COUNT(DISTINCT oi.order_id)::bigint AS orders_with_item
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            JOIN menu_items mi ON mi.id = oi.menu_item_id
            WHERE o.restaurant_id = :rid
              AND o.status NOT IN ('DRAFT', 'CANCELLED')
              AND o.submitted_at >= :from AND o.submitted_at < :to
            GROUP BY oi.menu_item_id, mi.name
            """)
            .setParameter("rid", tenantId.value())
            .setParameter("from", from)
            .setParameter("to", to)
            .getResultList();

        return rows.stream()
            .map(r -> new ItemSalesStat(
                r[0].toString(),
                (String) r[1],
                ((Number) r[2]).longValue(),
                r[3] != null ? new BigDecimal(r[3].toString()) : BigDecimal.ZERO,
                ((Number) r[4]).longValue()
            ))
            .toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ModifierStat> topModifiers(TenantId tenantId, Instant from, Instant to, int limit) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT oim.modifier_name,
                   SUM(oi.quantity)::bigint AS times_selected,
                   SUM(oi.quantity * oim.price_adjustment) AS revenue
            FROM order_item_modifiers oim
            JOIN order_items oi ON oi.id = oim.order_item_id
            JOIN orders o ON o.id = oi.order_id
            WHERE o.restaurant_id = :rid
              AND o.status NOT IN ('DRAFT', 'CANCELLED')
              AND o.submitted_at >= :from AND o.submitted_at < :to
            GROUP BY oim.modifier_name
            ORDER BY times_selected DESC
            LIMIT :lim
            """)
            .setParameter("rid", tenantId.value())
            .setParameter("from", from)
            .setParameter("to", to)
            .setParameter("lim", limit)
            .getResultList();

        return rows.stream()
            .map(r -> new ModifierStat(
                (String) r[0],
                ((Number) r[1]).longValue(),
                r[2] != null ? new BigDecimal(r[2].toString()) : BigDecimal.ZERO
            ))
            .toList();
    }

    @Override
    public BasketSummary basketSummary(TenantId tenantId, Instant from, Instant to) {
        Object[] row = (Object[]) em.createNativeQuery("""
            SELECT COUNT(DISTINCT o.id)::bigint AS orders,
                   COALESCE(SUM(oi.quantity), 0)::bigint AS units,
                   COUNT(DISTINCT oi.menu_item_id)::bigint AS distinct_items
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            WHERE o.restaurant_id = :rid
              AND o.status NOT IN ('DRAFT', 'CANCELLED')
              AND o.submitted_at >= :from AND o.submitted_at < :to
            """)
            .setParameter("rid", tenantId.value())
            .setParameter("from", from)
            .setParameter("to", to)
            .getSingleResult();

        return new BasketSummary(
            ((Number) row[0]).longValue(),
            ((Number) row[1]).longValue(),
            ((Number) row[2]).longValue()
        );
    }
}
