-- Product modifiers (extras, removals, customizations)
CREATE TABLE menu_item_modifiers (
    id UUID PRIMARY KEY,
    menu_item_id UUID NOT NULL REFERENCES menu_items(id) ON DELETE CASCADE,
    restaurant_id UUID NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    price_adjustment NUMERIC(10,2) NOT NULL DEFAULT 0,
    modifier_type VARCHAR(20) NOT NULL DEFAULT 'EXTRA',
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT check_modifier_type CHECK (modifier_type IN ('EXTRA', 'REMOVAL', 'SUBSTITUTION', 'SIZE'))
);

CREATE INDEX idx_modifiers_menu_item ON menu_item_modifiers(menu_item_id);
CREATE INDEX idx_modifiers_restaurant ON menu_item_modifiers(restaurant_id);

-- Order item modifiers (selected modifiers for each order item)
CREATE TABLE order_item_modifiers (
    id UUID PRIMARY KEY,
    order_item_id UUID NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    modifier_id UUID NOT NULL REFERENCES menu_item_modifiers(id),
    modifier_name VARCHAR(100) NOT NULL,
    price_adjustment NUMERIC(10,2) NOT NULL
);

CREATE INDEX idx_order_modifiers_item ON order_item_modifiers(order_item_id);

-- Update order_items to include modifier total
ALTER TABLE order_items ADD COLUMN modifiers_total NUMERIC(10,2) NOT NULL DEFAULT 0;
