package com.menudigital.application.menu;

import com.menudigital.application.menu.dto.MenuDTOs.UpdateSectionCommand;
import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.menu.MenuRepository;
import com.menudigital.domain.menu.MenuSection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class UpdateMenuSectionUseCase {
    
    @Inject
    MenuRepository menuRepository;
    
    @Inject
    TenantContext tenantContext;
    
    @Transactional
    public void execute(UUID sectionId, UpdateSectionCommand command) {
        if (!menuRepository.sectionBelongsToTenant(sectionId, tenantContext.getTenantId())) {
            throw new SectionNotFoundException("Section not found");
        }
        
        MenuSection section = menuRepository.findSectionById(sectionId)
            .orElseThrow(() -> new SectionNotFoundException("Section not found"));
        
        section.setName(command.name());
        section.setDisplayOrder(command.displayOrder());
        
        menuRepository.updateSection(section);
    }
    
    public static class SectionNotFoundException extends RuntimeException {
        public SectionNotFoundException(String message) {
            super(message);
        }
    }
}
