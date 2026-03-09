/* ======================================================
   VENDOR LEADS - AI SCORING FIELDS
   Adds lead scoring and strategic summary
   ====================================================== */

ALTER TABLE public.vendor_leads
    ADD COLUMN IF NOT EXISTS score INTEGER NOT NULL DEFAULT 0;

ALTER TABLE public.vendor_leads
    ADD COLUMN IF NOT EXISTS resumo_estrategico TEXT;