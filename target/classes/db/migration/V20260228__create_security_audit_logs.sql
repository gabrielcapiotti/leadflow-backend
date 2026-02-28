CREATE TABLE security_audit_logs (
    id UUID PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    email VARCHAR(150) NOT NULL,
    tenant VARCHAR(100) NOT NULL,
    success BOOLEAN NOT NULL,
    ip_address VARCHAR(100),
    user_agent VARCHAR(255),
    correlation_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_audit_email ON security_audit_logs(email);
CREATE INDEX idx_audit_tenant ON security_audit_logs(tenant);
CREATE INDEX idx_audit_created_at ON security_audit_logs(created_at);