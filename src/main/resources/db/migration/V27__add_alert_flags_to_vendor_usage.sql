/* ======================================================
   VENDOR USAGE ALERT FLAGS
   Tracks whether usage alerts were already sent
   ====================================================== */

ALTER TABLE public.vendor_usage
    ADD COLUMN IF NOT EXISTS alert80_sent BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS alert100_sent BOOLEAN NOT NULL DEFAULT FALSE;