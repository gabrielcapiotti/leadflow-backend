CREATE TABLE IF NOT EXISTS vendor_risk_alerts (
    id UUID PRIMARY KEY,
    vendor_id UUID NOT NULL,
    score INTEGER NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    resolved BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_vendor_risk_alerts_vendor_resolved
    ON vendor_risk_alerts (vendor_id, resolved);