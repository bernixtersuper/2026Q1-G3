package com.menudigital.infrastructure.persistence;

import com.menudigital.domain.tenant.Restaurant;
import com.menudigital.domain.tenant.RestaurantRepository;
import com.menudigital.domain.tenant.TenantId;
import com.menudigital.infrastructure.persistence.entity.RestaurantEntity;
import com.menudigital.infrastructure.persistence.mapper.RestaurantMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RestaurantRepositoryImpl implements RestaurantRepository, PanacheRepositoryBase<RestaurantEntity, UUID> {
    
    @Override
    @Transactional
    public Restaurant save(Restaurant restaurant) {
        RestaurantEntity entity = RestaurantMapper.toEntity(restaurant);
        persist(entity);
        return restaurant;
    }
    
    @Override
    public Optional<Restaurant> findById(TenantId id) {
        return findByIdOptional(id.value())
            .map(RestaurantMapper::toDomain);
    }
    
    @Override
    public Optional<Restaurant> findBySlug(String slug) {
        return find("slug", slug)
            .firstResultOptional()
            .map(RestaurantMapper::toDomain);
    }
    
    @Override
    public boolean existsBySlug(String slug) {
        return count("slug", slug) > 0;
    }
    
    @Override
    @Transactional
    public void update(Restaurant restaurant) {
        findByIdOptional(restaurant.getId().value())
            .ifPresent(entity -> RestaurantMapper.updateEntity(entity, restaurant));
    }
}
