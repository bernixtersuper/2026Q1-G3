-- Add position fields for floor plan layout
ALTER TABLE restaurant_tables ADD COLUMN IF NOT EXISTS position_x INTEGER;
ALTER TABLE restaurant_tables ADD COLUMN IF NOT EXISTS position_y INTEGER;
ALTER TABLE restaurant_tables ADD COLUMN IF NOT EXISTS width INTEGER DEFAULT 100;
ALTER TABLE restaurant_tables ADD COLUMN IF NOT EXISTS height INTEGER DEFAULT 80;
