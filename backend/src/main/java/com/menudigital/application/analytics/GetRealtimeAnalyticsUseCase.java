package com.menudigital.application.analytics;

import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.analytics.AnalyticsRepository;
import com.menudigital.domain.analytics.InteractionEvent;
import com.menudigital.domain.analytics.RealtimeAnalyticsResponse;
import com.menudigital.domain.analytics.RealtimeAnalyticsResponse.BucketCount;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class GetRealtimeAnalyticsUseCase {
    
    @Inject
    AnalyticsRepository analyticsRepository;
    
    @Inject
    TenantContext tenantContext;
    
    public RealtimeAnalyticsResponse execute() {
        String tenantId = tenantContext.getTenantId().toString();
        Instant now = Instant.now();
        Instant sixtyMinutesAgo = now.minus(60, ChronoUnit.MINUTES);
        Instant fiveMinutesAgo = now.minus(5, ChronoUnit.MINUTES);
        
        List<InteractionEvent> events = analyticsRepository.findByTenantAndPeriod(
            tenantId, sixtyMinutesAgo, now
        );
        
        List<BucketCount> buckets = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            Instant bucketStart = now.minus((i + 1) * 5L, ChronoUnit.MINUTES);
            Instant bucketEnd = now.minus(i * 5L, ChronoUnit.MINUTES);
            
            long count = events.stream()
                .filter(e -> !e.timestamp().isBefore(bucketStart) && e.timestamp().isBefore(bucketEnd))
                .count();
            
            buckets.add(new BucketCount(bucketStart, count));
        }
        
        long totalLast5Min = events.stream()
            .filter(e -> e.timestamp().isAfter(fiveMinutesAgo))
            .count();
        
        long totalLast60Min = events.size();
        
        return new RealtimeAnalyticsResponse(buckets, totalLast5Min, totalLast60Min);
    }
}
