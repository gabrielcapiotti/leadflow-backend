CREATE TABLE vendor_audit_logs (
    id UUID PRIMARY KEY,
    vendor_id UUID NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    acao VARCHAR(100) NOT NULL,
    entidade_id UUID NOT NULL,
    detalhes TEXT,
    created_at TIMESTAMP NOT NULL
);
