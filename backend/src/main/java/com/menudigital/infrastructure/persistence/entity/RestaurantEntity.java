package com.menudigital.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "restaurants")
public class RestaurantEntity extends PanacheEntityBase {
    
    @Id
    public UUID id;
    
    @Column(nullable = false)
    public String name;
    
    @Column(nullable = false, unique = true)
    public String slug;
    
    @Column(name = "owner_email", nullable = false)
    public String ownerEmail;
    
    @Column(name = "theme", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> theme;
    
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
