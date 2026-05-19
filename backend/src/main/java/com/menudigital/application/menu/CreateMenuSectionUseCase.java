package com.menudigital.application.menu;

import com.menudigital.application.menu.dto.MenuDTOs.CreateSectionCommand;
import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.menu.MenuRepository;
import com.menudigital.domain.menu.MenuSection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CreateMenuSectionUseCase {
    
    @Inject
    MenuRepository menuRepository;
    
    @Inject
    TenantContext tenantContext;
    
    @Transactional
    public MenuSection execute(CreateSectionCommand command) {
        MenuSection section = MenuSection.create(
            tenantContext.getTenantId(),
            command.name(),
            command.displayOrder()
        );
        return menuRepository.saveSection(section);
    }
}
