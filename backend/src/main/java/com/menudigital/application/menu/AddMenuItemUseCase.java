package com.menudigital.application.menu;

import com.menudigital.application.menu.dto.MenuDTOs.CreateItemCommand;
import com.menudigital.application.shared.TenantContext;
import com.menudigital.infrastructure.storage.MenuImageUrls;
import com.menudigital.domain.menu.MenuItem;
import com.menudigital.domain.menu.MenuRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.HashSet;

@ApplicationScoped
public class AddMenuItemUseCase {
    
    @Inject
    MenuRepository menuRepository;
    
    @Inject
    TenantContext tenantContext;

    @Inject
    MenuImageUrls menuImageUrls;
    
    @Transactional
    public MenuItem execute(CreateItemCommand command) {
        if (!menuRepository.sectionBelongsToTenant(command.sectionId(), tenantContext.getTenantId())) {
            throw new SectionNotFoundException("Section not found");
        }
        
        MenuItem item = MenuItem.create(
            command.sectionId(),
            tenantContext.getTenantId(),
            command.name(),
            command.description(),
            command.price(),
            menuImageUrls.normalizeForStorage(command.imageUrl()),
            command.dietaryTags() != null ? command.dietaryTags() : new HashSet<>(),
            command.displayOrder()
        );
        
        return menuRepository.saveItem(item);
    }
    
    public static class SectionNotFoundException extends RuntimeException {
        public SectionNotFoundException(String message) {
            super(message);
        }
    }
}
