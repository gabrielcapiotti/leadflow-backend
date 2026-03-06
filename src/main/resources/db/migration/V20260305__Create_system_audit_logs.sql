CREATE TABLE system_audit_logs (
    id UUID PRIMARY KEY,
    action VARCHAR NOT NULL,
    entity VARCHAR NOT NULL,
    entity_id VARCHAR,
    details TEXT,
    tenant VARCHAR,
    performed_by VARCHAR,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_audit_entity
ON system_audit_logs(entity);