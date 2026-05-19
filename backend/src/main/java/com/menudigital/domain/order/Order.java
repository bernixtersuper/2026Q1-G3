package com.menudigital.domain.order;

import com.menudigital.domain.tenant.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Order {
    
    private UUID id;
    private TenantId tenantId;
    private UUID tableId;
    private UUID sessionId;
    private String tableNumber;
    private int orderNumber;
    private OrderStatus status;
    private String notes;
    private BigDecimal subtotal;
    private List<OrderItem> items;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant submittedAt;
    private Instant confirmedAt;
    
    public Order() {
        this.items = new ArrayList<>();
        this.subtotal = BigDecimal.ZERO;
    }
    
    public Order(UUID id, TenantId tenantId, UUID tableId, UUID sessionId, String tableNumber,
                 int orderNumber, OrderStatus status, String notes, BigDecimal subtotal,
                 Instant createdAt, Instant updatedAt, Instant submittedAt, Instant confirmedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.tableId = tableId;
        this.sessionId = sessionId;
        this.tableNumber = tableNumber;
        this.orderNumber = orderNumber;
        this.status = status;
        this.notes = notes;
        this.subtotal = subtotal;
        this.items = new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.submittedAt = submittedAt;
        this.confirmedAt = confirmedAt;
    }
    
    public static Order create(TenantId tenantId, UUID tableId, UUID sessionId, String tableNumber) {
        Instant now = Instant.now();
        return new Order(
            UUID.randomUUID(),
            tenantId,
            tableId,
            sessionId,
            tableNumber,
            0,
            OrderStatus.DRAFT,
            null,
            BigDecimal.ZERO,
            now,
            now,
            null,
            null
        );
    }
    
    public void addItem(OrderItem item) {
        Optional<OrderItem> existing = items.stream()
            .filter(i -> i.getMenuItemId().equals(item.getMenuItemId()) && 
                        (i.getNotes() == null ? item.getNotes() == null : i.getNotes().equals(item.getNotes())))
            .findFirst();
        
        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + item.getQuantity());
        } else {
            item.setOrderId(this.id);
            items.add(item);
        }
        recalculateSubtotal();
    }
    
    public void removeItem(UUID itemId) {
        items.removeIf(i -> i.getId().equals(itemId));
        recalculateSubtotal();
    }
    
    public void updateItemQuantity(UUID itemId, int quantity) {
        items.stream()
            .filter(i -> i.getId().equals(itemId))
            .findFirst()
            .ifPresent(item -> {
                if (quantity <= 0) {
                    items.remove(item);
                } else {
                    item.setQuantity(quantity);
                }
            });
        recalculateSubtotal();
    }
    
    private void recalculateSubtotal() {
        this.subtotal = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.updatedAt = Instant.now();
    }
    
    public void submit() {
        if (this.status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Only draft orders can be submitted");
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot submit an empty order");
        }
        this.status = OrderStatus.SUBMITTED;
        this.submittedAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    public void confirm() {
        if (this.status != OrderStatus.SUBMITTED) {
            throw new IllegalStateException("Only submitted orders can be confirmed");
        }
        this.status = OrderStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    public void startPreparing() {
        if (this.status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed orders can start preparing");
        }
        this.status = OrderStatus.PREPARING;
        this.updatedAt = Instant.now();
    }
    
    public void markReady() {
        if (this.status != OrderStatus.PREPARING) {
            throw new IllegalStateException("Only preparing orders can be marked ready");
        }
        this.status = OrderStatus.READY;
        this.updatedAt = Instant.now();
    }
    
    public void markDelivered() {
        if (this.status != OrderStatus.READY) {
            throw new IllegalStateException("Only ready orders can be delivered");
        }
        this.status = OrderStatus.DELIVERED;
        this.updatedAt = Instant.now();
    }
    
    public void requestBill() {
        if (this.status != OrderStatus.DELIVERED && this.status != OrderStatus.READY 
            && this.status != OrderStatus.CONFIRMED && this.status != OrderStatus.PREPARING) {
            throw new IllegalStateException("Cannot request bill for orders that haven't been confirmed");
        }
        this.status = OrderStatus.BILL_REQUESTED;
        this.updatedAt = Instant.now();
    }
    
    public void cancel() {
        if (this.status == OrderStatus.DELIVERED || this.status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel delivered or already cancelled orders");
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }
    
    public boolean canBeModified() {
        return this.status == OrderStatus.DRAFT;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public TenantId getTenantId() { return tenantId; }
    public void setTenantId(TenantId tenantId) { this.tenantId = tenantId; }
    public UUID getTableId() { return tableId; }
    public void setTableId(UUID tableId) { this.tableId = tableId; }
    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String tableNumber) { this.tableNumber = tableNumber; }
    public int getOrderNumber() { return orderNumber; }
    public void setOrderNumber(int orderNumber) { this.orderNumber = orderNumber; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; recalculateSubtotal(); }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
}
