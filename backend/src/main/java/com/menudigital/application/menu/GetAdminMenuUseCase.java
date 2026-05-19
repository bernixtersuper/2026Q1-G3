package com.menudigital.application.menu;

import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.menu.Menu;
import com.menudigital.domain.menu.MenuRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GetAdminMenuUseCase {
    
    @Inject
    MenuRepository menuRepository;
    
    @Inject
    TenantContext tenantContext;
    
    public Menu execute() {
        return menuRepository.findByTenantId(tenantContext.getTenantId());
    }
}
