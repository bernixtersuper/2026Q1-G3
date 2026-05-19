package com.menudigital.domain.menu;

import com.menudigital.domain.tenant.TenantId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuRepository {
    
    Menu findByTenantId(TenantId tenantId);
    
    Optional<Menu> findBySlug(String slug);
    
    MenuSection saveSection(MenuSection section);
    
    Optional<MenuSection> findSectionById(UUID id);
    
    void updateSection(MenuSection section);
    
    void deleteSection(UUID id);
    
    boolean sectionBelongsToTenant(UUID sectionId, TenantId tenantId);
    
    MenuItem saveItem(MenuItem item);
    
    Optional<MenuItem> findItemById(UUID id);
    
    List<MenuItem> findItemsByIds(List<UUID> ids);
    
    void updateItem(MenuItem item);
    
    void deleteItem(UUID id);
    
    boolean itemBelongsToTenant(UUID itemId, TenantId tenantId);
    
    MenuItemModifier saveModifier(MenuItemModifier modifier);
    
    Optional<MenuItemModifier> findModifierById(UUID id);
    
    List<MenuItemModifier> findModifiersByItemId(UUID itemId);
    
    void updateModifier(MenuItemModifier modifier);
    
    void deleteModifier(UUID id);
}
