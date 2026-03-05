DO $$
BEGIN
    IF to_regclass('public.vendor_leads') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_vendor_lead_vendor
            ON public.vendor_leads (vendor_id);
        CREATE INDEX IF NOT EXISTS idx_vendor_lead_created
            ON public.vendor_leads (created_date);
    END IF;

    IF to_regclass('public.vendor_usage') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_usage_vendor
            ON public.vendor_usage (vendor_id);
        CREATE INDEX IF NOT EXISTS idx_usage_quota
            ON public.vendor_usage (quota_type);
        CREATE INDEX IF NOT EXISTS idx_usage_period
            ON public.vendor_usage (period_start);
    END IF;

    IF to_regclass('public.vendors') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_vendor_subscription
            ON public.vendors (subscription_status);
        CREATE INDEX IF NOT EXISTS idx_vendor_user_email
            ON public.vendors (user_email);
    END IF;

    IF to_regclass('public.vendor_audit_logs') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_audit_entity
            ON public.vendor_audit_logs (entidade_id);
        CREATE INDEX IF NOT EXISTS idx_audit_actor
            ON public.vendor_audit_logs (user_email);
        CREATE INDEX IF NOT EXISTS idx_audit_created
            ON public.vendor_audit_logs (created_at);
    END IF;
END $$;
