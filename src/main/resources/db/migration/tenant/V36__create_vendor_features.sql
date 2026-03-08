/* ======================================================
   VENDOR FEATURES
   Feature flags enabled per vendor
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.vendor_features (

    id UUID PRIMARY KEY,

    vendor_id UUID NOT NULL,

    feature_key VARCHAR(100) NOT NULL,

    enabled BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_vendor_features_vendor
        FOREIGN KEY (vendor_id)
        REFERENCES public.vendors(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_vendor_features_vendor_key
        UNIQUE (vendor_id, feature_key)
);


/* ======================================================
   INDEXES
   ====================================================== */

-- Lookup all features for a vendor
CREATE INDEX IF NOT EXISTS idx_vendor_features_vendor
    ON public.vendor_features (vendor_id);

-- Lookup enabled features for a vendor
CREATE INDEX IF NOT EXISTS idx_vendor_features_vendor_enabled
    ON public.vendor_features (vendor_id, enabled);

-- Lookup vendors using a specific feature
CREATE INDEX IF NOT EXISTS idx_vendor_features_feature_key
    ON public.vendor_features (feature_key);