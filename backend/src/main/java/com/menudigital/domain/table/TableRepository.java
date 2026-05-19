package com.menudigital.domain.table;

import com.menudigital.domain.tenant.TenantId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TableRepository {
    
    RestaurantTable save(RestaurantTable table);
    
    Optional<RestaurantTable> findById(UUID id);
    
    Optional<RestaurantTable> findByQrToken(String qrToken);
    
    List<RestaurantTable> findByTenantId(TenantId tenantId);
    
    void update(RestaurantTable table);
    
    void delete(UUID id);
    
    boolean existsByTableNumber(TenantId tenantId, String tableNumber);
    
    TableSession saveSession(TableSession session);
    
    Optional<TableSession> findActiveSessionByTableId(UUID tableId);
    
    Optional<TableSession> findSessionByCode(String sessionCode, UUID tableId);
    
    Optional<TableSession> findSessionById(UUID sessionId);
    
    List<TableSession> findActiveSessionsByTenantId(TenantId tenantId);
    
    void updateSession(TableSession session);
    
    void deactivateSession(UUID sessionId);
}
