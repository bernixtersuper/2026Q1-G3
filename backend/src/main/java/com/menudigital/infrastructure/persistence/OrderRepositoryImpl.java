package com.menudigital.infrastructure.persistence;

import com.menudigital.domain.order.Order;
import com.menudigital.domain.order.OrderItem;
import com.menudigital.domain.order.OrderRepository;
import com.menudigital.domain.order.OrderStatus;
import com.menudigital.domain.order.SelectedModifier;
import com.menudigital.domain.tenant.TenantId;
import com.menudigital.infrastructure.persistence.entity.MenuItemEntity;
import com.menudigital.infrastructure.persistence.entity.OrderEntity;
import com.menudigital.infrastructure.persistence.entity.OrderItemEntity;
import com.menudigital.infrastructure.persistence.entity.OrderItemModifierEntity;
import com.menudigital.infrastructure.persistence.entity.RestaurantTableEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OrderRepositoryImpl implements OrderRepository {
    
    @Inject
    EntityManager em;
    
    @Override
    @Transactional
    public Order save(Order order) {
        OrderEntity entity = toEntity(order);
        em.persist(entity);
        return order;
    }
    
    @Override
    public Optional<Order> findById(UUID id) {
        OrderEntity entity = em.find(OrderEntity.class, id);
        if (entity == null) return Optional.empty();
        
        Order order = toDomain(entity);
        order.setItems(findItemsByOrderId(id));
        return Optional.of(order);
    }
    
    @Override
    public Optional<Order> findDraftBySessionId(UUID sessionId) {
        List<OrderEntity> entities = em.createQuery(
            "SELECT o FROM OrderEntity o WHERE o.sessionId = :sid AND o.status = :status", 
            OrderEntity.class)
            .setParameter("sid", sessionId)
            .setParameter("status", OrderStatus.DRAFT)
            .getResultList();
        
        if (entities.isEmpty()) {
            return Optional.empty();
        }
        
        OrderEntity entity = entities.get(0);
        Order order = toDomain(entity);
        order.setItems(findItemsByOrderId(entity.id));
        return Optional.of(order);
    }
    
    @Override
    public Optional<Order> findCurrentBySessionId(UUID sessionId) {
        List<OrderEntity> entities = em.createQuery(
            "SELECT o FROM OrderEntity o WHERE o.sessionId = :sid AND o.status != :cancelled ORDER BY o.createdAt DESC", 
            OrderEntity.class)
            .setParameter("sid", sessionId)
            .setParameter("cancelled", OrderStatus.CANCELLED)
            .setMaxResults(1)
            .getResultList();
        
        if (entities.isEmpty()) {
            return Optional.empty();
        }
        
        OrderEntity entity = entities.get(0);
        Order order = toDomain(entity);
        order.setItems(findItemsByOrderId(entity.id));
        return Optional.of(order);
    }
    
    @Override
    public List<Order> findByTenantId(TenantId tenantId) {
        List<OrderEntity> entities = em.createQuery(
            "SELECT o FROM OrderEntity o WHERE o.restaurantId = :rid ORDER BY o.createdAt DESC", 
            OrderEntity.class)
            .setParameter("rid", tenantId.value())
            .getResultList();
        
        return entities.stream()
            .map(entity -> {
                Order order = toDomain(entity);
                order.setItems(findItemsByOrderId(entity.id));
                return order;
            })
            .toList();
    }
    
    @Override
    public List<Order> findActiveByTenantId(TenantId tenantId) {
        List<OrderEntity> entities = em.createQuery(
            "SELECT o FROM OrderEntity o WHERE o.restaurantId = :rid AND o.status NOT IN (:delivered, :cancelled) ORDER BY o.createdAt DESC", 
            OrderEntity.class)
            .setParameter("rid", tenantId.value())
            .setParameter("delivered", OrderStatus.DELIVERED)
            .setParameter("cancelled", OrderStatus.CANCELLED)
            .getResultList();
        
        return entities.stream()
            .map(entity -> {
                Order order = toDomain(entity);
                order.setItems(findItemsByOrderId(entity.id));
                return order;
            })
            .toList();
    }
    
    @Override
    public List<Order> findByTableId(UUID tableId) {
        List<OrderEntity> entities = em.createQuery(
            "SELECT o FROM OrderEntity o WHERE o.tableId = :tid ORDER BY o.createdAt DESC", 
            OrderEntity.class)
            .setParameter("tid", tableId)
            .getResultList();
        
        return entities.stream()
            .map(entity -> {
                Order order = toDomain(entity);
                order.setItems(findItemsByOrderId(entity.id));
                return order;
            })
            .toList();
    }
    
    @Override
    @Transactional
    public void update(Order order) {
        OrderEntity entity = em.find(OrderEntity.class, order.getId());
        if (entity != null) {
            entity.status = order.getStatus();
            entity.notes = order.getNotes();
            entity.subtotal = order.getSubtotal();
            entity.updatedAt = order.getUpdatedAt();
            entity.submittedAt = order.getSubmittedAt();
            entity.confirmedAt = order.getConfirmedAt();
            entity.orderNumber = order.getOrderNumber();
        }
    }
    
    @Override
    @Transactional
    public void saveItem(OrderItem item) {
        OrderItemEntity entity = toItemEntity(item);
        em.persist(entity);
    }
    
    @Override
    @Transactional
    public void updateItem(OrderItem item) {
        OrderItemEntity entity = em.find(OrderItemEntity.class, item.getId());
        if (entity != null) {
            entity.quantity = item.getQuantity();
            entity.notes = item.getNotes();
        }
    }
    
    @Override
    @Transactional
    public void deleteItem(UUID itemId) {
        OrderItemEntity entity = em.find(OrderItemEntity.class, itemId);
        if (entity != null) {
            em.remove(entity);
        }
    }
    
    @Override
    public List<OrderItem> findItemsByOrderId(UUID orderId) {
        List<OrderItemEntity> entities = em.createQuery(
            "SELECT i FROM OrderItemEntity i WHERE i.orderId = :oid ORDER BY i.createdAt", 
            OrderItemEntity.class)
            .setParameter("oid", orderId)
            .getResultList();
        
        return entities.stream()
            .map(entity -> {
                OrderItem item = toItemDomain(entity);
                item.setSelectedModifiers(findModifiersByOrderItemId(entity.id));
                return item;
            })
            .toList();
    }
    
    @Override
    @Transactional
    public void saveItemModifiers(UUID orderItemId, List<SelectedModifier> modifiers) {
        for (SelectedModifier modifier : modifiers) {
            OrderItemModifierEntity entity = new OrderItemModifierEntity();
            entity.id = modifier.getId();
            entity.orderItemId = orderItemId;
            entity.modifierId = modifier.getModifierId();
            entity.modifierName = modifier.getName();
            entity.priceAdjustment = modifier.getPriceAdjustment();
            entity.modifierType = modifier.getModifierType();
            entity.createdAt = modifier.getCreatedAt();
            em.persist(entity);
        }
    }
    
    @Override
    public List<SelectedModifier> findModifiersByOrderItemId(UUID orderItemId) {
        List<OrderItemModifierEntity> entities = em.createQuery(
            "SELECT m FROM OrderItemModifierEntity m WHERE m.orderItemId = :itemId", 
            OrderItemModifierEntity.class)
            .setParameter("itemId", orderItemId)
            .getResultList();
        
        return entities.stream()
            .map(this::toModifierDomain)
            .toList();
    }
    
    private SelectedModifier toModifierDomain(OrderItemModifierEntity entity) {
        return new SelectedModifier(
            entity.id,
            entity.orderItemId,
            entity.modifierId,
            entity.modifierName,
            entity.priceAdjustment,
            entity.modifierType,
            entity.createdAt
        );
    }
    
    @Override
    @Transactional
    public int getNextOrderNumber(TenantId tenantId) {
        LocalDate today = LocalDate.now();
        
        var result = em.createNativeQuery(
            "INSERT INTO order_sequences (restaurant_id, date, last_number) " +
            "VALUES (:rid, :date, 1) " +
            "ON CONFLICT (restaurant_id, date) DO UPDATE SET last_number = order_sequences.last_number + 1 " +
            "RETURNING last_number")
            .setParameter("rid", tenantId.value())
            .setParameter("date", today)
            .getSingleResult();
        
        return ((Number) result).intValue();
    }
    
    @Override
    @Transactional
    public void cancelDraftOrdersBySessionId(UUID sessionId) {
        em.createQuery(
            "UPDATE OrderEntity o SET o.status = :cancelled, o.updatedAt = :now " +
            "WHERE o.sessionId = :sid AND o.status = :draft")
            .setParameter("cancelled", OrderStatus.CANCELLED)
            .setParameter("now", Instant.now())
            .setParameter("sid", sessionId)
            .setParameter("draft", OrderStatus.DRAFT)
            .executeUpdate();
    }
    
    private OrderEntity toEntity(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.id = order.getId();
        entity.restaurantId = order.getTenantId().value();
        entity.tableId = order.getTableId();
        entity.sessionId = order.getSessionId();
        entity.orderNumber = order.getOrderNumber();
        entity.status = order.getStatus();
        entity.notes = order.getNotes();
        entity.subtotal = order.getSubtotal();
        entity.createdAt = order.getCreatedAt();
        entity.updatedAt = order.getUpdatedAt();
        entity.submittedAt = order.getSubmittedAt();
        entity.confirmedAt = order.getConfirmedAt();
        return entity;
    }
    
    private Order toDomain(OrderEntity entity) {
        RestaurantTableEntity table = em.find(RestaurantTableEntity.class, entity.tableId);
        String tableNumber = table != null ? table.tableNumber : "Unknown";
        
        return new Order(
            entity.id,
            new TenantId(entity.restaurantId),
            entity.tableId,
            entity.sessionId,
            tableNumber,
            entity.orderNumber,
            entity.status,
            entity.notes,
            entity.subtotal,
            entity.createdAt,
            entity.updatedAt,
            entity.submittedAt,
            entity.confirmedAt
        );
    }
    
    private OrderItemEntity toItemEntity(OrderItem item) {
        OrderItemEntity entity = new OrderItemEntity();
        entity.id = item.getId();
        entity.orderId = item.getOrderId();
        entity.menuItemId = item.getMenuItemId();
        entity.quantity = item.getQuantity();
        entity.unitPrice = item.getUnitPrice();
        entity.basePrice = item.getBasePrice() != null ? item.getBasePrice() : item.getUnitPrice();
        entity.notes = item.getNotes();
        entity.addedBy = item.getAddedBy();
        entity.createdAt = item.getCreatedAt();
        return entity;
    }
    
    private OrderItem toItemDomain(OrderItemEntity entity) {
        MenuItemEntity menuItem = em.find(MenuItemEntity.class, entity.menuItemId);
        String itemName = menuItem != null ? menuItem.name : "Unknown Item";
        
        return new OrderItem(
            entity.id,
            entity.orderId,
            entity.menuItemId,
            itemName,
            entity.quantity,
            entity.unitPrice,
            entity.basePrice,
            entity.notes,
            entity.addedBy,
            entity.createdAt
        );
    }
}
