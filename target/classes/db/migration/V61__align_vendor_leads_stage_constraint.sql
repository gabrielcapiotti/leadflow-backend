/* ======================================================
   ALIGN VENDOR_LEADS STAGE CONSTRAINT
   Supports current enum values used by application
   ====================================================== */

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'vendor_leads'
          AND constraint_name = 'chk_vendor_leads_stage'
          AND constraint_type = 'CHECK'
    ) THEN
        ALTER TABLE public.vendor_leads
            DROP CONSTRAINT chk_vendor_leads_stage;
    END IF;
END$$;

ALTER TABLE public.vendor_leads
    ADD CONSTRAINT chk_vendor_leads_stage
    CHECK (
        stage IN (
            'NOVO', 'CONTATO', 'PROPOSTA', 'FECHADO', 'PERDIDO',
            'NEW', 'CONTACT', 'PROPOSAL', 'CLOSED', 'LOST'
        )
    );
