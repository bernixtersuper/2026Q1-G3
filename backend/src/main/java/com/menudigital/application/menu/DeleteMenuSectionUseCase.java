package com.menudigital.application.menu;

import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.menu.MenuRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class DeleteMenuSectionUseCase {
    
    @Inject
    MenuRepository menuRepository;
    
    @Inject
    TenantContext tenantContext;
    
    @Transactional
    public void execute(UUID sectionId) {
        if (!menuRepository.sectionBelongsToTenant(sectionId, tenantContext.getTenantId())) {
            throw new SectionNotFoundException("Section not found");
        }
        menuRepository.deleteSection(sectionId);
    }
    
    public static class SectionNotFoundException extends RuntimeException {
        public SectionNotFoundException(String message) {
            super(message);
        }
    }
}
