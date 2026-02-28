ALTER TABLE refresh_tokens
ADD COLUMN device_fingerprint VARCHAR(255) NOT NULL;

CREATE INDEX idx_refresh_token_fingerprint
ON refresh_tokens(device_fingerprint);