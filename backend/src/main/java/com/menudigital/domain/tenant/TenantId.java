package com.menudigital.domain.tenant;

import java.util.UUID;

public record TenantId(UUID value) {
    
    public static TenantId generate() {
        return new TenantId(UUID.randomUUID());
    }
    
    public static TenantId of(String s) {
        return new TenantId(UUID.fromString(s));
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}
