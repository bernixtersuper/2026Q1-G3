package com.menudigital.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "menu_sections")
public class MenuSectionEntity extends PanacheEntityBase {
    
    @Id
    public UUID id;
    
    @Column(name = "restaurant_id", nullable = false)
    public UUID restaurantId;
    
    @Column(nullable = false)
    public String name;
    
    @Column(name = "display_order", nullable = false)
    public int displayOrder;
    
    @OneToMany(mappedBy = "sectionId", fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    public List<MenuItemEntity> items = new ArrayList<>();
}
