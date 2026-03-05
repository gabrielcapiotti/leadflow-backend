CREATE TABLE IF NOT EXISTS vendor_features (
    id UUID PRIMARY KEY,
    vendor_id UUID NOT NULL,
    feature_key VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_vendor_features_vendor
        FOREIGN KEY (vendor_id) REFERENCES vendors(id) ON DELETE CASCADE,
    CONSTRAINT uk_vendor_features_vendor_key
        UNIQUE (vendor_id, feature_key)
);

CREATE INDEX IF NOT EXISTS idx_vendor_features_vendor
    ON vendor_features (vendor_id);

CREATE INDEX IF NOT EXISTS idx_vendor_features_vendor_enabled
    ON vendor_features (vendor_id, enabled);
