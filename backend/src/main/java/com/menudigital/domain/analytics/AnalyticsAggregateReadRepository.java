package com.menudigital.domain.analytics;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AnalyticsAggregateReadRepository {

    Optional<DayAggregate> getDay(String tenantId, LocalDate date);

    List<DayAggregate> queryDays(String tenantId, LocalDate from, LocalDate to);

    List<HourAggregate> queryHours(String tenantId, Instant from, Instant to);

    List<ItemAggregate> queryItems(String tenantId);

    record DayAggregate(
        LocalDate date,
        long menuViews,
        long itemViews,
        long cartAdds,
        Long uniqueMenuSessions,
        Instant batchCompletedAt,
        List<String> topItemIds,
        Map<String, Long> filterBreakdown,
        Map<String, Long> sectionBreakdown
    ) {}

    record HourAggregate(
        Instant bucketStart,
        long menuViews,
        long itemViews,
        long cartAdds
    ) {}

    record ItemAggregate(
        String itemId,
        long views,
        Instant lastViewedAt
    ) {}
}
