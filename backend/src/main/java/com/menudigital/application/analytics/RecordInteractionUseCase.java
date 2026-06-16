package com.menudigital.application.analytics;

import com.menudigital.application.analytics.dto.RecordEventCommand;
import com.menudigital.domain.analytics.AnalyticsAggregateRepository;
import com.menudigital.domain.analytics.AnalyticsRepository;
import com.menudigital.domain.analytics.EventType;
import com.menudigital.domain.analytics.InteractionEvent;
import com.menudigital.domain.tenant.RestaurantRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.logging.Log;

@ApplicationScoped
public class RecordInteractionUseCase {

    @Inject
    AnalyticsRepository analyticsRepository;

    @Inject
    AnalyticsAggregateRepository aggregateRepository;

    @Inject
    RestaurantRepository restaurantRepository;

    public void execute(String slug, RecordEventCommand command) {
        var restaurant = restaurantRepository.findBySlug(slug)
            .orElseThrow(() -> new RestaurantNotFoundException("Restaurant not found"));

        EventType eventType = command.eventType().normalized();
        validateEvent(command, eventType);

        InteractionEvent event = InteractionEvent.create(
            command.eventId(),
            restaurant.getId().toString(),
            eventType,
            command.itemId(),
            command.sectionId(),
            command.sessionId(),
            command.metadata()
        );

        Log.debugf("Recording event - tenantId: %s, eventType: %s, eventId: %s",
            restaurant.getId(), eventType, event.id());

        if (eventType.updatesAggregates()) {
            aggregateRepository.increment(event);
        }
        analyticsRepository.save(event);

        Log.debugf("Event recorded successfully - eventId: %s", event.id());
    }

    private void validateEvent(RecordEventCommand command, EventType eventType) {
        switch (eventType) {
            case ITEM_VIEW, CART_ITEM_ADDED, CART_ITEM_REMOVED -> {
                if (command.itemId() == null || command.itemId().isBlank()) {
                    throw new InvalidEventException("itemId is required for " + eventType);
                }
            }
            case SECTION_VIEW -> {
                if (command.sectionId() == null || command.sectionId().isBlank()) {
                    throw new InvalidEventException("sectionId is required for SECTION_VIEW");
                }
            }
            default -> { }
        }
    }

    public static class RestaurantNotFoundException extends RuntimeException {
        public RestaurantNotFoundException(String message) {
            super(message);
        }
    }

    public static class InvalidEventException extends RuntimeException {
        public InvalidEventException(String message) {
            super(message);
        }
    }
}
