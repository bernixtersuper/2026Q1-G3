package com.menudigital.interfaces.rest.admin;

import com.menudigital.application.order.OrderEventBroadcaster;
import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.order.Order;
import com.menudigital.domain.order.OrderItem;
import com.menudigital.domain.order.OrderRepository;
import com.menudigital.domain.order.OrderStatus;
import com.menudigital.domain.order.SelectedModifier;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/api/admin/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Order Management", description = "Manage orders (waiter dashboard)")
@SecurityRequirement(name = "jwt")
public class OrderAdminResource {
    
    @Inject
    OrderRepository orderRepository;
    
    @Inject
    TenantContext tenantContext;
    
    @Inject
    OrderEventBroadcaster orderEventBroadcaster;
    
    @GET
    @Operation(summary = "List all active orders")
    public Response listActiveOrders() {
        List<OrderResponse> orders = orderRepository.findActiveByTenantId(tenantContext.getTenantId())
            .stream()
            .map(this::toResponse)
            .toList();
        return Response.ok(orders).build();
    }
    
    @GET
    @Path("/all")
    @Operation(summary = "List all orders (including completed)")
    public Response listAllOrders() {
        List<OrderResponse> orders = orderRepository.findByTenantId(tenantContext.getTenantId())
            .stream()
            .map(this::toResponse)
            .toList();
        return Response.ok(orders).build();
    }
    
    @GET
    @Path("/{id}")
    @Operation(summary = "Get order by ID")
    public Response getOrder(@PathParam("id") UUID id) {
        return orderRepository.findById(id)
            .filter(o -> o.getTenantId().equals(tenantContext.getTenantId()))
            .map(o -> Response.ok(toResponse(o)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    @POST
    @Path("/{id}/confirm")
    @Transactional
    @Operation(summary = "Confirm a submitted order")
    public Response confirmOrder(@PathParam("id") UUID id) {
        return orderRepository.findById(id)
            .filter(o -> o.getTenantId().equals(tenantContext.getTenantId()))
            .map(order -> {
                try {
                    order.confirm();
                    orderRepository.update(order);
                    broadcastOrderUpdate(order);
                    return Response.ok(toResponse(order)).build();
                } catch (IllegalStateException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("INVALID_STATE", e.getMessage()))
                        .build();
                }
            })
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    @POST
    @Path("/{id}/preparing")
    @Transactional
    @Operation(summary = "Mark order as preparing")
    public Response markPreparing(@PathParam("id") UUID id) {
        return orderRepository.findById(id)
            .filter(o -> o.getTenantId().equals(tenantContext.getTenantId()))
            .map(order -> {
                try {
                    order.startPreparing();
                    orderRepository.update(order);
                    broadcastOrderUpdate(order);
                    return Response.ok(toResponse(order)).build();
                } catch (IllegalStateException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("INVALID_STATE", e.getMessage()))
                        .build();
                }
            })
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    @POST
    @Path("/{id}/ready")
    @Transactional
    @Operation(summary = "Mark order as ready")
    public Response markReady(@PathParam("id") UUID id) {
        return orderRepository.findById(id)
            .filter(o -> o.getTenantId().equals(tenantContext.getTenantId()))
            .map(order -> {
                try {
                    order.markReady();
                    orderRepository.update(order);
                    broadcastOrderUpdate(order);
                    return Response.ok(toResponse(order)).build();
                } catch (IllegalStateException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("INVALID_STATE", e.getMessage()))
                        .build();
                }
            })
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    @POST
    @Path("/{id}/delivered")
    @Transactional
    @Operation(summary = "Mark order as delivered")
    public Response markDelivered(@PathParam("id") UUID id) {
        return orderRepository.findById(id)
            .filter(o -> o.getTenantId().equals(tenantContext.getTenantId()))
            .map(order -> {
                try {
                    order.markDelivered();
                    orderRepository.update(order);
                    broadcastOrderUpdate(order);
                    return Response.ok(toResponse(order)).build();
                } catch (IllegalStateException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("INVALID_STATE", e.getMessage()))
                        .build();
                }
            })
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    @POST
    @Path("/{id}/cancel")
    @Transactional
    @Operation(summary = "Cancel an order")
    public Response cancelOrder(@PathParam("id") UUID id) {
        return orderRepository.findById(id)
            .filter(o -> o.getTenantId().equals(tenantContext.getTenantId()))
            .map(order -> {
                try {
                    order.cancel();
                    orderRepository.update(order);
                    broadcastOrderUpdate(order);
                    return Response.ok(toResponse(order)).build();
                } catch (IllegalStateException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("INVALID_STATE", e.getMessage()))
                        .build();
                }
            })
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    private void broadcastOrderUpdate(Order order) {
        String data = "{\"id\":\"" + order.getId() + "\",\"status\":\"" + order.getStatus().name() + 
            "\",\"orderNumber\":" + order.getOrderNumber() + "}";
        orderEventBroadcaster.broadcastOrderUpdate(order.getId(), order.getStatus().name(), data);
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
        List<ModifierResponse> modifiers = item.getSelectedModifiers().stream()
            .map(m -> new ModifierResponse(
                m.getId().toString(),
                m.getName(),
                m.getPriceAdjustment().toPlainString(),
                m.getModifierType()
            ))
            .toList();
            
        return new OrderItemResponse(
            item.getId().toString(),
            item.getMenuItemName(),
            item.getQuantity(),
            item.getUnitPrice().toPlainString(),
            item.getBasePrice() != null ? item.getBasePrice().toPlainString() : item.getUnitPrice().toPlainString(),
            item.getSubtotal().toPlainString(),
            item.getNotes(),
            item.getAddedBy(),
            modifiers
        );
    }
    
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
        String basePrice,
        String subtotal,
        String notes,
        String addedBy,
        List<ModifierResponse> modifiers
    ) {}
    
    public record ModifierResponse(
        String id,
        String name,
        String priceAdjustment,
        String modifierType
    ) {}
    
    public record ErrorResponse(String code, String message) {}
}
