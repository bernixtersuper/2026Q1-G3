package com.menudigital.domain.tenant;

import java.util.Optional;

public interface RestaurantRepository {
    
    Restaurant save(Restaurant restaurant);
    
    Optional<Restaurant> findById(TenantId id);
    
    Optional<Restaurant> findBySlug(String slug);
    
    boolean existsBySlug(String slug);
    
    void update(Restaurant restaurant);
}
