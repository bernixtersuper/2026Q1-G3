package com.menudigital.infrastructure.persistence;

import com.menudigital.domain.menu.Menu;
import com.menudigital.domain.menu.MenuItem;
import com.menudigital.domain.menu.MenuItemModifier;
import com.menudigital.domain.menu.MenuRepository;
import com.menudigital.domain.menu.MenuSection;
import com.menudigital.domain.menu.ModifierType;
import com.menudigital.domain.tenant.TenantId;
import com.menudigital.infrastructure.persistence.entity.MenuItemEntity;
import com.menudigital.infrastructure.persistence.entity.MenuItemModifierEntity;
import com.menudigital.infrastructure.persistence.entity.MenuSectionEntity;
import com.menudigital.infrastructure.persistence.entity.RestaurantEntity;
import com.menudigital.infrastructure.persistence.mapper.MenuMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class MenuRepositoryImpl implements MenuRepository {
    
    @Inject
    EntityManager em;
    
    @Override
    public Menu findByTenantId(TenantId tenantId) {
        RestaurantEntity restaurant = em.find(RestaurantEntity.class, tenantId.value());
        if (restaurant == null) {
            return new Menu(tenantId, "", "", new ArrayList<>());
        }
        
        List<MenuSectionEntity> sections = em.createQuery(
            "SELECT DISTINCT s FROM MenuSectionEntity s " +
            "LEFT JOIN FETCH s.items " +
            "WHERE s.restaurantId = :restaurantId " +
            "ORDER BY s.displayOrder", MenuSectionEntity.class)
            .setParameter("restaurantId", tenantId.value())
            .getResultList();
        
        return MenuMapper.toDomain(restaurant, sections);
    }
    
    @Override
    public Optional<Menu> findBySlug(String slug) {
        return em.createQuery("SELECT r FROM RestaurantEntity r WHERE r.slug = :slug", RestaurantEntity.class)
            .setParameter("slug", slug)
            .getResultStream()
            .findFirst()
            .map(restaurant -> {
                List<MenuSectionEntity> sections = em.createQuery(
                    "SELECT DISTINCT s FROM MenuSectionEntity s " +
                    "LEFT JOIN FETCH s.items " +
                    "WHERE s.restaurantId = :restaurantId " +
                    "ORDER BY s.displayOrder", MenuSectionEntity.class)
                    .setParameter("restaurantId", restaurant.id)
                    .getResultList();
                return MenuMapper.toDomain(restaurant, sections);
            });
    }
    
    @Override
    @Transactional
    public MenuSection saveSection(MenuSection section) {
        MenuSectionEntity entity = MenuMapper.toSectionEntity(section);
        em.persist(entity);
        return section;
    }
    
    @Override
    public Optional<MenuSection> findSectionById(UUID id) {
        MenuSectionEntity entity = em.find(MenuSectionEntity.class, id);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(MenuMapper.toSectionDomainWithoutItems(entity));
    }
    
    @Override
    @Transactional
    public void updateSection(MenuSection section) {
        MenuSectionEntity entity = em.find(MenuSectionEntity.class, section.getId());
        if (entity != null) {
            MenuMapper.updateSectionEntity(entity, section);
        }
    }
    
    @Override
    @Transactional
    public void deleteSection(UUID id) {
        MenuSectionEntity entity = em.find(MenuSectionEntity.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }
    
    @Override
    public boolean sectionBelongsToTenant(UUID sectionId, TenantId tenantId) {
        Long count = em.createQuery(
            "SELECT COUNT(s) FROM MenuSectionEntity s WHERE s.id = :id AND s.restaurantId = :restaurantId", Long.class)
            .setParameter("id", sectionId)
            .setParameter("restaurantId", tenantId.value())
            .getSingleResult();
        return count > 0;
    }
    
    @Override
    @Transactional
    public MenuItem saveItem(MenuItem item) {
        MenuItemEntity entity = MenuMapper.toItemEntity(item);
        em.persist(entity);
        return item;
    }
    
    @Override
    public Optional<MenuItem> findItemById(UUID id) {
        MenuItemEntity entity = em.find(MenuItemEntity.class, id);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(MenuMapper.toItemDomainSimple(entity));
    }
    
    @Override
    public List<MenuItem> findItemsByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        
        return em.createQuery(
            "SELECT i FROM MenuItemEntity i WHERE i.id IN :ids", MenuItemEntity.class)
            .setParameter("ids", ids)
            .getResultStream()
            .map(MenuMapper::toItemDomainSimple)
            .toList();
    }
    
    @Override
    @Transactional
    public void updateItem(MenuItem item) {
        MenuItemEntity entity = em.find(MenuItemEntity.class, item.getId());
        if (entity != null) {
            MenuMapper.updateItemEntity(entity, item);
        }
    }
    
    @Override
    @Transactional
    public void deleteItem(UUID id) {
        MenuItemEntity entity = em.find(MenuItemEntity.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }
    
    @Override
    public boolean itemBelongsToTenant(UUID itemId, TenantId tenantId) {
        Long count = em.createQuery(
            "SELECT COUNT(i) FROM MenuItemEntity i WHERE i.id = :id AND i.restaurantId = :restaurantId", Long.class)
            .setParameter("id", itemId)
            .setParameter("restaurantId", tenantId.value())
            .getSingleResult();
        return count > 0;
    }
    
    @Override
    @Transactional
    public MenuItemModifier saveModifier(MenuItemModifier modifier) {
        MenuItemModifierEntity entity = toModifierEntity(modifier);
        em.persist(entity);
        return modifier;
    }
    
    @Override
    public Optional<MenuItemModifier> findModifierById(UUID id) {
        MenuItemModifierEntity entity = em.find(MenuItemModifierEntity.class, id);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toModifierDomain(entity));
    }
    
    @Override
    public List<MenuItemModifier> findModifiersByItemId(UUID itemId) {
        return em.createQuery(
            "SELECT m FROM MenuItemModifierEntity m WHERE m.menuItemId = :itemId ORDER BY m.displayOrder", 
            MenuItemModifierEntity.class)
            .setParameter("itemId", itemId)
            .getResultStream()
            .map(this::toModifierDomain)
            .toList();
    }
    
    @Override
    @Transactional
    public void updateModifier(MenuItemModifier modifier) {
        MenuItemModifierEntity entity = em.find(MenuItemModifierEntity.class, modifier.getId());
        if (entity != null) {
            entity.name = modifier.getName();
            entity.priceAdjustment = modifier.getPriceAdjustment();
            entity.modifierType = modifier.getModifierType().name();
            entity.available = modifier.isAvailable();
            entity.displayOrder = modifier.getDisplayOrder();
        }
    }
    
    @Override
    @Transactional
    public void deleteModifier(UUID id) {
        MenuItemModifierEntity entity = em.find(MenuItemModifierEntity.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }
    
    private MenuItemModifierEntity toModifierEntity(MenuItemModifier modifier) {
        MenuItemModifierEntity entity = new MenuItemModifierEntity();
        entity.id = modifier.getId();
        entity.menuItemId = modifier.getMenuItemId();
        entity.restaurantId = modifier.getTenantId();
        entity.name = modifier.getName();
        entity.priceAdjustment = modifier.getPriceAdjustment();
        entity.modifierType = modifier.getModifierType().name();
        entity.available = modifier.isAvailable();
        entity.displayOrder = modifier.getDisplayOrder();
        entity.createdAt = modifier.getCreatedAt();
        return entity;
    }
    
    private MenuItemModifier toModifierDomain(MenuItemModifierEntity entity) {
        MenuItemModifier modifier = new MenuItemModifier();
        modifier.setId(entity.id);
        modifier.setMenuItemId(entity.menuItemId);
        modifier.setTenantId(entity.restaurantId);
        modifier.setName(entity.name);
        modifier.setPriceAdjustment(entity.priceAdjustment);
        modifier.setModifierType(ModifierType.valueOf(entity.modifierType));
        modifier.setAvailable(entity.available);
        modifier.setDisplayOrder(entity.displayOrder);
        modifier.setCreatedAt(entity.createdAt);
        return modifier;
    }
}
