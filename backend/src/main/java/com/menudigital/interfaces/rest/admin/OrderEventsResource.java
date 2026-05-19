package com.menudigital.interfaces.rest.admin;

import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.order.Order;
import com.menudigital.domain.order.OrderItem;
import com.menudigital.domain.order.OrderRepository;
import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestStreamElementType;

import java.time.Duration;
import java.util.List;

@Path("/api/admin/orders/stream")
@Authenticated
@Tag(name = "Order Events", description = "Real-time order updates via SSE")
@SecurityRequirement(name = "jwt")
public class OrderEventsResource {
    
    @Inject
    OrderRepository orderRepository;
    
    @Inject
    TenantContext tenantContext;
    
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @Operation(summary = "Stream active orders (SSE)")
    public Multi<OrdersSnapshot> streamOrders() {
        return Multi.createFrom().ticks().every(Duration.ofSeconds(3))
            .map(tick -> {
                List<OrderResponse> orders = orderRepository.findActiveByTenantId(tenantContext.getTenantId())
                    .stream()
                    .map(this::toResponse)
                    .toList();
                return new OrdersSnapshot(orders, System.currentTimeMillis());
            });
    }
    
    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
            order.getId().toString(),
            order.getTableNumber(),
            order.getOrderNumber(),
            order.getStatus().name(),
            order.getNotes(),
            order.getSubtotal().toPlainString(),
            order.getItems().stream().map(this::toItemResponse).toList(),
            order.getCreatedAt().toString(),
            order.getSubmittedAt() != null ? order.getSubmittedAt().toString() : null
        );
    }
    
    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
            item.getId().toString(),
            item.getMenuItemName(),
            item.getQuantity(),
            item.getUnitPrice().toPlainString(),
            item.getSubtotal().toPlainString(),
            item.getNotes(),
            item.getAddedBy()
        );
    }
    
    public record OrdersSnapshot(List<OrderResponse> orders, long timestamp) {}
    
    public record OrderResponse(
        String id,
        String tableNumber,
        int orderNumber,
        String status,
        String notes,
        String subtotal,
        List<OrderItemResponse> items,
        String createdAt,
        String submittedAt
    ) {}
    
    public record OrderItemResponse(
        String id,
        String name,
        int quantity,
        String unitPrice,
        String subtotal,
        String notes,
        String addedBy
    ) {}
}
