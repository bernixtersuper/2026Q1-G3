package com.menudigital.application.menu;

import com.menudigital.domain.menu.Menu;
import com.menudigital.domain.menu.MenuRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class GetPublicMenuUseCase {
    
    @Inject
    MenuRepository menuRepository;
    
    public Optional<Menu> execute(String slug) {
        return menuRepository.findBySlug(slug)
            .map(Menu::withOnlyAvailableItems);
    }
}
