package com.menudigital.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "table_sessions")
public class TableSessionEntity extends PanacheEntityBase {
    
    @Id
    public UUID id;
    
    @Column(name = "table_id", nullable = false)
    public UUID tableId;
    
    @Column(name = "restaurant_id", nullable = false)
    public UUID restaurantId;
    
    @Column(name = "session_code", nullable = false)
    public String sessionCode;
    
    @Column(name = "started_at", nullable = false)
    public Instant startedAt;
    
    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;
    
    @Column(name = "is_active", nullable = false)
    public boolean isActive = true;
    
    @Column(name = "created_by")
    public String createdBy;
}
