-- Make historical orders consistent with the free-shipping policy.
UPDATE orders
SET order_shipping_fee = 0
WHERE order_shipping_fee IS DISTINCT FROM 0;

ALTER TABLE orders
    ALTER COLUMN order_shipping_fee SET DEFAULT 0;
