ALTER TABLE password_reset_token
DROP COLUMN token;

ALTER TABLE password_reset_token
ADD COLUMN token_hash VARCHAR(255) NOT NULL UNIQUE;

CREATE INDEX idx_password_reset_token_hash
ON password_reset_token(token_hash);