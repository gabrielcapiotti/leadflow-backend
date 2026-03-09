/* ======================================================
   ADD MISSING VENDOR BILLING COLUMNS
   Compatibility migration for Vendor entity fields
   ====================================================== */

-- Columns used directly by com.leadflow.backend.entities.vendor.Vendor
ALTER TABLE public.vendors
    ADD COLUMN IF NOT EXISTS external_customer_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS external_subscription_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS next_billing_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS last_payment_at TIMESTAMP WITH TIME ZONE;

-- Backfill from recently introduced aliases when present
UPDATE public.vendors
SET external_customer_id = stripe_customer_id
WHERE external_customer_id IS NULL
  AND stripe_customer_id IS NOT NULL;

UPDATE public.vendors
SET next_billing_at = next_billing_date
WHERE next_billing_at IS NULL
  AND next_billing_date IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_vendors_external_customer_id
    ON public.vendors (external_customer_id);

CREATE INDEX IF NOT EXISTS idx_vendors_external_subscription_id
    ON public.vendors (external_subscription_id);

CREATE INDEX IF NOT EXISTS idx_vendors_next_billing_at
    ON public.vendors (next_billing_at);
