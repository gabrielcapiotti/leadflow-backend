-- Add subscription tracking fields to vendors

ALTER TABLE public.vendors
    ADD COLUMN IF NOT EXISTS subscription_expires_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS external_subscription_id VARCHAR(255);

-- Optional index to speed up subscription expiration checks
CREATE INDEX IF NOT EXISTS idx_vendors_subscription_expires_at
    ON public.vendors(subscription_expires_at);

-- Optional index for external subscription lookup (Stripe / billing systems)
CREATE INDEX IF NOT EXISTS idx_vendors_external_subscription_id
    ON public.vendors(external_subscription_id);