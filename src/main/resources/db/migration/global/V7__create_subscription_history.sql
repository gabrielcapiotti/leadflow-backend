CREATE TABLE IF NOT EXISTS subscription_history (
    id UUID PRIMARY KEY,
    vendor_id UUID NOT NULL,
    previous_status VARCHAR(32) NOT NULL,
    new_status VARCHAR(32) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(255),
    external_event_id VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_subscription_history_vendor_changed
    ON subscription_history (vendor_id, changed_at DESC);
