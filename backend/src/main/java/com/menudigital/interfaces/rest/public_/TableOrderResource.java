package com.menudigital.interfaces.rest.public_;

import com.menudigital.application.order.OrderEventBroadcaster;
import com.menudigital.domain.menu.Menu;
import com.menudigital.domain.menu.MenuItem;
import com.menudigital.domain.menu.MenuItemModifier;
import com.menudigital.domain.menu.MenuRepository;
import com.menudigital.domain.menu.MenuSection;
import com.menudigital.domain.order.Order;
import com.menudigital.domain.order.OrderItem;
import com.menudigital.domain.order.OrderRepository;
import com.menudigital.domain.order.SelectedModifier;
import com.menudigital.domain.table.RestaurantTable;
import com.menudigital.domain.table.TableRepository;
import com.menudigital.domain.table.TableSession;
import com.menudigital.domain.tenant.Restaurant;
import com.menudigital.domain.tenant.RestaurantRepository;
import com.menudigital.domain.tenant.RestaurantTheme;
import com.menudigital.infrastructure.storage.MenuImageUrls;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Path("/api/table")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Table Ordering", description = "Public endpoints for table-based ordering")
public class TableOrderResource {
    
    @Inject
    TableRepository tableRepository;
    
    @Inject
    RestaurantRepository restaurantRepository;
    
    @Inject
    MenuRepository menuRepository;
    
    @Inject
    OrderRepository orderRepository;
    
    @Inject
    OrderEventBroadcaster orderEventBroadcaster;

    @Inject
    MenuImageUrls menuImageUrls;
    
