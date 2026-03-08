/* ======================================================
   USERS - CREDENTIALS UPDATED TIMESTAMP
   Tracks the last time user credentials were changed
   ====================================================== */

ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS credentials_updated_at TIMESTAMP WITH TIME ZONE;