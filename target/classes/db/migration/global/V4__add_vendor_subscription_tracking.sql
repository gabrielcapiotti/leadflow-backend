ALTER TABLE vendors
    ADD COLUMN IF NOT EXISTS subscription_expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS external_subscription_id VARCHAR(255);
