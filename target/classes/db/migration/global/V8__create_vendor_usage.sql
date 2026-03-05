CREATE TABLE IF NOT EXISTS vendor_usage (
    id UUID PRIMARY KEY,
    vendor_id UUID NOT NULL,
    quota_type VARCHAR(50) NOT NULL,
    used INTEGER NOT NULL DEFAULT 0,
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_vendor_usage_vendor_quota
    ON vendor_usage (vendor_id, quota_type);
