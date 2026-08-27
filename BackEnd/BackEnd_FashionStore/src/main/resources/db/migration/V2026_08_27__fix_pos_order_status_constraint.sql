-- POS VNPay creates an unpaid order before redirecting to VNPay.
-- Keep PENDING for backward compatibility, and accept the shared online
-- status PENDING_PAYMENT used by the service for new POS orders.

ALTER TABLE pos_orders
    DROP CONSTRAINT IF EXISTS chk_pos_order_status;

ALTER TABLE pos_orders
    DROP CONSTRAINT IF EXISTS pos_orders_pos_order_status_check;

ALTER TABLE pos_orders
    ADD CONSTRAINT chk_pos_order_status CHECK (pos_order_status IN (
        'PENDING', 'PENDING_PAYMENT', 'COMPLETED', 'CANCELLED', 'REFUNDED', 'DELETED'
    ));

-- VNPay needs a PENDING payment row before redirecting to the gateway.
ALTER TABLE pos_payments
    DROP CONSTRAINT IF EXISTS chk_pos_payment_status;

ALTER TABLE pos_payments
    DROP CONSTRAINT IF EXISTS pos_payments_pos_payment_status_check;

ALTER TABLE pos_payments
    ADD CONSTRAINT chk_pos_payment_status CHECK (pos_payment_status IN (
        'PENDING', 'COMPLETED', 'FAILED', 'REFUNDED', 'CANCELLED', 'DELETED'
    ));
