/* ======================================================
   ENSURE VENDOR ENTITY CORE COLUMNS
   Align public.vendors with Vendor JPA mapping
   ====================================================== */

ALTER TABLE public.vendors
    ADD COLUMN IF NOT EXISTS nome_vendedor VARCHAR(255),
    ADD COLUMN IF NOT EXISTS whatsapp_vendedor VARCHAR(255),
    ADD COLUMN IF NOT EXISTS nome_empresa VARCHAR(255),
    ADD COLUMN IF NOT EXISTS logo_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS cor_destaque VARCHAR(50),
    ADD COLUMN IF NOT EXISTS mensagem_boas_vindas TEXT,
    ADD COLUMN IF NOT EXISTS slug VARCHAR(255),
    ADD COLUMN IF NOT EXISTS user_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS email_invalid BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS schema_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS name VARCHAR(255);

-- Backfill when legacy column 'email' exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'vendors'
          AND column_name = 'email'
    ) THEN
        UPDATE public.vendors
        SET user_email = email
        WHERE user_email IS NULL
          AND email IS NOT NULL;
    END IF;
END$$;

CREATE INDEX IF NOT EXISTS idx_vendors_user_email
    ON public.vendors (user_email);

CREATE INDEX IF NOT EXISTS idx_vendors_slug
    ON public.vendors (slug);
