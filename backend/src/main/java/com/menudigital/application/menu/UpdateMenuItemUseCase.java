package com.menudigital.application.menu;

import com.menudigital.application.menu.dto.MenuDTOs.UpdateItemCommand;
import com.menudigital.application.shared.TenantContext;
import com.menudigital.infrastructure.storage.MenuImageUrls;
import com.menudigital.domain.menu.MenuItem;
import com.menudigital.domain.menu.MenuRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.HashSet;
import java.util.UUID;

@ApplicationScoped
public class UpdateMenuItemUseCase {
    
    @Inject
    MenuRepository menuRepository;
    
    @Inject
    TenantContext tenantContext;

    @Inject
    MenuImageUrls menuImageUrls;
    
    @Transactional
    public void execute(UUID itemId, UpdateItemCommand command) {
        if (!menuRepository.itemBelongsToTenant(itemId, tenantContext.getTenantId())) {
            throw new ItemNotFoundException("Item not found");
        }
        
        if (!menuRepository.sectionBelongsToTenant(command.sectionId(), tenantContext.getTenantId())) {
            throw new SectionNotFoundException("Section not found");
        }
        
        MenuItem item = menuRepository.findItemById(itemId)
            .orElseThrow(() -> new ItemNotFoundException("Item not found"));
        
        item.setSectionId(command.sectionId());
        item.setName(command.name());
        item.setDescription(command.description());
        item.setPrice(command.price());
        item.setImageUrl(menuImageUrls.normalizeForStorage(command.imageUrl()));
        item.setDietaryTags(command.dietaryTags() != null ? command.dietaryTags() : new HashSet<>());
        item.setDisplayOrder(command.displayOrder());
        
        menuRepository.updateItem(item);
    }
    
    public static class ItemNotFoundException extends RuntimeException {
        public ItemNotFoundException(String message) {
            super(message);
        }
    }
    
    public static class SectionNotFoundException extends RuntimeException {
        public SectionNotFoundException(String message) {
            super(message);
        }
    }
}
