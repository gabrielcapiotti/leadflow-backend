ALTER TABLE refresh_tokens
ADD COLUMN IF NOT EXISTS device_fingerprint VARCHAR(255);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_refresh_token_fingerprint'
    ) THEN
        CREATE INDEX idx_refresh_token_fingerprint ON refresh_tokens (device_fingerprint);
    END IF;
END $$;