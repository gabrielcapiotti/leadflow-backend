/* ======================================================
   VENDOR USAGE
   Tracks usage quotas for vendors (API calls, leads, etc.)
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.vendor_usage (
    id UUID PRIMARY KEY,

    vendor_id UUID NOT NULL,

    quota_type VARCHAR(50) NOT NULL,
    used INTEGER NOT NULL DEFAULT 0,

    period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_vendor_usage_vendor
        FOREIGN KEY (vendor_id)
        REFERENCES public.vendors(id)
        ON DELETE CASCADE
);

-- Ensures one usage record per vendor and quota type
CREATE UNIQUE INDEX IF NOT EXISTS uq_vendor_usage_vendor_quota
    ON public.vendor_usage (vendor_id, quota_type);

-- Useful for billing / monthly resets
CREATE INDEX IF NOT EXISTS idx_vendor_usage_period
    ON public.vendor_usage (period_start, period_end);

-- Useful for vendor usage lookups
CREATE INDEX IF NOT EXISTS idx_vendor_usage_vendor
    ON public.vendor_usage (vendor_id);