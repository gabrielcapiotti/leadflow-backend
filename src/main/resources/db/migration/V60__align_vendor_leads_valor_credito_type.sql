/* ======================================================
   ALIGN VENDOR_LEADS VALOR_CREDITO TYPE
   VendorLead entity maps valorCredito as String
   ====================================================== */

ALTER TABLE public.vendor_leads
    ADD COLUMN IF NOT EXISTS valor_credito VARCHAR(100);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'vendor_leads'
          AND column_name = 'valor_credito'
          AND data_type <> 'character varying'
    ) THEN
        ALTER TABLE public.vendor_leads
            ALTER COLUMN valor_credito TYPE VARCHAR(100)
            USING valor_credito::text;
    END IF;
END$$;
