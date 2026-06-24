-- ============================================================
-- Rollback: Revert order_status CHECK constraint to original
-- Date: 2026-06-23
-- Rollback for: V2026_06_23__unify_order_status_constraint.sql
-- ============================================================

-- Step 1: Drop the unified CHECK constraint
ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS orders_order_status_check;

-- Step 2: Restore original CHECK constraint
ALTER TABLE orders
    ADD CONSTRAINT orders_order_status_check
    CHECK (order_status IN (
        'PENDING_PAYMENT',
        'PROCESSING',
        'COMPLETED',
        'CANCELLED',
        'RETURNED',
        'DELETED'
    ));

-- Step 3: Revert any data migrations
UPDATE orders
SET order_status = 'DELETED'
WHERE order_status = 'CANCELLED'
  AND deleted_at IS NOT NULL
  AND order_status IN (
      SELECT order_id FROM orders
      WHERE order_status = 'CANCELLED' AND deleted_at IS NOT NULL
  );

UPDATE orders
SET order_status = 'RETURNED'
WHERE order_status = 'COMPLETED'
  AND order_id IN (
      SELECT order_id FROM order_histories
      WHERE order_history_note LIKE '%RETURNED%'
  );
