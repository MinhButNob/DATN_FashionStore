-- ============================================================
-- Migration: Unify order_status CHECK constraint with Java enum
-- Date: 2026-06-23
-- Issue: Java OrderStatus enum has values (CONFIRMED, SHIPPING, DELIVERED)
--        that are missing from the SQL CHECK constraint, causing
--        "violates check constraint orders_order_status_check"
-- ============================================================

-- Step 1: Drop the old CHECK constraint
ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS orders_order_status_check;

-- Step 2: Add unified CHECK constraint that matches Java OrderStatus enum
-- Final values: PENDING_PAYMENT, CONFIRMED, PROCESSING, SHIPPING, DELIVERED, COMPLETED, CANCELLED, RETURNED
-- Note: DELETED is excluded because soft-delete uses deleted_at column per project convention
ALTER TABLE orders
    ADD CONSTRAINT orders_order_status_check
    CHECK (order_status IN (
        'PENDING_PAYMENT',
        'CONFIRMED',
        'PROCESSING',
        'SHIPPING',
        'DELIVERED',
        'COMPLETED',
        'CANCELLED',
        'RETURNED'
    ));

-- Step 3: Update default value to match Java default
ALTER TABLE orders
    ALTER COLUMN order_status SET DEFAULT 'PENDING_PAYMENT';

-- Step 4: Migrate existing data — map old non-matching statuses to closest equivalents
-- (Only needed if database has rows with RETURNED or DELETED status)
UPDATE orders
SET order_status = 'CANCELLED'
WHERE order_status = 'DELETED'
  AND deleted_at IS NOT NULL;

UPDATE orders
SET order_status = 'COMPLETED'
WHERE order_status = 'RETURNED';
