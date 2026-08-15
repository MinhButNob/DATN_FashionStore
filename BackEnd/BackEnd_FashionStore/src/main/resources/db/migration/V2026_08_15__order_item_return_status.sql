ALTER TABLE order_details ADD COLUMN IF NOT EXISTS order_detail_status VARCHAR(30);
UPDATE order_details SET order_detail_status = 'COMPLETED' WHERE order_detail_status IS NULL;
ALTER TABLE order_details ALTER COLUMN order_detail_status SET DEFAULT 'COMPLETED';
ALTER TABLE order_details ALTER COLUMN order_detail_status SET NOT NULL;

ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_order_status_check;
ALTER TABLE orders ADD CONSTRAINT orders_order_status_check CHECK (order_status IN (
    'PENDING_PAYMENT', 'CONFIRMED', 'PROCESSING', 'SHIPPING', 'DELIVERED',
    'COMPLETED', 'CANCELLED', 'RETURN_REQUESTED', 'RETURN_APPROVED',
    'RETURN_RECEIVED', 'RETURN_PENDING', 'RETURNED', 'PARTIALLY_RETURNED', 'REJECTED'
));
