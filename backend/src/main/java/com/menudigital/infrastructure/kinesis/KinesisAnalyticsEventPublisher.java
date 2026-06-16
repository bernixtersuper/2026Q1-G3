package com.menudigital.infrastructure.kinesis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.menudigital.domain.analytics.AnalyticsEventPublisher;
import com.menudigital.domain.analytics.InteractionEvent;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry;

import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class KinesisAnalyticsEventPublisher {

    @Inject
    KinesisClient kinesisClient;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "aws.kinesis.stream-name", defaultValue = "menuqr-events")
    String streamName;

    public void publish(InteractionEvent event) {
        String json = toJson(event);
        PutRecordsRequestEntry entry = PutRecordsRequestEntry.builder()
            .data(SdkBytes.fromString(json, StandardCharsets.UTF_8))
            .partitionKey(event.tenantId())
            .build();

        var response = kinesisClient.putRecords(PutRecordsRequest.builder()
            .streamName(streamName)
            .records(entry)
            .build());

        if (response.failedRecordCount() != null && response.failedRecordCount() > 0) {
            Log.errorf("Kinesis PutRecords failed for eventId %s: %s",
                event.id(), response.records().getFirst().errorMessage());
            throw new AnalyticsPublishException("Failed to publish event to Kinesis");
        }

        Log.debugf("Published event to Kinesis - eventId: %s, type: %s", event.id(), event.eventType());
    }

    private String toJson(InteractionEvent event) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("eventId", event.id());
            node.put("eventType", event.eventType().name());
            node.put("tenantId", event.tenantId());
            node.put("sessionId", event.sessionId());
            node.put("timestamp", event.timestamp().toString());
            if (event.itemId() != null) node.put("itemId", event.itemId());
            if (event.sectionId() != null) node.put("sectionId", event.sectionId());
            if (event.metadata() != null && !event.metadata().isEmpty()) {
                node.set("metadata", objectMapper.valueToTree(event.metadata()));
            }
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new AnalyticsPublishException("Failed to serialize analytics event", e);
        }
    }

    public static class AnalyticsPublishException extends RuntimeException {
        public AnalyticsPublishException(String message) {
            super(message);
        }

        public AnalyticsPublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
