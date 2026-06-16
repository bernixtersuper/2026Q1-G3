package com.menudigital.infrastructure.dynamo;

import com.menudigital.domain.analytics.AnalyticsAggregateRepository;
import com.menudigital.domain.analytics.EventType;
import com.menudigital.domain.analytics.InteractionEvent;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DynamoAnalyticsAggregateRepository implements AnalyticsAggregateRepository {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH");

    @Inject
    DynamoDbClient dynamoDbClient;

    @ConfigProperty(name = "aws.dynamodb.analytics-table", defaultValue = "menuqr-analytics")
    String tableName;

    @Override
    public void increment(InteractionEvent event) {
        if (!event.eventType().updatesAggregates()) {
            return;
        }

        String pk = "TENANT#" + event.tenantId();
        var zoned = event.timestamp().atZone(ZONE);
        String daySk = "DAY#" + zoned.format(DAY_FMT);
        String hourSk = "HOUR#" + zoned.format(HOUR_FMT);
        long ttl = Instant.now().plusSeconds(7L * 24 * 3600).getEpochSecond();

        List<TransactWriteItem> items = new ArrayList<>();

        items.add(TransactWriteItem.builder()
            .put(Put.builder()
                .tableName(tableName)
                .item(Map.of(
                    "PK", AttributeValue.builder().s(pk).build(),
                    "SK", AttributeValue.builder().s("PROC#" + event.id()).build(),
                    "processedAt", AttributeValue.builder().s(event.timestamp().toString()).build(),
                    "ttl", AttributeValue.builder().n(String.valueOf(ttl)).build()
                ))
                .conditionExpression("attribute_not_exists(SK)")
                .build())
            .build());

        Map<String, String> dayAdds = counterFields(event.eventType());
        if (!dayAdds.isEmpty()) {
            items.add(counterUpdate(pk, daySk, dayAdds));
        }

        Map<String, String> hourAdds = counterFields(event.eventType());
        if (!hourAdds.isEmpty()) {
            items.add(counterUpdate(pk, hourSk, hourAdds));
        }

        if (event.eventType() == EventType.ITEM_VIEW && event.itemId() != null) {
            items.add(TransactWriteItem.builder()
                .update(Update.builder()
                    .tableName(tableName)
                    .key(Map.of(
                        "PK", AttributeValue.builder().s(pk).build(),
                        "SK", AttributeValue.builder().s("ITEM#" + event.itemId()).build()
                    ))
                    .updateExpression("ADD views :one SET lastViewedAt = :ts")
                    .expressionAttributeValues(Map.of(
                        ":one", AttributeValue.builder().n("1").build(),
                        ":ts", AttributeValue.builder().s(event.timestamp().toString()).build()
                    ))
                    .build())
                .build());
        }

        try {
            dynamoDbClient.transactWriteItems(TransactWriteItemsRequest.builder()
                .transactItems(items)
                .build());
        } catch (TransactionCanceledException e) {
            if (isDuplicateEvent(e)) {
                Log.debugf("Duplicate eventId %s — idempotent skip", event.id());
                return;
            }
            throw e;
        }
    }

    private TransactWriteItem counterUpdate(String pk, String sk, Map<String, String> fields) {
        StringBuilder expr = new StringBuilder("ADD ");
        Map<String, AttributeValue> values = new HashMap<>();
        int i = 0;
        for (String field : fields.keySet()) {
            if (i++ > 0) expr.append(", ");
            String placeholder = ":v" + field;
            expr.append(field).append(" ").append(placeholder);
            values.put(placeholder, AttributeValue.builder().n("1").build());
        }

        return TransactWriteItem.builder()
            .update(Update.builder()
                .tableName(tableName)
                .key(Map.of(
                    "PK", AttributeValue.builder().s(pk).build(),
                    "SK", AttributeValue.builder().s(sk).build()
                ))
                .updateExpression(expr.toString())
                .expressionAttributeValues(values)
                .build())
            .build();
    }

    private Map<String, String> counterFields(EventType type) {
        return switch (type.normalized()) {
            case MENU_VIEW -> Map.of("menuViews", "1");
            case ITEM_VIEW, SECTION_VIEW -> Map.of("itemViews", "1");
            case CART_ITEM_ADDED -> Map.of("cartAdds", "1");
            default -> Map.of();
        };
    }

    private boolean isDuplicateEvent(TransactionCanceledException e) {
        return e.cancellationReasons() != null && e.cancellationReasons().stream()
            .anyMatch(r -> "ConditionalCheckFailed".equals(r.code()));
    }
}
