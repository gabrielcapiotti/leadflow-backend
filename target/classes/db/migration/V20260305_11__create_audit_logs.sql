CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    action VARCHAR(100) NOT NULL,
    actor_email VARCHAR(255),
    entity_type VARCHAR(100),
    entity_id VARCHAR(100),
    details TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_actor_email ON audit_logs(actor_email);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
