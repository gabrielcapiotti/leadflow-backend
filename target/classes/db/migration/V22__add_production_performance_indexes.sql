/* ======================================================
   PERFORMANCE INDEXES
   Creates indexes safely if tables exist
   ====================================================== */

DO $$
BEGIN

    -- ==================================================
    -- VENDOR LEADS
    -- ==================================================
    IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema='public'
            AND table_name='vendor_leads'
            AND column_name='vendor_id'
        ) THEN
        CREATE INDEX IF NOT EXISTS idx_vendor_leads_vendor
            ON public.vendor_leads (vendor_id);
    END IF;

    IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema='public'
            AND table_name='vendor_leads'
            AND column_name='created_date'
        ) THEN
        CREATE INDEX IF NOT EXISTS idx_vendor_leads_created
            ON public.vendor_leads (created_date);
    END IF;


    -- ==================================================
    -- VENDOR USAGE
    -- ==================================================
    IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema='public'
            AND table_name='vendor_usage'
            AND column_name='vendor_id'
        ) THEN
        CREATE INDEX IF NOT EXISTS idx_vendor_usage_vendor
            ON public.vendor_usage (vendor_id);
    END IF;

    IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema='public'
            AND table_name='vendor_usage'
            AND column_name='quota_type'
        ) THEN
        CREATE INDEX IF NOT EXISTS idx_vendor_usage_quota
            ON public.vendor_usage (quota_type);
    END IF;

    IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema='public'
            AND table_name='vendor_usage'
            AND column_name='period_start'
        ) THEN
        CREATE INDEX IF NOT EXISTS idx_vendor_usage_period
            ON public.vendor_usage (period_start);
    END IF;


    -- ==================================================
    -- VENDORS
    -- ==================================================
    IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema='public'
            AND table_name='venders'
            AND column_name='subscription_status'
        ) THEN
        CREATE INDEX IF NOT EXISTS idx_vendors_subscription_status
            ON public.vendors (subscription_status);
    END IF;

    IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema='public'
            AND table_name='venders'
            AND column_name='user_email'
        ) THEN
        CREATE INDEX IF NOT EXISTS idx_vendors_user_email
            ON public.vendors (user_email);
    END IF;


    -- ==================================================
    -- VENDOR AUDIT LOGS
    -- ==================================================
    IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema='public'
            AND table_name='vendor_audit_logs'
            AND column_name='entidade_id'
        ) THEN
        CREATE INDEX IF NOT EXISTS idx_vendor_audit_entity
            ON public.vendor_audit_logs (entidade_id);
    END IF;

    IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema='public'
            AND table_name='vendor_audit_logs'
            AND column_name='user_email'
        ) THEN
        CREATE INDEX IF NOT EXISTS idx_vendor_audit_actor
            ON public.vendor_audit_logs (user_email);
    END IF;

    IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema='public'
            AND table_name='vendor_audit_logs'
            AND column_name='created_at'
        ) THEN
        CREATE INDEX IF NOT EXISTS idx_vendor_audit_created
            ON public.vendor_audit_logs (created_at);
    END IF;

END
$$ LANGUAGE plpgsql;