package com.menudigital.domain.table;

import com.menudigital.domain.tenant.TenantId;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class TableSession {
    
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration DEFAULT_SESSION_DURATION = Duration.ofHours(4);
    
    private UUID id;
    private UUID tableId;
    private TenantId tenantId;
    private String sessionCode;
    private Instant startedAt;
    private Instant expiresAt;
    private boolean active;
    private String createdBy;
    
    public TableSession() {
    }
    
    public TableSession(UUID id, UUID tableId, TenantId tenantId, String sessionCode,
                        Instant startedAt, Instant expiresAt, boolean active, String createdBy) {
        this.id = id;
        this.tableId = tableId;
        this.tenantId = tenantId;
        this.sessionCode = sessionCode;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
        this.active = active;
        this.createdBy = createdBy;
    }
    
    public static TableSession create(UUID tableId, TenantId tenantId, String createdBy) {
        Instant now = Instant.now();
        return new TableSession(
            UUID.randomUUID(),
            tableId,
            tenantId,
            generateSessionCode(),
            now,
            now.plus(DEFAULT_SESSION_DURATION),
            true,
            createdBy
        );
    }
    
    private static String generateSessionCode() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }
    
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
    
    public boolean isValid() {
        return active && !isExpired();
    }
    
    public void deactivate() {
        this.active = false;
    }
    
    public void extend(Duration duration) {
        this.expiresAt = Instant.now().plus(duration);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTableId() { return tableId; }
    public void setTableId(UUID tableId) { this.tableId = tableId; }
    public TenantId getTenantId() { return tenantId; }
    public void setTenantId(TenantId tenantId) { this.tenantId = tenantId; }
    public String getSessionCode() { return sessionCode; }
    public void setSessionCode(String sessionCode) { this.sessionCode = sessionCode; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
