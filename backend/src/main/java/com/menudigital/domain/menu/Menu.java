package com.menudigital.domain.menu;

import com.menudigital.domain.tenant.TenantId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Menu {
    
    private TenantId tenantId;
    private String restaurantName;
    private String slug;
    private List<MenuSection> sections;
    
    public Menu() {
        this.sections = new ArrayList<>();
    }
    
    public Menu(TenantId tenantId, String restaurantName, String slug, List<MenuSection> sections) {
        this.tenantId = tenantId;
        this.restaurantName = restaurantName;
        this.slug = slug;
        this.sections = sections != null ? sections : new ArrayList<>();
    }
    
    public List<MenuSection> getSortedSections() {
        return sections.stream()
            .sorted(Comparator.comparingInt(MenuSection::getDisplayOrder))
            .toList();
    }
    
    public Menu withOnlyAvailableItems() {
        List<MenuSection> filteredSections = sections.stream()
            .map(section -> {
                MenuSection filtered = new MenuSection(
                    section.getId(),
                    section.getTenantId(),
                    section.getName(),
                    section.getDisplayOrder()
                );
                List<MenuItem> availableItems = section.getItems().stream()
                    .filter(MenuItem::isAvailable)
                    .sorted(Comparator.comparingInt(MenuItem::getDisplayOrder))
                    .toList();
                filtered.setItems(new ArrayList<>(availableItems));
                return filtered;
            })
            .filter(section -> !section.getItems().isEmpty())
            .sorted(Comparator.comparingInt(MenuSection::getDisplayOrder))
            .toList();
        
        return new Menu(tenantId, restaurantName, slug, filteredSections);
    }
    
    public TenantId getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }
    
    public String getRestaurantName() {
        return restaurantName;
    }
    
    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }
    
    public String getSlug() {
        return slug;
    }
    
    public void setSlug(String slug) {
        this.slug = slug;
    }
    
    public List<MenuSection> getSections() {
        return sections;
    }
    
    public void setSections(List<MenuSection> sections) {
        this.sections = sections;
    }
}
