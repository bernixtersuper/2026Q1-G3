package com.menudigital.infrastructure.analytics;

import com.menudigital.domain.analytics.AnalyticsAggregateRepository;
import com.menudigital.domain.analytics.AnalyticsEventPublisher;
import com.menudigital.domain.analytics.InteractionEvent;
import com.menudigital.infrastructure.kinesis.KinesisAnalyticsEventPublisher;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AnalyticsEventPublisherImpl implements AnalyticsEventPublisher {

    @Inject
    KinesisAnalyticsEventPublisher kinesisPublisher;

    @Inject
    AnalyticsAggregateRepository aggregateRepository;

    @ConfigProperty(name = "aws.analytics.kinesis.enabled", defaultValue = "false")
    boolean kinesisEnabled;

    @Override
    public void publish(InteractionEvent event) {
        if (kinesisEnabled) {
            kinesisPublisher.publish(event);
            return;
        }

        Log.debugf("Kinesis disabled — inline aggregate write for eventId %s", event.id());
        if (event.eventType().updatesAggregates()) {
            aggregateRepository.increment(event);
        }
    }
}
