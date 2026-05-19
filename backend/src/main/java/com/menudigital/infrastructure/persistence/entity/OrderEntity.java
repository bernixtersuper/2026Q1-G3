package com.menudigital.infrastructure.persistence.entity;

import com.menudigital.domain.order.OrderStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity extends PanacheEntityBase {
    
    @Id
    public UUID id;
    
    @Column(name = "restaurant_id", nullable = false)
    public UUID restaurantId;
    
    @Column(name = "table_id", nullable = false)
    public UUID tableId;
    
    @Column(name = "session_id")
    public UUID sessionId;
    
    @Column(name = "order_number")
    public int orderNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public OrderStatus status = OrderStatus.DRAFT;
    
    @Column(columnDefinition = "TEXT")
    public String notes;
    
    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal subtotal = BigDecimal.ZERO;
    
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
    
    @Column(name = "submitted_at")
    public Instant submittedAt;
    
    @Column(name = "confirmed_at")
    public Instant confirmedAt;
    
    @OneToMany(mappedBy = "orderId", fetch = FetchType.LAZY)
    public List<OrderItemEntity> items = new ArrayList<>();
}
