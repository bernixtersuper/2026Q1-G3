package com.menudigital.domain.analytics;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record InteractionEvent(
    String id,
    String tenantId,
    EventType eventType,
    String itemId,
    String sectionId,
    String sessionId,
    Instant timestamp,
    Map<String, String> metadata
) {
    public static InteractionEvent create(
            String tenantId,
            EventType eventType,
            String itemId,
            String sectionId,
            String sessionId,
            Map<String, String> metadata
    ) {
        return new InteractionEvent(
            UUID.randomUUID().toString(),
            tenantId,
            eventType,
            itemId,
            sectionId,
            sessionId,
            Instant.now(),
            metadata != null ? metadata : Map.of()
        );
    }
}
