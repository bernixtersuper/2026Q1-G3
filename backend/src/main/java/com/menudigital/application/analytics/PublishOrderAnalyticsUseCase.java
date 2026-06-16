package com.menudigital.application.analytics;

import com.menudigital.domain.analytics.AnalyticsEventPublisher;
import com.menudigital.domain.analytics.EventType;
import com.menudigital.domain.analytics.InteractionEvent;
import com.menudigital.domain.order.Order;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class PublishOrderAnalyticsUseCase {

    @Inject
    AnalyticsEventPublisher eventPublisher;

    public void publishOrderSubmitted(Order order) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("orderId", order.getId().toString());
        metadata.put("tableId", order.getTableId().toString());
        metadata.put("itemCount", String.valueOf(order.getItems().size()));
        metadata.put("subtotal", order.getSubtotal().toPlainString());
        metadata.put("source", "TABLE_QR");
        metadata.put("itemIds", order.getItems().stream()
            .map(i -> i.getMenuItemId().toString())
            .collect(Collectors.joining(",")));

        InteractionEvent event = InteractionEvent.create(
            null,
            order.getTenantId().toString(),
            EventType.ORDER_SUBMITTED,
            null,
            null,
            order.getSessionId().toString(),
            metadata
        );

        eventPublisher.publish(event);
    }

    public void publishOrderStatusChanged(Order order, String fromStatus, String toStatus) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("orderId", order.getId().toString());
        metadata.put("fromStatus", fromStatus);
        metadata.put("toStatus", toStatus);
        metadata.put("source", "TABLE_QR");

        InteractionEvent event = InteractionEvent.create(
            null,
            order.getTenantId().toString(),
            EventType.ORDER_STATUS_CHANGED,
            null,
            null,
            order.getSessionId() != null ? order.getSessionId().toString() : null,
            metadata
        );

        eventPublisher.publish(event);
    }
}
