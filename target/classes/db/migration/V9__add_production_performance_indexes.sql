/* ======================================================
   PERFORMANCE INDEXES (SAFE CREATION)
   Creates indexes only if target tables AND columns exist
   ====================================================== */

DO $$
BEGIN

    -- ==================================================
    -- VENDOR LEADS
    -- ==================================================
    IF to_regclass('public.vendor_leads') IS NOT NULL THEN

        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'vendor_leads'
            AND column_name = 'vendor_id'
        ) THEN
            CREATE INDEX IF NOT EXISTS idx_vendor_leads_vendor
                ON public.vendor_leads (vendor_id);
        END IF;

        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'vendor_leads'
            AND column_name = 'created_date'
        ) THEN
            CREATE INDEX IF NOT EXISTS idx_vendor_leads_created
                ON public.vendor_leads (created_date);
        END IF;

    END IF;


    -- ==================================================
    -- VENDOR USAGE
    -- ==================================================
    IF to_regclass('public.vendor_usage') IS NOT NULL THEN

        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'vendor_usage'
            AND column_name = 'vendor_id'
        ) THEN
            CREATE INDEX IF NOT EXISTS idx_vendor_usage_vendor
                ON public.vendor_usage (vendor_id);
        END IF;

        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'vendor_usage'
            AND column_name = 'quota_type'
        ) THEN
            CREATE INDEX IF NOT EXISTS idx_vendor_usage_quota
                ON public.vendor_usage (quota_type);
        END IF;

        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'vendor_usage'
            AND column_name = 'period_start'
        ) THEN
            CREATE INDEX IF NOT EXISTS idx_vendor_usage_period
                ON public.vendor_usage (period_start);
        END IF;

    END IF;


    -- ==================================================
    -- VENDORS
    -- ==================================================
    IF to_regclass('public.vendors') IS NOT NULL THEN

        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'vendors'
            AND column_name = 'subscription_status'
        ) THEN
            CREATE INDEX IF NOT EXISTS idx_vendors_subscription_status
                ON public.vendors (subscription_status);
        END IF;

        -- Corrigido: só cria índice se coluna existir
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'vendors'
            AND column_name = 'user_email'
        ) THEN
            CREATE INDEX IF NOT EXISTS idx_vendors_user_email
                ON public.vendors (user_email);
        END IF;

    END IF;


    -- ==================================================
    -- VENDOR AUDIT LOGS
    -- ==================================================
    IF to_regclass('public.vendor_audit_logs') IS NOT NULL THEN

        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'vendor_audit_logs'
            AND column_name = 'entidade_id'
        ) THEN
            CREATE INDEX IF NOT EXISTS idx_vendor_audit_entity
                ON public.vendor_audit_logs (entidade_id);
        END IF;

        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'vendor_audit_logs'
            AND column_name = 'user_email'
        ) THEN
            CREATE INDEX IF NOT EXISTS idx_vendor_audit_actor
                ON public.vendor_audit_logs (user_email);
        END IF;

        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'vendor_audit_logs'
            AND column_name = 'created_at'
        ) THEN
            CREATE INDEX IF NOT EXISTS idx_vendor_audit_created
                ON public.vendor_audit_logs (created_at);
        END IF;

    END IF;

END
$$ LANGUAGE plpgsql;