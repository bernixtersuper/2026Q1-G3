-- Fix order_status to use VARCHAR instead of PostgreSQL ENUM for Hibernate compatibility
-- First, remove the default, change the type, then add a new default
ALTER TABLE orders ALTER COLUMN status DROP DEFAULT;
ALTER TABLE orders ALTER COLUMN status TYPE VARCHAR(20) USING status::text;
ALTER TABLE orders ALTER COLUMN status SET DEFAULT 'DRAFT';

-- Drop the enum type (no longer needed)
DROP TYPE IF EXISTS order_status CASCADE;
