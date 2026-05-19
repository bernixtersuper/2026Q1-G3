package com.menudigital.domain.table;

import com.menudigital.domain.tenant.TenantId;

import java.time.Instant;
import java.util.UUID;

public class RestaurantTable {
    
    private UUID id;
    private TenantId tenantId;
    private String tableNumber;
    private String tableName;
    private int capacity;
    private String qrCodeToken;
    private boolean active;
    private Instant createdAt;
    private Integer positionX;
    private Integer positionY;
    private Integer width;
    private Integer height;
    
    public RestaurantTable() {
    }
    
    public RestaurantTable(UUID id, TenantId tenantId, String tableNumber, String tableName, 
                           int capacity, String qrCodeToken, boolean active, Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.tableNumber = tableNumber;
        this.tableName = tableName;
        this.capacity = capacity;
        this.qrCodeToken = qrCodeToken;
        this.active = active;
        this.createdAt = createdAt;
        this.positionX = null;
        this.positionY = null;
        this.width = 100;
        this.height = 80;
    }
    
    public static RestaurantTable create(TenantId tenantId, String tableNumber, String tableName, int capacity) {
        return new RestaurantTable(
            UUID.randomUUID(),
            tenantId,
            tableNumber,
            tableName,
            capacity,
            generateQrToken(),
            true,
            Instant.now()
        );
    }
    
    private static String generateQrToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    public void regenerateQrToken() {
        this.qrCodeToken = generateQrToken();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public TenantId getTenantId() { return tenantId; }
    public void setTenantId(TenantId tenantId) { this.tenantId = tenantId; }
    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String tableNumber) { this.tableNumber = tableNumber; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public String getQrCodeToken() { return qrCodeToken; }
    public void setQrCodeToken(String qrCodeToken) { this.qrCodeToken = qrCodeToken; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Integer getPositionX() { return positionX; }
    public void setPositionX(Integer positionX) { this.positionX = positionX; }
    public Integer getPositionY() { return positionY; }
    public void setPositionY(Integer positionY) { this.positionY = positionY; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
}
