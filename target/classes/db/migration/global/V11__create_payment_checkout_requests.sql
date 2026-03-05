CREATE TABLE IF NOT EXISTS payment_checkout_requests (
    id UUID PRIMARY KEY,
    reference_id VARCHAR(255) NOT NULL,
    provider VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    nome_vendedor VARCHAR(120) NOT NULL,
    whatsapp_vendedor VARCHAR(20) NOT NULL,
    nome_empresa VARCHAR(120),
    slug VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_payment_checkout_reference UNIQUE (reference_id)
);

CREATE INDEX IF NOT EXISTS idx_payment_checkout_email_status
    ON payment_checkout_requests (email, status);
