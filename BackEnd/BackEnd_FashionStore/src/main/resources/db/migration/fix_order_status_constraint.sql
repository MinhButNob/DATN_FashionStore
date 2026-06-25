-- ============================================================
-- Quick Fix: Unify order_status CHECK constraint
-- Run this SQL directly in PostgreSQL to fix the constraint
-- ============================================================

-- 1. Drop旧constraint
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_order_status_check;

-- 2. Add unified constraint (matches Java OrderStatus enum)
ALTER TABLE orders ADD CONSTRAINT orders_order_status_check
    CHECK (order_status IN (
        'PENDING_PAYMENT', 'CONFIRMED', 'PROCESSING',
        'SHIPPING', 'DELIVERED', 'COMPLETED', 'CANCELLED', 'RETURNED'
    ));

-- 3. Migrate data if needed (map DELETED → CANCELLED for soft-deleted orders)
UPDATE orders SET order_status = 'CANCELLED'
WHERE order_status = 'DELETED' AND deleted_at IS NOT NULL;

-- 4. Verify
SELECT conname, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conname = 'orders_order_status_check';
