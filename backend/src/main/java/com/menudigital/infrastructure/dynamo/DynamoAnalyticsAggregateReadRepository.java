package com.menudigital.infrastructure.dynamo;

import com.menudigital.domain.analytics.AnalyticsAggregateReadRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@ApplicationScoped
public class DynamoAnalyticsAggregateReadRepository implements AnalyticsAggregateReadRepository {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH");

    @Inject
    DynamoDbClient dynamoDbClient;

    @ConfigProperty(name = "aws.dynamodb.analytics-table", defaultValue = "menuqr-analytics")
    String tableName;

    @Override
    public Optional<DayAggregate> getDay(String tenantId, LocalDate date) {
        return queryDays(tenantId, date, date).stream().findFirst();
    }

    @Override
    public List<DayAggregate> queryDays(String tenantId, LocalDate from, LocalDate to) {
        String pk = "TENANT#" + tenantId;
        String skFrom = "DAY#" + from.format(DAY_FMT);
        String skTo = "DAY#" + to.format(DAY_FMT) + "~";

        QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
            .tableName(tableName)
            .keyConditionExpression("PK = :pk AND SK BETWEEN :from AND :to")
            .expressionAttributeValues(Map.of(
                ":pk", AttributeValue.builder().s(pk).build(),
                ":from", AttributeValue.builder().s(skFrom).build(),
                ":to", AttributeValue.builder().s(skTo).build()
            ))
            .build());

        return response.items().stream().map(this::toDayAggregate).toList();
    }

    @Override
    public List<HourAggregate> queryHours(String tenantId, Instant from, Instant to) {
        String pk = "TENANT#" + tenantId;
        String skFrom = "HOUR#" + from.atZone(ZONE).format(HOUR_FMT);
        String skTo = "HOUR#" + to.atZone(ZONE).format(HOUR_FMT) + "~";

        QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
            .tableName(tableName)
            .keyConditionExpression("PK = :pk AND SK BETWEEN :from AND :to")
            .expressionAttributeValues(Map.of(
                ":pk", AttributeValue.builder().s(pk).build(),
                ":from", AttributeValue.builder().s(skFrom).build(),
                ":to", AttributeValue.builder().s(skTo).build()
            ))
            .build());

        return response.items().stream().map(this::toHourAggregate).toList();
    }

    @Override
    public List<ItemAggregate> queryItems(String tenantId) {
        String pk = "TENANT#" + tenantId;

        QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
            .tableName(tableName)
            .keyConditionExpression("PK = :pk AND begins_with(SK, :prefix)")
            .expressionAttributeValues(Map.of(
                ":pk", AttributeValue.builder().s(pk).build(),
                ":prefix", AttributeValue.builder().s("ITEM#").build()
            ))
            .build());

        return response.items().stream().map(this::toItemAggregate).toList();
    }

    private DayAggregate toDayAggregate(Map<String, AttributeValue> item) {
        String sk = item.get("SK").s();
        LocalDate date = LocalDate.parse(sk.substring("DAY#".length()), DAY_FMT);
        Long uniqueSessions = item.containsKey("uniqueMenuSessions")
            ? Long.parseLong(item.get("uniqueMenuSessions").n()) : null;
        Instant batchCompletedAt = item.containsKey("batchCompletedAt")
            ? Instant.parse(item.get("batchCompletedAt").s()) : null;

        return new DayAggregate(
            date,
            getLong(item, "menuViews"),
            getLong(item, "itemViews"),
            getLong(item, "cartAdds"),
            uniqueSessions,
            batchCompletedAt,
            getStringList(item, "topItemIds"),
            getLongMap(item, "filterBreakdown"),
            getLongMap(item, "sectionBreakdown")
        );
    }

    private List<String> getStringList(Map<String, AttributeValue> item, String key) {
        if (!item.containsKey(key) || item.get(key).l() == null) {
            return List.of();
        }
        return item.get(key).l().stream()
            .map(AttributeValue::s)
            .filter(Objects::nonNull)
            .toList();
    }

    private Map<String, Long> getLongMap(Map<String, AttributeValue> item, String key) {
        if (!item.containsKey(key) || item.get(key).m() == null) {
            return Map.of();
        }
        Map<String, Long> result = new LinkedHashMap<>();
        item.get(key).m().forEach((k, v) -> {
            if (v.n() != null) {
                result.put(k, Long.parseLong(v.n()));
            }
        });
        return result;
    }

    private HourAggregate toHourAggregate(Map<String, AttributeValue> item) {
        String sk = item.get("SK").s();
        String hourPart = sk.substring("HOUR#".length());
        Instant bucketStart = LocalDate.parse(hourPart.substring(0, 10), DAY_FMT)
            .atTime(Integer.parseInt(hourPart.substring(11)), 0)
            .atZone(ZONE)
            .toInstant();

        return new HourAggregate(
            bucketStart,
            getLong(item, "menuViews"),
            getLong(item, "itemViews"),
            getLong(item, "cartAdds")
        );
    }

    private ItemAggregate toItemAggregate(Map<String, AttributeValue> item) {
        String itemId = item.get("SK").s().substring("ITEM#".length());
        Instant lastViewed = item.containsKey("lastViewedAt")
            ? Instant.parse(item.get("lastViewedAt").s()) : null;
        return new ItemAggregate(itemId, getLong(item, "views"), lastViewed);
    }

    private long getLong(Map<String, AttributeValue> item, String key) {
        return item.containsKey(key) ? Long.parseLong(item.get(key).n()) : 0L;
    }
}
