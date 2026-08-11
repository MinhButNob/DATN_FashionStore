ALTER TABLE return_requests
    ADD COLUMN IF NOT EXISTS inspection_result TEXT,
    ADD COLUMN IF NOT EXISTS inspection_note TEXT,
    ADD COLUMN IF NOT EXISTS inspected_by INTEGER,
    ADD COLUMN IF NOT EXISTS inspected_at TIMESTAMP;
