package com.menudigital.domain.tenant;

import java.time.Instant;

public class Restaurant {
    
    private TenantId id;
    private String name;
    private String slug;
    private String ownerEmail;
    private RestaurantTheme theme;
    private Instant createdAt;
    
    public Restaurant() {
        this.theme = RestaurantTheme.defaultTheme();
    }
    
    public Restaurant(TenantId id, String name, String slug, String ownerEmail, RestaurantTheme theme, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.ownerEmail = ownerEmail;
        this.theme = theme != null ? theme : RestaurantTheme.defaultTheme();
        this.createdAt = createdAt;
    }
    
    public static Restaurant create(String name, String slug, String ownerEmail) {
        return new Restaurant(
            TenantId.generate(),
            name,
            slug,
            ownerEmail,
            RestaurantTheme.defaultTheme(),
            Instant.now()
        );
    }
    
    public RestaurantTheme getTheme() {
        return theme;
    }
    
    public void setTheme(RestaurantTheme theme) {
        this.theme = theme;
    }
    
    public TenantId getId() {
        return id;
    }
    
    public void setId(TenantId id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSlug() {
        return slug;
    }
    
    public void setSlug(String slug) {
        this.slug = slug;
    }
    
    public String getOwnerEmail() {
        return ownerEmail;
    }
    
    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
