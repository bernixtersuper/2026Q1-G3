package com.menudigital.domain.analytics;

import java.time.Instant;
import java.util.List;

public interface AnalyticsRepository {
    
    void save(InteractionEvent event);
    
    List<InteractionEvent> findByTenantAndPeriod(String tenantId, Instant from, Instant to);
    
    List<InteractionEvent> findByTenantTypeAndPeriod(String tenantId, EventType eventType, Instant from, Instant to);
}
