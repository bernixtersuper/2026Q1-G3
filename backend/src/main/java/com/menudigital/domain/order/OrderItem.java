package com.menudigital.domain.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderItem {
    
    private UUID id;
    private UUID orderId;
    private UUID menuItemId;
    private String menuItemName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal basePrice;
    private String notes;
    private String addedBy;
    private Instant createdAt;
    private List<SelectedModifier> selectedModifiers = new ArrayList<>();
    
    public OrderItem() {
    }
    
    public OrderItem(UUID id, UUID orderId, UUID menuItemId, String menuItemName,
                     int quantity, BigDecimal unitPrice, BigDecimal basePrice, 
                     String notes, String addedBy, Instant createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.menuItemId = menuItemId;
        this.menuItemName = menuItemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.basePrice = basePrice;
        this.notes = notes;
        this.addedBy = addedBy;
        this.createdAt = createdAt;
    }
    
    public static OrderItem create(UUID orderId, UUID menuItemId, String menuItemName,
                                    BigDecimal basePrice, BigDecimal modifierTotal,
                                    int quantity, String notes, String addedBy) {
        BigDecimal unitPrice = basePrice.add(modifierTotal);
        return new OrderItem(
            UUID.randomUUID(),
            orderId,
            menuItemId,
            menuItemName,
            quantity,
            unitPrice,
            basePrice,
            notes,
            addedBy,
            Instant.now()
        );
    }
    
    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    public void incrementQuantity() {
        this.quantity++;
    }
    
    public void decrementQuantity() {
        if (this.quantity > 1) {
            this.quantity--;
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public UUID getMenuItemId() { return menuItemId; }
    public void setMenuItemId(UUID menuItemId) { this.menuItemId = menuItemId; }
    public String getMenuItemName() { return menuItemName; }
    public void setMenuItemName(String menuItemName) { this.menuItemName = menuItemName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getAddedBy() { return addedBy; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<SelectedModifier> getSelectedModifiers() { return selectedModifiers; }
    public void setSelectedModifiers(List<SelectedModifier> selectedModifiers) { 
        this.selectedModifiers = selectedModifiers != null ? selectedModifiers : new ArrayList<>(); 
    }
}
