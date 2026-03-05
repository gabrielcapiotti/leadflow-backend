CREATE TABLE IF NOT EXISTS payment_events (
    id UUID PRIMARY KEY,
    provider_event_id VARCHAR(255) NOT NULL,
    provider VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_payment_event_provider_id UNIQUE (provider_event_id)
);

CREATE INDEX IF NOT EXISTS idx_payment_events_processed_at ON payment_events (processed_at);
