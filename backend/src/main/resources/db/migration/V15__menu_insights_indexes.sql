-- Indexes supporting owner menu-insights queries (frequently bought together,
-- menu engineering, top modifiers). The bought-together self-join matches order_items
-- on order_id and compares menu_item_id; this composite index covers both sides.
CREATE INDEX IF NOT EXISTS idx_order_items_order_menu_item ON order_items (order_id, menu_item_id);
CREATE INDEX IF NOT EXISTS idx_order_items_menu_item ON order_items (menu_item_id);
