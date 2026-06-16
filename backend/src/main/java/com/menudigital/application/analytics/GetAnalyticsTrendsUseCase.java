package com.menudigital.application.analytics;

import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.analytics.*;
import com.menudigital.domain.tenant.TenantId;
import com.menudigital.infrastructure.persistence.entity.MenuSectionEntity;
import com.menudigital.infrastructure.persistence.entity.MenuSectionEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@ApplicationScoped
public class GetAnalyticsTrendsUseCase {

    @Inject
    AnalyticsAggregateReadRepository aggregateReadRepository;

    @Inject
    OrderAnalyticsRepository orderAnalyticsRepository;

    @Inject
    TenantContext tenantContext;

    @Inject
    EntityManager em;

    public AnalyticsTrendsResponse execute(int days) {
        TenantId tenantId = tenantContext.getTenantId();
        ZoneId zone = ZoneId.systemDefault();
        int clampedDays = Math.min(Math.max(days, 1), 90);
        LocalDate today = LocalDate.now(zone);
        LocalDate from = today.minusDays(clampedDays - 1L);

        Map<LocalDate, AnalyticsAggregateReadRepository.DayAggregate> dayByDate = new HashMap<>();
        for (var day : aggregateReadRepository.queryDays(tenantId.toString(), from, today)) {
            dayByDate.put(day.date(), day);
        }

        List<OrderAnalyticsRepository.DailyOrderStats> orderStats =
            orderAnalyticsRepository.dailyStats(tenantId, from, today);

        List<AnalyticsTrendsResponse.DailyTrendPoint> series = new ArrayList<>();
        long totalOrders = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        Map<String, Long> filterUsage = new LinkedHashMap<>();
        Map<String, Long> sectionCounts = new LinkedHashMap<>();

        for (var stats : orderStats) {
            var dayAgg = dayByDate.get(stats.date());
            long menuViews = dayAgg != null ? dayAgg.menuViews() : 0;
            long itemViews = dayAgg != null ? dayAgg.itemViews() : 0;
            Long uniqueSessions = dayAgg != null ? dayAgg.uniqueMenuSessions() : null;

            ConversionStatus status = ConversionStatus.PRELIMINARY;
            Double conversionRate = null;
            if (dayAgg != null
                && uniqueSessions != null
                && uniqueSessions > 0
                && dayAgg.batchCompletedAt() != null) {
                status = ConversionStatus.FINAL;
                conversionRate = (double) stats.distinctSessions() / uniqueSessions;
            }

            series.add(new AnalyticsTrendsResponse.DailyTrendPoint(
                stats.date(),
                stats.orders(),
                stats.revenue(),
                menuViews,
                itemViews,
                uniqueSessions,
                conversionRate,
                status
            ));

            totalOrders += stats.orders();
            totalRevenue = totalRevenue.add(stats.revenue());

            if (dayAgg != null) {
                mergeCounts(filterUsage, dayAgg.filterBreakdown());
                mergeCounts(sectionCounts, dayAgg.sectionBreakdown());
            }
        }

        Map<UUID, String> sectionNames = loadSectionNames(tenantId);
        List<AnalyticsTrendsResponse.SectionTrendPoint> sectionEngagement = sectionCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .map(e -> {
                UUID sectionId = UUID.fromString(e.getKey());
                String name = sectionNames.getOrDefault(sectionId, e.getKey());
                return new AnalyticsTrendsResponse.SectionTrendPoint(e.getKey(), name, e.getValue());
            })
            .toList();

        return new AnalyticsTrendsResponse(
            clampedDays,
            series,
            filterUsage,
            sectionEngagement,
            totalOrders,
            totalRevenue
        );
    }

    private void mergeCounts(Map<String, Long> target, Map<String, Long> source) {
        if (source == null) return;
        source.forEach((k, v) -> target.merge(k, v, Long::sum));
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, String> loadSectionNames(TenantId tenantId) {
        List<MenuSectionEntity> sections = em.createQuery(
            "SELECT s FROM MenuSectionEntity s WHERE s.restaurantId = :rid",
            MenuSectionEntity.class)
            .setParameter("rid", tenantId.value())
            .getResultList();

        Map<UUID, String> names = new HashMap<>();
        for (MenuSectionEntity section : sections) {
            names.put(section.id, section.name);
        }
        return names;
    }
}
