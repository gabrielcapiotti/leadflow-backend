/* ======================================================
   REFRESH TOKEN DEVICE FINGERPRINT
   Adds device fingerprint for session/device tracking
   ====================================================== */

DO $$
BEGIN

IF EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = 'public'
    AND table_name = 'refresh_tokens'
) THEN

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema='public'
        AND table_name='refresh_tokens'
        AND column_name='device_fingerprint'
    ) THEN

        ALTER TABLE public.refresh_tokens
        ADD COLUMN device_fingerprint TEXT;

    END IF;

END IF;

END
$$;