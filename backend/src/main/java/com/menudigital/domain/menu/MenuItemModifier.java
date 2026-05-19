package com.menudigital.domain.menu;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class MenuItemModifier {
    private UUID id;
    private UUID menuItemId;
    private UUID tenantId;
    private String name;
    private BigDecimal priceAdjustment;
    private ModifierType modifierType;
    private boolean available;
    private int displayOrder;
    private Instant createdAt;

    public MenuItemModifier() {
    }

    public static MenuItemModifier create(
            UUID menuItemId,
            UUID tenantId,
            String name,
            BigDecimal priceAdjustment,
            ModifierType modifierType,
            int displayOrder
    ) {
        MenuItemModifier modifier = new MenuItemModifier();
        modifier.id = UUID.randomUUID();
        modifier.menuItemId = menuItemId;
        modifier.tenantId = tenantId;
        modifier.name = name;
        modifier.priceAdjustment = priceAdjustment;
        modifier.modifierType = modifierType;
        modifier.available = true;
        modifier.displayOrder = displayOrder;
        modifier.createdAt = Instant.now();
        return modifier;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(UUID menuItemId) {
        this.menuItemId = menuItemId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPriceAdjustment() {
        return priceAdjustment;
    }

    public void setPriceAdjustment(BigDecimal priceAdjustment) {
        this.priceAdjustment = priceAdjustment;
    }

    public ModifierType getModifierType() {
        return modifierType;
    }

    public void setModifierType(ModifierType modifierType) {
        this.modifierType = modifierType;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
