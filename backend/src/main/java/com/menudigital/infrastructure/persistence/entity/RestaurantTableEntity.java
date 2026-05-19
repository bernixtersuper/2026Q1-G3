package com.menudigital.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "restaurant_tables")
public class RestaurantTableEntity extends PanacheEntityBase {
    
    @Id
    public UUID id;
    
    @Column(name = "restaurant_id", nullable = false)
    public UUID restaurantId;
    
    @Column(name = "table_number", nullable = false)
    public String tableNumber;
    
    @Column(name = "table_name")
    public String tableName;
    
    @Column
    public int capacity = 4;
    
    @Column(name = "qr_code_token", nullable = false, unique = true)
    public String qrCodeToken;
    
    @Column(name = "is_active", nullable = false)
    public boolean isActive = true;
    
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
    
    @Column(name = "position_x")
    public Integer positionX;
    
    @Column(name = "position_y")
    public Integer positionY;
    
    @Column(name = "width")
    public Integer width = 100;
    
    @Column(name = "height")
    public Integer height = 80;
}
