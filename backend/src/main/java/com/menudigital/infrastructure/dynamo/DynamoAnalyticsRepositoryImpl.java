package com.menudigital.infrastructure.dynamo;

import com.menudigital.domain.analytics.AnalyticsRepository;
import com.menudigital.domain.analytics.EventType;
import com.menudigital.domain.analytics.InteractionEvent;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class DynamoAnalyticsRepositoryImpl implements AnalyticsRepository {

    @Inject
    DynamoDbClient dynamoDbClient;
    
    @ConfigProperty(name = "aws.dynamodb.table", defaultValue = "menuqr-events")
    String tableName;
    
    @Override
    public void save(InteractionEvent event) {
        String pk = "TENANT#" + event.tenantId();
        String sk = "EVENT#" + event.timestamp().toString() + "#" + event.id();

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("PK", AttributeValue.builder().s(pk).build());
        item.put("SK", AttributeValue.builder().s(sk).build());
        item.put("tenantId", AttributeValue.builder().s(event.tenantId()).build());
        item.put("eventType", AttributeValue.builder().s(event.eventType().name()).build());
        item.put("sessionId", AttributeValue.builder().s(event.sessionId()).build());
        item.put("timestamp", AttributeValue.builder().s(event.timestamp().toString()).build());
        
        if (event.itemId() != null && !event.itemId().isBlank()) {
            item.put("itemId", AttributeValue.builder().s(event.itemId()).build());
        }
        if (event.sectionId() != null && !event.sectionId().isBlank()) {
            item.put("sectionId", AttributeValue.builder().s(event.sectionId()).build());
        }
        
        if (event.metadata() != null && !event.metadata().isEmpty()) {
            Map<String, AttributeValue> metadataMap = event.metadata().entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> AttributeValue.builder().s(e.getValue()).build()
                ));
            item.put("metadata", AttributeValue.builder().m(metadataMap).build());
        }
        
        PutItemRequest request = PutItemRequest.builder()
            .tableName(tableName)
            .item(item)
            .build();
        
        try {
            Log.debugf("Writing event to DynamoDB table '%s' - PK: %s, SK: %s", tableName, pk, sk);
            dynamoDbClient.putItem(request);
            Log.debugf("Event written successfully to DynamoDB");
        } catch (Exception e) {
            Log.errorf("Failed to write event to DynamoDB table '%s': %s", tableName, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public List<InteractionEvent> findByTenantAndPeriod(String tenantId, Instant from, Instant to) {
        String pk = "TENANT#" + tenantId;
        String skFrom = "EVENT#" + from.toString();
        String skTo = "EVENT#" + to.toString() + "~";
        
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":pk", AttributeValue.builder().s(pk).build());
        expressionValues.put(":skFrom", AttributeValue.builder().s(skFrom).build());
        expressionValues.put(":skTo", AttributeValue.builder().s(skTo).build());
        
        QueryRequest request = QueryRequest.builder()
            .tableName(tableName)
            .keyConditionExpression("PK = :pk AND SK BETWEEN :skFrom AND :skTo")
            .expressionAttributeValues(expressionValues)
            .build();
        
        try {
            Log.debugf("Querying DynamoDB for tenant %s from %s to %s", tenantId, from, to);
            QueryResponse response = dynamoDbClient.query(request);
            Log.debugf("Query returned %d items", response.items().size());
            return response.items().stream()
                .map(this::toInteractionEvent)
                .toList();
        } catch (Exception e) {
            Log.errorf("Failed to query events from DynamoDB: %s", e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public List<InteractionEvent> findByTenantTypeAndPeriod(String tenantId, EventType eventType, Instant from, Instant to) {
        return findByTenantAndPeriod(tenantId, from, to).stream()
            .filter(e -> e.eventType() == eventType)
            .toList();
    }
    
    private InteractionEvent toInteractionEvent(Map<String, AttributeValue> item) {
        Map<String, String> metadata = new HashMap<>();
        if (item.containsKey("metadata") && item.get("metadata").m() != null) {
            item.get("metadata").m().forEach((k, v) -> metadata.put(k, v.s()));
        }
        
        String id = item.containsKey("SK") ? 
            item.get("SK").s().replaceFirst("EVENT#.*#", "") : 
            UUID.randomUUID().toString();
        
        return new InteractionEvent(
            id,
            getStringOrNull(item, "tenantId"),
            EventType.valueOf(getStringOrNull(item, "eventType")),
            getStringOrNull(item, "itemId"),
            getStringOrNull(item, "sectionId"),
            getStringOrNull(item, "sessionId"),
            Instant.parse(getStringOrNull(item, "timestamp")),
            metadata
        );
    }
    
    private String getStringOrNull(Map<String, AttributeValue> item, String key) {
        return item.containsKey(key) ? item.get(key).s() : null;
    }
}
