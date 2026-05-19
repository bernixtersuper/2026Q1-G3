package com.menudigital.domain.order;

import com.menudigital.domain.tenant.TenantId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    
    Order save(Order order);
    
    Optional<Order> findById(UUID id);
    
    Optional<Order> findDraftBySessionId(UUID sessionId);
    
    Optional<Order> findCurrentBySessionId(UUID sessionId);
    
    List<Order> findByTenantId(TenantId tenantId);
    
    List<Order> findActiveByTenantId(TenantId tenantId);
    
    List<Order> findByTableId(UUID tableId);
    
    void update(Order order);
    
    void saveItem(OrderItem item);
    
    void saveItemModifiers(UUID orderItemId, List<SelectedModifier> modifiers);
    
    void updateItem(OrderItem item);
    
    void deleteItem(UUID itemId);
    
    List<OrderItem> findItemsByOrderId(UUID orderId);
    
    List<SelectedModifier> findModifiersByOrderItemId(UUID orderItemId);
    
    int getNextOrderNumber(TenantId tenantId);
    
    void cancelDraftOrdersBySessionId(UUID sessionId);
}
