package com.menudigital.infrastructure.dynamodb;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DynamoDBEventRepository {

    @Inject
    DynamoDbClient dynamoDb;

    @ConfigProperty(name = "dynamodb.events.table", defaultValue = "menuqr-events")
    String eventsTable;

    public List<Map<String, AttributeValue>> getEventsForTenant(String tenantId, Instant from, Instant to) {
        String pk = "TENANT#" + tenantId;
        String skFrom = "EVENT#" + from.toString();
        String skTo = "EVENT#" + to.toString();

        Map<String, AttributeValue> attrValues = new HashMap<>();
        attrValues.put(":pk", AttributeValue.builder().s(pk).build());
        attrValues.put(":from", AttributeValue.builder().s(skFrom).build());
        attrValues.put(":to", AttributeValue.builder().s(skTo).build());

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(eventsTable)
                .keyConditionExpression("PK = :pk AND SK BETWEEN :from AND :to")
                .expressionAttributeValues(attrValues)
                .build();

        QueryResponse response = dynamoDb.query(queryRequest);
        return response.hasItems() ? response.items() : new ArrayList<>();
    }
}
