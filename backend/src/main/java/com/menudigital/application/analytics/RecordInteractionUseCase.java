package com.menudigital.application.analytics;

import com.menudigital.application.analytics.dto.RecordEventCommand;
import com.menudigital.domain.analytics.AnalyticsRepository;
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
    RestaurantRepository restaurantRepository;
    
    public void execute(String slug, RecordEventCommand command) {
        var restaurant = restaurantRepository.findBySlug(slug)
            .orElseThrow(() -> new RestaurantNotFoundException("Restaurant not found"));
        
        InteractionEvent event = InteractionEvent.create(
            restaurant.getId().toString(),
            command.eventType(),
            command.itemId(),
            command.sectionId(),
            command.sessionId(),
            command.metadata()
        );
        
        Log.debugf("Saving event to DynamoDB - tenantId: %s, eventType: %s, eventId: %s", 
            restaurant.getId(), command.eventType(), event.id());
        
        analyticsRepository.save(event);
        
        Log.debugf("Event saved successfully - eventId: %s", event.id());
    }
    
    public static class RestaurantNotFoundException extends RuntimeException {
        public RestaurantNotFoundException(String message) {
            super(message);
        }
    }
}
