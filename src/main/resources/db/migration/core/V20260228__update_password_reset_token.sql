/* ======================================================
   PASSWORD RESET TOKEN SECURITY UPDATE
   Replaces plain token storage with hashed token
   ====================================================== */

-- Remove plain token column if it exists
ALTER TABLE public.password_reset_token
    DROP COLUMN IF EXISTS token;

-- Add hashed token column
ALTER TABLE public.password_reset_token
    ADD COLUMN IF NOT EXISTS token_hash VARCHAR(255);

-- Ensure column is NOT NULL only after creation
ALTER TABLE public.password_reset_token
    ALTER COLUMN token_hash SET NOT NULL;

-- Add uniqueness constraint
ALTER TABLE public.password_reset_token
    ADD CONSTRAINT uq_password_reset_token_hash
        UNIQUE (token_hash);

-- Index for fast lookup
CREATE INDEX IF NOT EXISTS idx_password_reset_token_hash
    ON public.password_reset_token (token_hash);