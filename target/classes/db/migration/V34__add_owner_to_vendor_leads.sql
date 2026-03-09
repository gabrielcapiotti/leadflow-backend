/* ======================================================
   VENDOR LEADS - OWNER EMAIL
   Assigns a responsible user/salesperson to the lead
   ====================================================== */

ALTER TABLE public.vendor_leads
    ADD COLUMN IF NOT EXISTS owner_email VARCHAR(255);