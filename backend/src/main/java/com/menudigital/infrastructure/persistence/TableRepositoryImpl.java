package com.menudigital.infrastructure.persistence;

import com.menudigital.domain.table.RestaurantTable;
import com.menudigital.domain.table.TableRepository;
import com.menudigital.domain.table.TableSession;
import com.menudigital.domain.tenant.TenantId;
import com.menudigital.infrastructure.persistence.entity.RestaurantTableEntity;
import com.menudigital.infrastructure.persistence.entity.TableSessionEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TableRepositoryImpl implements TableRepository {
    
    @Inject
    EntityManager em;
    
    @Override
    @Transactional
    public RestaurantTable save(RestaurantTable table) {
        RestaurantTableEntity entity = toEntity(table);
        em.persist(entity);
        return table;
    }
    
    @Override
    public Optional<RestaurantTable> findById(UUID id) {
        RestaurantTableEntity entity = em.find(RestaurantTableEntity.class, id);
        return Optional.ofNullable(entity).map(this::toDomain);
    }
    
    @Override
    public Optional<RestaurantTable> findByQrToken(String qrToken) {
        return em.createQuery("SELECT t FROM RestaurantTableEntity t WHERE t.qrCodeToken = :token", 
                RestaurantTableEntity.class)
            .setParameter("token", qrToken)
            .getResultStream()
            .findFirst()
            .map(this::toDomain);
    }
    
    @Override
    public List<RestaurantTable> findByTenantId(TenantId tenantId) {
        return em.createQuery("SELECT t FROM RestaurantTableEntity t WHERE t.restaurantId = :rid ORDER BY t.tableNumber", 
                RestaurantTableEntity.class)
            .setParameter("rid", tenantId.value())
            .getResultStream()
            .map(this::toDomain)
            .toList();
    }
    
    @Override
    @Transactional
    public void update(RestaurantTable table) {
        RestaurantTableEntity entity = em.find(RestaurantTableEntity.class, table.getId());
        if (entity != null) {
            entity.tableNumber = table.getTableNumber();
            entity.tableName = table.getTableName();
            entity.capacity = table.getCapacity();
            entity.qrCodeToken = table.getQrCodeToken();
            entity.isActive = table.isActive();
            entity.positionX = table.getPositionX();
            entity.positionY = table.getPositionY();
            entity.width = table.getWidth();
            entity.height = table.getHeight();
        }
    }
    
    @Override
    @Transactional
    public void delete(UUID id) {
        RestaurantTableEntity entity = em.find(RestaurantTableEntity.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }
    
    @Override
    public boolean existsByTableNumber(TenantId tenantId, String tableNumber) {
        Long count = em.createQuery(
            "SELECT COUNT(t) FROM RestaurantTableEntity t WHERE t.restaurantId = :rid AND t.tableNumber = :num", 
            Long.class)
            .setParameter("rid", tenantId.value())
            .setParameter("num", tableNumber)
            .getSingleResult();
        return count > 0;
    }
    
    @Override
    @Transactional
    public TableSession saveSession(TableSession session) {
        TableSessionEntity entity = toSessionEntity(session);
        em.persist(entity);
        return session;
    }
    
    @Override
    public Optional<TableSession> findActiveSessionByTableId(UUID tableId) {
        return em.createQuery(
            "SELECT s FROM TableSessionEntity s WHERE s.tableId = :tid AND s.isActive = true AND s.expiresAt > :now", 
            TableSessionEntity.class)
            .setParameter("tid", tableId)
            .setParameter("now", Instant.now())
            .getResultStream()
            .findFirst()
            .map(this::toSessionDomain);
    }
    
    @Override
    public Optional<TableSession> findSessionByCode(String sessionCode, UUID tableId) {
        return em.createQuery(
            "SELECT s FROM TableSessionEntity s WHERE s.sessionCode = :code AND s.tableId = :tid AND s.isActive = true AND s.expiresAt > :now", 
            TableSessionEntity.class)
            .setParameter("code", sessionCode)
            .setParameter("tid", tableId)
            .setParameter("now", Instant.now())
            .getResultStream()
            .findFirst()
            .map(this::toSessionDomain);
    }
    
    @Override
    public Optional<TableSession> findSessionById(UUID sessionId) {
        TableSessionEntity entity = em.find(TableSessionEntity.class, sessionId);
        return Optional.ofNullable(entity).map(this::toSessionDomain);
    }
    
    @Override
    public List<TableSession> findActiveSessionsByTenantId(TenantId tenantId) {
        return em.createQuery(
            "SELECT s FROM TableSessionEntity s WHERE s.restaurantId = :rid AND s.isActive = true AND s.expiresAt > :now ORDER BY s.expiresAt",
            TableSessionEntity.class)
            .setParameter("rid", tenantId.value())
            .setParameter("now", Instant.now())
            .getResultList()
            .stream()
            .map(this::toSessionDomain)
            .toList();
    }
    
    @Override
    @Transactional
    public void updateSession(TableSession session) {
        TableSessionEntity entity = em.find(TableSessionEntity.class, session.getId());
        if (entity != null) {
            entity.expiresAt = session.getExpiresAt();
            entity.isActive = session.isActive();
        }
    }
    
    @Override
    @Transactional
    public void deactivateSession(UUID sessionId) {
        TableSessionEntity entity = em.find(TableSessionEntity.class, sessionId);
        if (entity != null) {
            entity.isActive = false;
        }
    }
    
    private RestaurantTableEntity toEntity(RestaurantTable table) {
        RestaurantTableEntity entity = new RestaurantTableEntity();
        entity.id = table.getId();
        entity.restaurantId = table.getTenantId().value();
        entity.tableNumber = table.getTableNumber();
        entity.tableName = table.getTableName();
        entity.capacity = table.getCapacity();
        entity.qrCodeToken = table.getQrCodeToken();
        entity.isActive = table.isActive();
        entity.createdAt = table.getCreatedAt();
        entity.positionX = table.getPositionX();
        entity.positionY = table.getPositionY();
        entity.width = table.getWidth();
        entity.height = table.getHeight();
        return entity;
    }
    
    private RestaurantTable toDomain(RestaurantTableEntity entity) {
        RestaurantTable table = new RestaurantTable(
            entity.id,
            new TenantId(entity.restaurantId),
            entity.tableNumber,
            entity.tableName,
            entity.capacity,
            entity.qrCodeToken,
            entity.isActive,
            entity.createdAt
        );
        table.setPositionX(entity.positionX);
        table.setPositionY(entity.positionY);
        table.setWidth(entity.width);
        table.setHeight(entity.height);
        return table;
    }
    
    private TableSessionEntity toSessionEntity(TableSession session) {
        TableSessionEntity entity = new TableSessionEntity();
        entity.id = session.getId();
        entity.tableId = session.getTableId();
        entity.restaurantId = session.getTenantId().value();
        entity.sessionCode = session.getSessionCode();
        entity.startedAt = session.getStartedAt();
        entity.expiresAt = session.getExpiresAt();
        entity.isActive = session.isActive();
        entity.createdBy = session.getCreatedBy();
        return entity;
    }
    
    private TableSession toSessionDomain(TableSessionEntity entity) {
        return new TableSession(
            entity.id,
            entity.tableId,
            new TenantId(entity.restaurantId),
            entity.sessionCode,
            entity.startedAt,
            entity.expiresAt,
            entity.isActive,
            entity.createdBy
        );
    }
}
