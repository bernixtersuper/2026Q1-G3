package com.menudigital.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_item_modifiers")
public class OrderItemModifierEntity extends PanacheEntityBase {
    
    @Id
    public UUID id;
    
    @Column(name = "order_item_id", nullable = false)
    public UUID orderItemId;
    
    @Column(name = "modifier_id", nullable = false)
    public UUID modifierId;
    
    @Column(name = "modifier_name", nullable = false)
    public String modifierName;
    
    @Column(name = "price_adjustment", nullable = false, precision = 10, scale = 2)
    public BigDecimal priceAdjustment = BigDecimal.ZERO;
    
    @Column(name = "modifier_type", nullable = false)
    public String modifierType;
    
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
