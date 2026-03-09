/* ======================================================
   USERS TEMPLATE - CREDENTIALS UPDATED TIMESTAMP
   Tracks the last time user credentials were changed
   ====================================================== */

ALTER TABLE public.template_users
    ADD COLUMN IF NOT EXISTS credentials_updated_at TIMESTAMPTZ;