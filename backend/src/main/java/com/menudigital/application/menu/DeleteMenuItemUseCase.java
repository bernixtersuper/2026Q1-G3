package com.menudigital.application.menu;

import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.menu.MenuRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class DeleteMenuItemUseCase {
    
    @Inject
    MenuRepository menuRepository;
    
    @Inject
    TenantContext tenantContext;
    
    @Transactional
    public void execute(UUID itemId) {
        if (!menuRepository.itemBelongsToTenant(itemId, tenantContext.getTenantId())) {
            throw new ItemNotFoundException("Item not found");
        }
        menuRepository.deleteItem(itemId);
    }
    
    public static class ItemNotFoundException extends RuntimeException {
        public ItemNotFoundException(String message) {
            super(message);
        }
    }
}
