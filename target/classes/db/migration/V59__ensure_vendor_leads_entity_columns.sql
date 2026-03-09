/* ======================================================
   ENSURE VENDOR_LEADS ENTITY COLUMNS
   Align public.vendor_leads with VendorLead JPA mapping
   ====================================================== */

ALTER TABLE public.vendor_leads
    ADD COLUMN IF NOT EXISTS created_date TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS status VARCHAR(50),
    ADD COLUMN IF NOT EXISTS owner_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS resumo_estrategico TEXT,
    ADD COLUMN IF NOT EXISTS score INTEGER;

-- Backfill from legacy columns when available
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'vendor_leads'
          AND column_name = 'created_at'
    ) THEN
        UPDATE public.vendor_leads
        SET created_date = created_at
        WHERE created_date IS NULL
          AND created_at IS NOT NULL;
    END IF;
END$$;

UPDATE public.vendor_leads
SET status = 'novo'
WHERE status IS NULL;

UPDATE public.vendor_leads
SET score = 0
WHERE score IS NULL;

ALTER TABLE public.vendor_leads
    ALTER COLUMN created_date SET DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_vendor_leads_created_date
    ON public.vendor_leads (created_date);
