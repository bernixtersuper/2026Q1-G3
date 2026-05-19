-- Add base_price column to order_items to track the original item price
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS base_price NUMERIC(10,2);

-- Set base_price equal to unit_price for existing records
UPDATE order_items SET base_price = unit_price WHERE base_price IS NULL;

-- Make base_price NOT NULL after setting default values
ALTER TABLE order_items ALTER COLUMN base_price SET NOT NULL;

-- Add modifier_type and created_at columns to order_item_modifiers if not present
ALTER TABLE order_item_modifiers ADD COLUMN IF NOT EXISTS modifier_type VARCHAR(20);
UPDATE order_item_modifiers SET modifier_type = 'EXTRA' WHERE modifier_type IS NULL;
ALTER TABLE order_item_modifiers ALTER COLUMN modifier_type SET NOT NULL;

ALTER TABLE order_item_modifiers ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ;
UPDATE order_item_modifiers SET created_at = NOW() WHERE created_at IS NULL;
ALTER TABLE order_item_modifiers ALTER COLUMN created_at SET NOT NULL;
