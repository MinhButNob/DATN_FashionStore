-- Chạy thủ công một lần trên database hiện tại nếu ứng dụng không khởi động
-- do Hibernate đã phát hiện NULL trước khi migration được áp dụng.
UPDATE shipping_addresses
SET shipping_address_needs_update = TRUE
WHERE shipping_address_needs_update IS NULL;

ALTER TABLE shipping_addresses
    ALTER COLUMN shipping_address_needs_update SET DEFAULT TRUE;

ALTER TABLE shipping_addresses
    ALTER COLUMN shipping_address_needs_update SET NOT NULL;