    @GET
    @Path("/{qrToken}")
    @Operation(summary = "Get table info and menu by QR token")
    public Response getTableMenu(@PathParam("qrToken") String qrToken) {
        return tableRepository.findByQrToken(qrToken)
            .filter(RestaurantTable::isActive)
            .map(table -> {
                Restaurant restaurant = restaurantRepository.findById(table.getTenantId()).orElse(null);
                if (restaurant == null) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                }
                
                Menu menu = menuRepository.findByTenantId(table.getTenantId()).withOnlyAvailableItems();
                Optional<TableSession> activeSession = tableRepository.findActiveSessionByTableId(table.getId());
                
                return Response.ok(new TableMenuResponse(
                    table.getId().toString(),
                    table.getTableNumber(),
                    table.getTableName(),
                    restaurant.getName(),
                    restaurant.getSlug(),
                    toThemeResponse(restaurant.getTheme()),
                    activeSession.map(s -> s.getSessionCode()).orElse(null),
                    activeSession.map(s -> s.getId().toString()).orElse(null),
                    menu.getSortedSections().stream().map(this::toSectionResponse).toList()
                )).build();
            })
            .orElse(Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("NOT_FOUND", "Table not found or inactive"))
                .build());
    }
    
    @POST
    @Path("/{qrToken}/join")
    @Transactional
    @Operation(summary = "Join or create a table session")
    public Response joinTable(@PathParam("qrToken") String qrToken, @Valid JoinTableRequest request) {
        return tableRepository.findByQrToken(qrToken)
            .filter(RestaurantTable::isActive)
            .map(table -> {
                Optional<TableSession> existingSession = tableRepository.findActiveSessionByTableId(table.getId());
                
                if (existingSession.isPresent()) {
                    TableSession session = existingSession.get();
                    if (request.sessionCode() != null && !session.getSessionCode().equals(request.sessionCode())) {
                        return Response.status(Response.Status.FORBIDDEN)
                            .entity(new ErrorResponse("INVALID_CODE", "Invalid session code"))
                            .build();
                    }
                    
                    Optional<Order> currentOrder = orderRepository.findCurrentBySessionId(session.getId());
                    
                    return Response.ok(new JoinResponse(
                        session.getId().toString(),
                        session.getSessionCode(),
                        table.getTableNumber(),
                        currentOrder.map(o -> toOrderResponse(o)).orElse(null)
                    )).build();
                } else {
                    TableSession newSession = TableSession.create(table.getId(), table.getTenantId(), "qr_scan");
                    tableRepository.saveSession(newSession);
                    
                    return Response.ok(new JoinResponse(
                        newSession.getId().toString(),
                        newSession.getSessionCode(),
                        table.getTableNumber(),
                        null
                    )).build();
                }
            })
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    @GET
    @Path("/{qrToken}/order")
    @Operation(summary = "Get current order for session")
    public Response getOrder(
            @PathParam("qrToken") String qrToken,
            @QueryParam("sessionId") UUID sessionId) {
        
        if (sessionId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("MISSING_SESSION", "Session ID required"))
                .build();
        }
        
        return tableRepository.findByQrToken(qrToken)
            .map(table -> {
                Optional<Order> order = orderRepository.findCurrentBySessionId(sessionId);
                return order
                    .map(o -> Response.ok(toOrderResponse(o)).build())
                    .orElse(Response.ok(new OrderResponse(null, table.getTableNumber(), 0, "DRAFT", 
                        "0.00", List.of())).build());
            })
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    @GET
    @Path("/{qrToken}/order/{orderId}/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Operation(summary = "Subscribe to order status updates via SSE")
    public void subscribeToOrderUpdates(
            @PathParam("qrToken") String qrToken,
            @PathParam("orderId") UUID orderId,
            @Context SseEventSink eventSink,
            @Context Sse sse) {
        
        orderEventBroadcaster.setSse(sse);
        
        orderRepository.findById(orderId).ifPresentOrElse(
            order -> {
                var broadcaster = orderEventBroadcaster.getBroadcaster(orderId);
                broadcaster.register(eventSink);
                
                eventSink.send(sse.newEventBuilder()
                    .name("connected")
                    .data(String.class, "{\"status\":\"" + order.getStatus().name() + "\"}")
                    .build());
            },
            () -> {
                eventSink.send(sse.newEventBuilder()
                    .name("error")
                    .data(String.class, "{\"error\":\"Order not found\"}")
                    .build());
                eventSink.close();
            }
        );
    }
    
    @POST
    @Path("/{qrToken}/order/items")
    @Transactional
    @Operation(summary = "Add item to order")
    public Response addItemToOrder(
            @PathParam("qrToken") String qrToken,
            @Valid AddItemRequest request) {
        
        return tableRepository.findByQrToken(qrToken)
            .map(table -> {
                TableSession session = tableRepository.findActiveSessionByTableId(table.getId())
                    .filter(s -> s.getId().toString().equals(request.sessionId()))
                    .orElse(null);
                
                if (session == null || !session.isValid()) {
                    return Response.status(Response.Status.FORBIDDEN)
                        .entity(new ErrorResponse("INVALID_SESSION", "Invalid or expired session"))
                        .build();
                }
                
                MenuItem menuItem = menuRepository.findItemById(UUID.fromString(request.menuItemId()))
                    .filter(i -> i.isAvailable())
                    .orElse(null);
                
                if (menuItem == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("ITEM_NOT_FOUND", "Menu item not found or unavailable"))
                        .build();
                }
                
                List<MenuItemModifier> selectedModifiers = new java.util.ArrayList<>();
                BigDecimal modifierTotal = BigDecimal.ZERO;
                
                if (request.selectedModifierIds() != null && !request.selectedModifierIds().isEmpty()) {
                    List<MenuItemModifier> availableModifiers = menuRepository.findModifiersByItemId(menuItem.getId());
                    for (String modifierId : request.selectedModifierIds()) {
                        availableModifiers.stream()
                            .filter(m -> m.getId().toString().equals(modifierId) && m.isAvailable())
                            .findFirst()
                            .ifPresent(selectedModifiers::add);
                    }
                    modifierTotal = selectedModifiers.stream()
                        .map(MenuItemModifier::getPriceAdjustment)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                }
                
                Order order = orderRepository.findDraftBySessionId(session.getId())
                    .orElseGet(() -> {
                        Order newOrder = Order.create(
                            table.getTenantId(),
                            table.getId(),
                            session.getId(),
                            table.getTableNumber()
                        );
                        orderRepository.save(newOrder);
                        return newOrder;
                    });
                
                OrderItem item = OrderItem.create(
                    order.getId(),
                    menuItem.getId(),
                    menuItem.getName(),
                    menuItem.getPrice(),
                    modifierTotal,
                    request.quantity(),
                    request.notes(),
                    request.guestName()
                );
                
                orderRepository.saveItem(item);
                
                if (!selectedModifiers.isEmpty()) {
                    List<SelectedModifier> modifiersToSave = selectedModifiers.stream()
                        .map(m -> SelectedModifier.create(
                            item.getId(),
                            m.getId(),
                            m.getName(),
                            m.getPriceAdjustment(),
                            m.getModifierType().name()
                        ))
                        .toList();
                    orderRepository.saveItemModifiers(item.getId(), modifiersToSave);
                }
                
                Order updatedOrder = orderRepository.findById(order.getId()).orElse(order);
                return Response.ok(toOrderResponse(updatedOrder)).build();
            })
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    @PUT
    @Path("/{qrToken}/order/items/{itemId}")
    @Transactional
    @Operation(summary = "Update item quantity")
    public Response updateItemQuantity(
            @PathParam("qrToken") String qrToken,
            @PathParam("itemId") UUID itemId,
            @Valid UpdateItemRequest request) {
        
        return tableRepository.findByQrToken(qrToken)
            .map(table -> {
                TableSession session = tableRepository.findActiveSessionByTableId(table.getId())
                    .filter(s -> s.getId().toString().equals(request.sessionId()))
                    .orElse(null);
                
                if (session == null || !session.isValid()) {
                    return Response.status(Response.Status.FORBIDDEN)
                        .entity(new ErrorResponse("INVALID_SESSION", "Invalid or expired session"))
                        .build();
                }
                
                Order order = orderRepository.findDraftBySessionId(session.getId()).orElse(null);
                if (order == null || !order.canBeModified()) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("ORDER_NOT_MODIFIABLE", "Order cannot be modified"))
                        .build();
                }
                
                if (request.quantity() <= 0) {
                    orderRepository.deleteItem(itemId);
                } else {
                    order.getItems().stream()
                        .filter(i -> i.getId().equals(itemId))
                        .findFirst()
                        .ifPresent(item -> {
                            item.setQuantity(request.quantity());
                            orderRepository.updateItem(item);
                        });
                }
                
                Order updatedOrder = orderRepository.findById(order.getId()).orElse(order);
                return Response.ok(toOrderResponse(updatedOrder)).build();
            })
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    @DELETE
    @Path("/{qrToken}/order/items/{itemId}")
    @Transactional
    @Operation(summary = "Remove item from order")
    public Response removeItem(
            @PathParam("qrToken") String qrToken,
            @PathParam("itemId") UUID itemId,
            @QueryParam("sessionId") String sessionId) {
        
        return tableRepository.findByQrToken(qrToken)
            .map(table -> {
                TableSession session = tableRepository.findActiveSessionByTableId(table.getId())
                    .filter(s -> s.getId().toString().equals(sessionId))
                    .orElse(null);
                
                if (session == null || !session.isValid()) {
                    return Response.status(Response.Status.FORBIDDEN)
                        .entity(new ErrorResponse("INVALID_SESSION", "Invalid or expired session"))
                        .build();
                }
                
                orderRepository.deleteItem(itemId);
                
                Order order = orderRepository.findDraftBySessionId(session.getId()).orElse(null);
                return order != null 
                    ? Response.ok(toOrderResponse(order)).build()
                    : Response.noContent().build();
            })
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    @POST
    @Path("/{qrToken}/order/submit")
    @Transactional
    @Operation(summary = "Submit order for confirmation")
    public Response submitOrder(
            @PathParam("qrToken") String qrToken,
            @Valid SubmitOrderRequest request) {
        
        return tableRepository.findByQrToken(qrToken)
            .map(table -> {
                TableSession session = tableRepository.findActiveSessionByTableId(table.getId())
                    .filter(s -> s.getId().toString().equals(request.sessionId()))
                    .orElse(null);
                
                if (session == null || !session.isValid()) {
                    return Response.status(Response.Status.FORBIDDEN)
                        .entity(new ErrorResponse("INVALID_SESSION", "Invalid or expired session"))
                        .build();
                }
                
                Order order = orderRepository.findDraftBySessionId(session.getId()).orElse(null);
                if (order == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("NO_ORDER", "No order to submit"))
                        .build();
                }
                
                try {
                    int orderNumber = orderRepository.getNextOrderNumber(table.getTenantId());
                    order.setOrderNumber(orderNumber);
                    if (request.notes() != null) {
                        order.setNotes(request.notes());
                    }
                    order.submit();
                    orderRepository.update(order);
                    
                    return Response.ok(toOrderResponse(order)).build();
                } catch (IllegalStateException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("SUBMIT_FAILED", e.getMessage()))
                        .build();
                }
            })
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    @POST
    @Path("/{qrToken}/order/request-bill")
    @Transactional
    @Operation(summary = "Request bill for the order")
    public Response requestBill(
            @PathParam("qrToken") String qrToken,
            @Valid RequestBillRequest request) {
        
        return tableRepository.findByQrToken(qrToken)
            .map(table -> {
                TableSession session = tableRepository.findActiveSessionByTableId(table.getId())
                    .filter(s -> s.getId().toString().equals(request.sessionId()))
                    .orElse(null);
                
                if (session == null || !session.isValid()) {
                    return Response.status(Response.Status.FORBIDDEN)
                        .entity(new ErrorResponse("INVALID_SESSION", "Invalid or expired session"))
                        .build();
                }
                
                Order order = orderRepository.findCurrentBySessionId(session.getId()).orElse(null);
                if (order == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("NO_ORDER", "No order found"))
                        .build();
                }
                
                try {
                    order.requestBill();
                    orderRepository.update(order);
                    orderEventBroadcaster.broadcastOrderUpdate(
                        order.getId(), 
                        order.getStatus().name(), 
                        "{\"id\":\"" + order.getId() + "\",\"status\":\"BILL_REQUESTED\",\"orderNumber\":" + order.getOrderNumber() + "}"
                    );
                    
                    return Response.ok(toOrderResponse(order)).build();
                } catch (IllegalStateException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("REQUEST_FAILED", e.getMessage()))
                        .build();
                }
            })
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    private ThemeResponse toThemeResponse(RestaurantTheme theme) {
        return new ThemeResponse(
            theme.primaryColor(),
            theme.secondaryColor(),
            theme.accentColor(),
            theme.backgroundColor(),
            theme.textColor(),
            theme.cardBackground(),
            theme.gradientStart(),
            theme.gradientEnd(),
            theme.gradientDirection(),
            theme.fontFamily(),
            theme.borderRadius(),
            theme.logoUrl(),
            theme.bannerUrl(),
            theme.showGradientHeader()
        );
    }
    
    private SectionResponse toSectionResponse(MenuSection section) {
        return new SectionResponse(
            section.getId().toString(),
            section.getName(),
            section.getDisplayOrder(),
            section.getItems().stream().map(this::toItemResponse).toList()
        );
    }
    
    private ItemResponse toItemResponse(MenuItem item) {
        List<ModifierResponse> modifiers = menuRepository.findModifiersByItemId(item.getId())
            .stream()
            .filter(m -> m.isAvailable())
            .map(m -> new ModifierResponse(
                m.getId().toString(),
                m.getName(),
                m.getPriceAdjustment().toPlainString(),
                m.getModifierType().name()
            ))
            .toList();
            
        return new ItemResponse(
            item.getId().toString(),
            item.getName(),
            item.getDescription(),
            item.getPrice().toPlainString(),
            menuImageUrls.toApiPath(item.getImageUrl()),
            item.getDietaryTags().stream().map(Enum::name).toList(),
            modifiers
        );
    }
    
    private OrderResponse toOrderResponse(Order order) {
        return new OrderResponse(
            order.getId().toString(),
            order.getTableNumber(),
            order.getOrderNumber(),
            order.getStatus().name(),
            order.getSubtotal().toPlainString(),
            order.getItems().stream().map(this::toOrderItemResponse).toList()
        );
    }
    
    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        List<SelectedModifierResponse> modifiers = item.getSelectedModifiers().stream()
            .map(m -> new SelectedModifierResponse(
                m.getId().toString(),
                m.getName(),
                m.getPriceAdjustment().toPlainString(),
                m.getModifierType()
            ))
            .toList();
            
        return new OrderItemResponse(
            item.getId().toString(),
            item.getMenuItemId().toString(),
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
    
    public record JoinTableRequest(String sessionCode) {}
    
    public record AddItemRequest(
        @NotBlank String sessionId,
        @NotBlank String menuItemId,
        @Positive int quantity,
        String notes,
        String guestName,
        List<String> selectedModifierIds
    ) {}
    
    public record UpdateItemRequest(
        @NotBlank String sessionId,
        int quantity
    ) {}
    
    public record SubmitOrderRequest(
        @NotBlank String sessionId,
        String notes
    ) {}
    
    public record RequestBillRequest(
        @NotBlank String sessionId
    ) {}
    
    public record JoinResponse(
        String sessionId,
        String sessionCode,
        String tableNumber,
        OrderResponse currentOrder
    ) {}
    
    public record TableMenuResponse(
        String tableId,
        String tableNumber,
        String tableName,
        String restaurantName,
        String slug,
        ThemeResponse theme,
        String sessionCode,
        String sessionId,
        List<SectionResponse> sections
    ) {}
    
    public record ThemeResponse(
        String primaryColor,
        String secondaryColor,
        String accentColor,
        String backgroundColor,
        String textColor,
        String cardBackground,
        String gradientStart,
        String gradientEnd,
        String gradientDirection,
        String fontFamily,
        String borderRadius,
        String logoUrl,
        String bannerUrl,
        boolean showGradientHeader
    ) {}
    
    public record SectionResponse(
        String id,
        String name,
        int displayOrder,
        List<ItemResponse> items
    ) {}
    
    public record ItemResponse(
        String id,
        String name,
        String description,
        String price,
        String imageUrl,
        List<String> dietaryTags,
        List<ModifierResponse> modifiers
    ) {}
    
    public record ModifierResponse(
        String id,
        String name,
        String priceAdjustment,
        String modifierType
    ) {}
    
    public record OrderResponse(
        String id,
        String tableNumber,
        int orderNumber,
        String status,
        String subtotal,
        List<OrderItemResponse> items
    ) {}
    
    public record OrderItemResponse(
        String id,
        String menuItemId,
        String name,
        int quantity,
        String unitPrice,
        String basePrice,
        String subtotal,
        String notes,
        String addedBy,
        List<SelectedModifierResponse> modifiers
    ) {}
    
    public record SelectedModifierResponse(
        String id,
        String name,
        String priceAdjustment,
        String modifierType
    ) {}
    
    public record ErrorResponse(String code, String message) {}
}
