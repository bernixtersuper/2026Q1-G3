package com.menudigital.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "menu_item_modifiers")
public class MenuItemModifierEntity {
    
    @Id
    public UUID id;
    
    @Column(name = "menu_item_id", nullable = false)
    public UUID menuItemId;
    
    @Column(name = "restaurant_id", nullable = false)
    public UUID restaurantId;
    
    @Column(nullable = false, length = 100)
    public String name;
    
    @Column(name = "price_adjustment", nullable = false, precision = 10, scale = 2)
    public BigDecimal priceAdjustment;
    
    @Column(name = "modifier_type", nullable = false, length = 20)
    public String modifierType;
    
    @Column(name = "is_available", nullable = false)
    public boolean available;
    
    @Column(name = "display_order", nullable = false)
    public int displayOrder;
    
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
