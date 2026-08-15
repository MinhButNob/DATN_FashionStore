ALTER TABLE shipping_addresses ADD COLUMN IF NOT EXISTS shipping_address_province_code VARCHAR(20);
ALTER TABLE shipping_addresses ADD COLUMN IF NOT EXISTS shipping_address_province_name VARCHAR(150);
ALTER TABLE shipping_addresses ADD COLUMN IF NOT EXISTS shipping_address_district_code VARCHAR(20);
ALTER TABLE shipping_addresses ADD COLUMN IF NOT EXISTS shipping_address_district_name VARCHAR(150);
ALTER TABLE shipping_addresses ADD COLUMN IF NOT EXISTS shipping_address_ward_code VARCHAR(20);
ALTER TABLE shipping_addresses ADD COLUMN IF NOT EXISTS shipping_address_ward_name VARCHAR(150);
ALTER TABLE shipping_addresses ADD COLUMN IF NOT EXISTS shipping_address_detail TEXT;
ALTER TABLE shipping_addresses ADD COLUMN IF NOT EXISTS shipping_address_needs_update BOOLEAN DEFAULT TRUE;
UPDATE shipping_addresses
SET shipping_address_detail = COALESCE(shipping_address_detail, shipping_address_street),
    shipping_address_needs_update = TRUE
WHERE shipping_address_needs_update IS NULL;

ALTER TABLE shipping_addresses ALTER COLUMN shipping_address_needs_update SET DEFAULT TRUE;
ALTER TABLE shipping_addresses ALTER COLUMN shipping_address_needs_update SET NOT NULL;

UPDATE shipping_addresses
SET shipping_address_province_name = COALESCE(NULLIF(shipping_address_province_name, ''), shipping_address_city),
    shipping_address_district_name = COALESCE(NULLIF(shipping_address_district_name, ''), shipping_address_district),
    shipping_address_ward_name = COALESCE(NULLIF(shipping_address_ward_name, ''), shipping_address_ward),
    shipping_address_detail = COALESCE(NULLIF(shipping_address_detail, ''), shipping_address_street)
WHERE shipping_address_province_code IS NULL
   OR shipping_address_district_code IS NULL
   OR shipping_address_ward_code IS NULL;
