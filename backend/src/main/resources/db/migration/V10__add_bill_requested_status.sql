-- BILL_REQUESTED status support
-- Note: V6 converted order status from PostgreSQL ENUM to VARCHAR(20)
-- The Java OrderStatus enum includes BILL_REQUESTED, which will work
-- with the VARCHAR column without any database changes needed.
-- This migration is kept for versioning consistency.
SELECT 1;
