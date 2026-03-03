CREATE TABLE login_audit (
    id UUID PRIMARY KEY,
    user_id UUID,
    email VARCHAR(255),
    ip_address VARCHAR(100),
    user_agent TEXT,
    success BOOLEAN,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_login_user ON login_audit (user_id);
CREATE INDEX idx_login_email ON login_audit (email);
CREATE INDEX idx_login_created ON login_audit (created_at);
CREATE INDEX idx_login_success ON login_audit (success);