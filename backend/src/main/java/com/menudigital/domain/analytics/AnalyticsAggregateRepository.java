package com.menudigital.domain.analytics;

public interface AnalyticsAggregateRepository {
    void increment(InteractionEvent event);
}
