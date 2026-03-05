CREATE TABLE vendor_lead_alerts (
    id UUID PRIMARY KEY,
    vendor_lead_id UUID NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    mensagem TEXT NOT NULL,
    resolvido BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL
);