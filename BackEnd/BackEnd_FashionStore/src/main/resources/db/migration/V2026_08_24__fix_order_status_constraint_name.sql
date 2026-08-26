-- Keep the database constraint in sync with OrderStatus.
--
-- Some databases were created with chk_order_status while the previous
-- migrations only replaced orders_order_status_check. In that situation
-- PARTIALLY_RETURNED is valid in Java but rejected by PostgreSQL.

ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS chk_order_status;

ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS orders_order_status_check;

ALTER TABLE orders
    ADD CONSTRAINT chk_order_status CHECK (order_status IN (
        'PENDING_PAYMENT', 'CONFIRMED', 'PROCESSING', 'SHIPPING', 'DELIVERED',
        'COMPLETED', 'CANCELLED', 'RETURN_REQUESTED', 'RETURN_APPROVED',
        'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED',
        'REJECTED'
    ));
