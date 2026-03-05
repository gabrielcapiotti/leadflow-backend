ALTER TABLE vendors
    ADD COLUMN IF NOT EXISTS email_invalid BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS email_events (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reason TEXT
);

CREATE INDEX IF NOT EXISTS idx_email_events_email ON email_events (email);
CREATE INDEX IF NOT EXISTS idx_email_events_event_type ON email_events (event_type);
CREATE INDEX IF NOT EXISTS idx_email_events_occurred_at ON email_events (occurred_at);
