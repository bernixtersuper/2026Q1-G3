package com.menudigital.application.analytics;

import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.analytics.AnalyticsAggregateReadRepository;
import com.menudigital.domain.analytics.OrderAnalyticsRepository;
import com.menudigital.domain.analytics.RealtimeAnalyticsResponse;
import com.menudigital.domain.analytics.RealtimeAnalyticsResponse.BucketCount;
import com.menudigital.domain.tenant.TenantId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class GetRealtimeAnalyticsUseCase {

    @Inject
    AnalyticsAggregateReadRepository aggregateReadRepository;

    @Inject
    OrderAnalyticsRepository orderAnalyticsRepository;

    @Inject
    TenantContext tenantContext;

    public RealtimeAnalyticsResponse execute() {
        TenantId tenantId = tenantContext.getTenantId();
        String tenantStr = tenantId.toString();
        Instant now = Instant.now();
        Instant sixtyMinutesAgo = now.minus(60, ChronoUnit.MINUTES);
        Instant fiveMinutesAgo = now.minus(5, ChronoUnit.MINUTES);

        var hours = aggregateReadRepository.queryHours(tenantStr, sixtyMinutesAgo, now);
        var orderBuckets = orderAnalyticsRepository.orderBuckets(tenantId, sixtyMinutesAgo, now, 5);
        Map<Long, Long> ordersByStart = orderBuckets.stream()
            .collect(Collectors.toMap(b -> b.bucketStart().toEpochMilli(), OrderAnalyticsRepository.RealtimeOrderBucket::orderCount));

        List<BucketCount> buckets = new ArrayList<>();
        long totalEvents5 = 0;
        long totalEvents60 = 0;
        long totalOrders5 = 0;
        long totalOrders60 = 0;

        for (int i = 11; i >= 0; i--) {
            Instant bucketStart = now.minus((i + 1) * 5L, ChronoUnit.MINUTES);
            Instant bucketEnd = now.minus(i * 5L, ChronoUnit.MINUTES);

            long eventCount = hours.stream()
                .filter(h -> !h.bucketStart().isBefore(bucketStart) && h.bucketStart().isBefore(bucketEnd))
                .mapToLong(h -> h.menuViews() + h.itemViews())
                .sum();

            long orderCount = ordersByStart.getOrDefault(bucketStart.toEpochMilli(), 0L);

            buckets.add(new BucketCount(bucketStart, eventCount, orderCount));

            if (!bucketStart.isBefore(fiveMinutesAgo)) {
                totalEvents5 += eventCount;
                totalOrders5 += orderCount;
            }
            totalEvents60 += eventCount;
            totalOrders60 += orderCount;
        }

        return new RealtimeAnalyticsResponse(
            buckets, totalEvents5, totalEvents60, totalOrders5, totalOrders60
        );
    }
}
