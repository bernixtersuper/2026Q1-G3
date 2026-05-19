package com.menudigital.domain.menu;

import com.menudigital.domain.tenant.TenantId;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MenuItem {
    
    private UUID id;
    private UUID sectionId;
    private TenantId tenantId;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private boolean available;
    private Set<DietaryTag> dietaryTags;
    private int displayOrder;
    
    public MenuItem() {
        this.dietaryTags = new HashSet<>();
        this.available = true;
    }
    
    public MenuItem(UUID id, UUID sectionId, TenantId tenantId, String name, String description,
                    BigDecimal price, String imageUrl, boolean available,
                    Set<DietaryTag> dietaryTags, int displayOrder) {
        this.id = id;
        this.sectionId = sectionId;
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.available = available;
        this.dietaryTags = dietaryTags != null ? dietaryTags : new HashSet<>();
        this.displayOrder = displayOrder;
    }
    
    public static MenuItem create(UUID sectionId, TenantId tenantId, String name, String description,
                                   BigDecimal price, String imageUrl, Set<DietaryTag> dietaryTags, int displayOrder) {
        return new MenuItem(
            UUID.randomUUID(),
            sectionId,
            tenantId,
            name,
            description,
            price,
            imageUrl,
            true,
            dietaryTags,
            displayOrder
        );
    }
    
    public void toggleAvailability() {
        this.available = !this.available;
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getSectionId() {
        return sectionId;
    }
    
    public void setSectionId(UUID sectionId) {
        this.sectionId = sectionId;
    }
    
    public TenantId getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public boolean isAvailable() {
        return available;
    }
    
    public void setAvailable(boolean available) {
        this.available = available;
    }
    
    public Set<DietaryTag> getDietaryTags() {
        return dietaryTags;
    }
    
    public void setDietaryTags(Set<DietaryTag> dietaryTags) {
        this.dietaryTags = dietaryTags;
    }
    
    public int getDisplayOrder() {
        return displayOrder;
    }
    
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
