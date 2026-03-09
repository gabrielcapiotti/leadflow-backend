/* ======================================================
   NORMALIZE LEAD STAGE VALUES
   Converts existing stage values to uppercase
   ====================================================== */

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
        AND table_name = 'vendor_leads'
        AND column_name = 'stage'
    ) THEN

        UPDATE public.vendor_leads
        SET stage = UPPER(stage)
        WHERE stage <> UPPER(stage);

    END IF;
END
$$ LANGUAGE plpgsql;