/* ======================================================
   ADD NEW COLUMNS
   ====================================================== */

ALTER TABLE public.template_leads
ADD COLUMN IF NOT EXISTS status VARCHAR(30);

ALTER TABLE public.template_leads
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;


/* ======================================================
   DEFAULT VALUE
   ====================================================== */

ALTER TABLE public.template_leads
ALTER COLUMN status SET DEFAULT 'NEW';


/* ======================================================
   STATUS CONSTRAINT
   ====================================================== */

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_template_lead_status'
    ) THEN
        ALTER TABLE public.template_leads
        ADD CONSTRAINT chk_template_lead_status
        CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'CLOSED'));
    END IF;
END $$;


/* ======================================================
   INDEXES
   ====================================================== */

CREATE INDEX IF NOT EXISTS idx_template_leads_status
    ON public.template_leads (status);

CREATE INDEX IF NOT EXISTS idx_template_leads_deleted_at
    ON public.template_leads (deleted_at);