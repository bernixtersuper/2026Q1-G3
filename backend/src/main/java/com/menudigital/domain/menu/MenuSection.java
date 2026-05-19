package com.menudigital.domain.menu;

import com.menudigital.domain.tenant.TenantId;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MenuSection {
    
    private UUID id;
    private TenantId tenantId;
    private String name;
    private int displayOrder;
    private List<MenuItem> items;
    
    public MenuSection() {
        this.items = new ArrayList<>();
    }
    
    public MenuSection(UUID id, TenantId tenantId, String name, int displayOrder) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.displayOrder = displayOrder;
        this.items = new ArrayList<>();
    }
    
    public static MenuSection create(TenantId tenantId, String name, int displayOrder) {
        return new MenuSection(UUID.randomUUID(), tenantId, name, displayOrder);
    }
    
    public void addItem(MenuItem item) {
        this.items.add(item);
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
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
    
    public int getDisplayOrder() {
        return displayOrder;
    }
    
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
    
    public List<MenuItem> getItems() {
        return items;
    }
    
    public void setItems(List<MenuItem> items) {
        this.items = items;
    }
}
