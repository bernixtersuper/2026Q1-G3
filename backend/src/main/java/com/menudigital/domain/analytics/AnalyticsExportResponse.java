package com.menudigital.domain.analytics;

import java.time.Instant;

public record AnalyticsExportResponse(
    String jobId,
    String status,
    String downloadUrl,
    Instant createdAt,
    Instant completedAt,
    String errorMessage
) {}
