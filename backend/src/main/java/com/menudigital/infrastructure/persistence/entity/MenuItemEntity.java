package com.menudigital.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "menu_items")
public class MenuItemEntity extends PanacheEntityBase {
    
    @Id
    public UUID id;
    
    @Column(name = "section_id", nullable = false)
    public UUID sectionId;
    
    @Column(name = "restaurant_id", nullable = false)
    public UUID restaurantId;
    
    @Column(nullable = false)
    public String name;
    
    @Column(columnDefinition = "TEXT")
    public String description;
    
    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal price;
    
    @Column(name = "image_url", columnDefinition = "TEXT")
    public String imageUrl;
    
    @Column(nullable = false)
    public boolean available = true;
    
    @Column(name = "dietary_tags", columnDefinition = "TEXT[]")
    public String[] dietaryTags = new String[0];
    
    @Column(name = "display_order", nullable = false)
    public int displayOrder;
}
