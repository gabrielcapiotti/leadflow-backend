/* ======================================================
   VENDOR RISK ALERTS
   Tracks fraud or risk alerts for vendors
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.vendor_risk_alerts (

    id UUID PRIMARY KEY,

    vendor_id UUID NOT NULL,

    score INTEGER NOT NULL,

    risk_level VARCHAR(20) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    resolved BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_vendor_risk_alerts_vendor
        FOREIGN KEY (vendor_id)
        REFERENCES public.vendors(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_vendor_risk_level
        CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);


/* ======================================================
   INDEXES
   ====================================================== */

-- Lookup unresolved alerts for a vendor
CREATE INDEX IF NOT EXISTS idx_vendor_risk_alerts_vendor_resolved
    ON public.vendor_risk_alerts (vendor_id, resolved);

-- Timeline queries
CREATE INDEX IF NOT EXISTS idx_vendor_risk_alerts_created
    ON public.vendor_risk_alerts (created_at DESC);

-- Filtering by risk level
CREATE INDEX IF NOT EXISTS idx_vendor_risk_alerts_risk_level
    ON public.vendor_risk_alerts (risk_level);