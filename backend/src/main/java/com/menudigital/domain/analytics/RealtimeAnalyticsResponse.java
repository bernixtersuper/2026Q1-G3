package com.menudigital.domain.analytics;

import java.time.Instant;
import java.util.List;

public record RealtimeAnalyticsResponse(
    List<BucketCount> buckets,
    long totalLast5Min,
    long totalLast60Min
) {
    public record BucketCount(Instant bucketStart, long count) {}
}
