package com.menudigital.domain.analytics;

import java.time.Instant;
import java.util.List;

public record RealtimeAnalyticsResponse(
    List<BucketCount> buckets,
    long totalEventsLast5Min,
    long totalEventsLast60Min,
    long totalOrdersLast5Min,
    long totalOrdersLast60Min
) {
    public record BucketCount(
        Instant bucketStart,
        long eventCount,
        long orderCount
    ) {}
}
