package com.menudigital.domain.analytics;

public interface AnalyticsEventPublisher {
    void publish(InteractionEvent event);
}
