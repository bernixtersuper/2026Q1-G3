package com.menudigital.application.analytics;

import com.menudigital.domain.analytics.AnalyticsRepository;
import com.menudigital.domain.analytics.EventType;
import com.menudigital.domain.analytics.InteractionEvent;
import com.menudigital.domain.order.Order;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class PublishOrderAnalyticsUseCase {

    @Inject
    AnalyticsRepository analyticsRepository;

    public void publishOrderSubmitted(Order order) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("orderId", order.getId().toString());
        metadata.put("tableId", order.getTableId().toString());
        metadata.put("itemCount", String.valueOf(order.getItems().size()));
        metadata.put("subtotal", order.getSubtotal().toPlainString());
        metadata.put("source", "TABLE_QR");

        InteractionEvent event = InteractionEvent.create(
            null,
            order.getTenantId().toString(),
            EventType.ORDER_SUBMITTED,
            null,
            null,
            order.getSessionId().toString(),
            metadata
        );

        analyticsRepository.save(event);
    }
}
