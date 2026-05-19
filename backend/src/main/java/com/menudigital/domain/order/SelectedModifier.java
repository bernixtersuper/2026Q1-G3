package com.menudigital.domain.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class SelectedModifier {
    
    private UUID id;
    private UUID orderItemId;
    private UUID modifierId;
    private String name;
    private BigDecimal priceAdjustment;
    private String modifierType;
    private Instant createdAt;
    
    public SelectedModifier() {
    }
    
    public SelectedModifier(UUID id, UUID orderItemId, UUID modifierId, String name,
                           BigDecimal priceAdjustment, String modifierType, Instant createdAt) {
        this.id = id;
        this.orderItemId = orderItemId;
        this.modifierId = modifierId;
        this.name = name;
        this.priceAdjustment = priceAdjustment;
        this.modifierType = modifierType;
        this.createdAt = createdAt;
    }
    
    public static SelectedModifier create(UUID orderItemId, UUID modifierId, String name,
                                          BigDecimal priceAdjustment, String modifierType) {
        return new SelectedModifier(
            UUID.randomUUID(),
            orderItemId,
            modifierId,
            name,
            priceAdjustment,
            modifierType,
            Instant.now()
        );
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrderItemId() { return orderItemId; }
    public void setOrderItemId(UUID orderItemId) { this.orderItemId = orderItemId; }
    public UUID getModifierId() { return modifierId; }
    public void setModifierId(UUID modifierId) { this.modifierId = modifierId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPriceAdjustment() { return priceAdjustment; }
    public void setPriceAdjustment(BigDecimal priceAdjustment) { this.priceAdjustment = priceAdjustment; }
    public String getModifierType() { return modifierType; }
    public void setModifierType(String modifierType) { this.modifierType = modifierType; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
