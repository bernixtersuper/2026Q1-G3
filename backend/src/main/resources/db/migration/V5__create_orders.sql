-- Orders system
CREATE TYPE order_status AS ENUM (
    'DRAFT',
    'SUBMITTED', 
    'CONFIRMED',
    'PREPARING',
    'READY',
    'DELIVERED',
    'CANCELLED'
);

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    table_id UUID NOT NULL REFERENCES restaurant_tables(id) ON DELETE CASCADE,
    session_id UUID REFERENCES table_sessions(id) ON DELETE SET NULL,
    order_number SERIAL,
    status order_status NOT NULL DEFAULT 'DRAFT',
    notes TEXT,
    subtotal NUMERIC(10,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    submitted_at TIMESTAMPTZ,
    confirmed_at TIMESTAMPTZ
);

CREATE INDEX idx_orders_restaurant ON orders(restaurant_id);
CREATE INDEX idx_orders_table ON orders(table_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_session ON orders(session_id);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id UUID NOT NULL REFERENCES menu_items(id) ON DELETE CASCADE,
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price NUMERIC(10,2) NOT NULL,
    notes TEXT,
    added_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_items_order ON order_items(order_id);

-- Function to update order subtotal
CREATE OR REPLACE FUNCTION update_order_subtotal()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE orders 
    SET subtotal = (
        SELECT COALESCE(SUM(quantity * unit_price), 0) 
        FROM order_items 
        WHERE order_id = COALESCE(NEW.order_id, OLD.order_id)
    ),
    updated_at = NOW()
    WHERE id = COALESCE(NEW.order_id, OLD.order_id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_order_subtotal
AFTER INSERT OR UPDATE OR DELETE ON order_items
FOR EACH ROW EXECUTE FUNCTION update_order_subtotal();

-- Daily order number sequence per restaurant
CREATE TABLE order_sequences (
    restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    last_number INT NOT NULL DEFAULT 0,
    PRIMARY KEY (restaurant_id, date)
);
