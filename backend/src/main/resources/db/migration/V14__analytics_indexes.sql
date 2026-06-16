-- Analytics dashboard query indexes
CREATE INDEX IF NOT EXISTS idx_orders_tenant_submitted ON orders (restaurant_id, submitted_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_tenant_created ON orders (restaurant_id, created_at DESC);
