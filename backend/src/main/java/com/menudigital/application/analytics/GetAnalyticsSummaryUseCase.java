package com.menudigital.application.analytics;

import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.analytics.*;
import com.menudigital.domain.tenant.TenantId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.*;
import java.util.Optional;

@ApplicationScoped
public class GetAnalyticsSummaryUseCase {

    @Inject
    AnalyticsAggregateReadRepository aggregateReadRepository;

    @Inject
    OrderAnalyticsRepository orderAnalyticsRepository;

    @Inject
    TenantContext tenantContext;

    public AnalyticsSummaryResponse execute() {
        TenantId tenantId = tenantContext.getTenantId();
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate yesterday = today.minusDays(1);

        Instant todayStart = today.atStartOfDay(zone).toInstant();
        Instant tomorrowStart = today.plusDays(1).atStartOfDay(zone).toInstant();
        Instant yesterdayStart = yesterday.atStartOfDay(zone).toInstant();

        long ordersToday = orderAnalyticsRepository.countOrders(tenantId, todayStart, tomorrowStart);
        long ordersYesterday = orderAnalyticsRepository.countOrders(tenantId, yesterdayStart, todayStart);
        var revenueToday = orderAnalyticsRepository.sumRevenue(tenantId, todayStart, tomorrowStart);
        var avgTicket = orderAnalyticsRepository.avgTicket(tenantId, todayStart, tomorrowStart);

        long menuViewsToday = aggregateReadRepository.getDay(tenantId.toString(), today)
            .map(AnalyticsAggregateReadRepository.DayAggregate::menuViews).orElse(0L);
        long menuViewsYesterday = aggregateReadRepository.getDay(tenantId.toString(), yesterday)
            .map(AnalyticsAggregateReadRepository.DayAggregate::menuViews).orElse(0L);

        var dayAgg = aggregateReadRepository.getDay(tenantId.toString(), today);
        ConversionStatus status = ConversionStatus.PRELIMINARY;
        Double conversionRate = null;
        String conversionNote = "Conversión final disponible tras job Glue nocturno; ver pedidos y vistas por separado.";

        if (dayAgg.isPresent()
            && dayAgg.get().uniqueMenuSessions() != null
            && dayAgg.get().batchCompletedAt() != null
            && dayAgg.get().batchCompletedAt().atZone(zone).toLocalDate().equals(today)) {
            status = ConversionStatus.FINAL;
            long sessions = dayAgg.get().uniqueMenuSessions();
            long orderSessions = orderAnalyticsRepository.countDistinctSessions(tenantId, todayStart, tomorrowStart);
            conversionRate = sessions > 0 ? (double) orderSessions / sessions : 0.0;
            conversionNote = null;
        }

        int activeTables = orderAnalyticsRepository.countActiveTables(tenantId);
        Integer peakHour = orderAnalyticsRepository.peakHourToday(tenantId, todayStart, tomorrowStart).orElse(null);

        return new AnalyticsSummaryResponse(
            "today",
            ordersToday,
            ordersYesterday,
            revenueToday,
            avgTicket,
            menuViewsToday,
            menuViewsYesterday,
            conversionRate,
            status,
            conversionNote,
            activeTables,
            peakHour
        );
    }
}
