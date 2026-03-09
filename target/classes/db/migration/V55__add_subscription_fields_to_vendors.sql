/* ======================================================
   ADD SUBSCRIPTION FIELDS TO VENDORS
   ====================================================== */

-- ======================================================
-- 1. ADD SUBSCRIPTION PLAN FIELD
-- ======================================================

ALTER TABLE public.vendors
ADD COLUMN IF NOT EXISTS subscription_plan VARCHAR(50);

-- ======================================================
-- 2. ADD BILLING AMOUNT FIELD
-- ======================================================

ALTER TABLE public.vendors
ADD COLUMN IF NOT EXISTS billing_amount DECIMAL(10, 2);

-- ======================================================
-- 3. ADD NEXT BILLING DATE FIELD
-- ======================================================

ALTER TABLE public.vendors
ADD COLUMN IF NOT EXISTS next_billing_date TIMESTAMPTZ;

-- ======================================================
-- 4. ADD AUTO RENEWAL FLAG
-- ======================================================

ALTER TABLE public.vendors
ADD COLUMN IF NOT EXISTS auto_renewal BOOLEAN DEFAULT TRUE;

-- ======================================================
-- 5. ADD CANCELLATION REQUESTED AT
-- ======================================================

ALTER TABLE public.vendors
ADD COLUMN IF NOT EXISTS cancellation_requested_at TIMESTAMPTZ;

-- ======================================================
-- 6. ADD CANCELLATION REASON
-- ======================================================

ALTER TABLE public.vendors
ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(255);

-- ======================================================
-- 7. ADD PAYMENT METHOD ID (FOR STRIPE INTEGRATION)
-- ======================================================

ALTER TABLE public.vendors
ADD COLUMN IF NOT EXISTS payment_method_id VARCHAR(255);

-- ======================================================
-- 8. ADD STRIPE CUSTOMER ID
-- ======================================================

ALTER TABLE public.vendors
ADD COLUMN IF NOT EXISTS stripe_customer_id VARCHAR(255) UNIQUE;

-- ======================================================
-- 9. CREATE INDEXES FOR SUBSCRIPTION QUERIES
-- ======================================================

CREATE INDEX IF NOT EXISTS idx_vendors_subscription_plan 
  ON public.vendors (subscription_plan);

CREATE INDEX IF NOT EXISTS idx_vendors_auto_renewal 
  ON public.vendors (auto_renewal);

CREATE INDEX IF NOT EXISTS idx_vendors_next_billing_date 
  ON public.vendors (next_billing_date);

CREATE INDEX IF NOT EXISTS idx_vendors_stripe_customer_id 
  ON public.vendors (stripe_customer_id);

CREATE INDEX IF NOT EXISTS idx_vendors_subscription_expires_at 
  ON public.vendors (subscription_expires_at);
