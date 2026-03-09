/* ======================================================
   ENSURE VENDOR SUBSCRIPTION CORE COLUMNS
   Keeps public.vendors aligned with Vendor entity mapping
   ====================================================== */

ALTER TABLE public.vendors
    ADD COLUMN IF NOT EXISTS subscription_status VARCHAR(32),
    ADD COLUMN IF NOT EXISTS subscription_started_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS subscription_expires_at TIMESTAMP WITH TIME ZONE;

UPDATE public.vendors
SET subscription_status = 'TRIAL'
WHERE subscription_status IS NULL;

CREATE INDEX IF NOT EXISTS idx_vendors_subscription_status
    ON public.vendors (subscription_status);

CREATE INDEX IF NOT EXISTS idx_vendors_subscription_started_at
    ON public.vendors (subscription_started_at);

CREATE INDEX IF NOT EXISTS idx_vendors_subscription_expires_at
    ON public.vendors (subscription_expires_at);
