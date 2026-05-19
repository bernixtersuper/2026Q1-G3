package com.menudigital.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItemEntity extends PanacheEntityBase {
    
    @Id
    public UUID id;
    
    @Column(name = "order_id", nullable = false)
    public UUID orderId;
    
    @Column(name = "menu_item_id", nullable = false)
    public UUID menuItemId;
    
    @Column(nullable = false)
    public int quantity = 1;
    
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    public BigDecimal unitPrice;
    
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    public BigDecimal basePrice;
    
    @Column(columnDefinition = "TEXT")
    public String notes;
    
    @Column(name = "added_by")
    public String addedBy;
    
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
