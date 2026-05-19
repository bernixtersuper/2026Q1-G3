package com.menudigital.domain.menu;

import java.util.UUID;

public record MenuId(UUID value) {
    
    public static MenuId generate() {
        return new MenuId(UUID.randomUUID());
    }
    
    public static MenuId of(String s) {
        return new MenuId(UUID.fromString(s));
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}
