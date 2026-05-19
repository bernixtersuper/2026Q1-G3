package com.menudigital.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity extends PanacheEntityBase {
    
    @Id
    public UUID id;
    
    @Column(nullable = false, unique = true)
    public String email;
    
    @Column(name = "password_hash")
    public String passwordHash;

    @Column(name = "cognito_sub", unique = true)
    public String cognitoSub;

    @Column(name = "restaurant_id")
    public UUID restaurantId;
    
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
