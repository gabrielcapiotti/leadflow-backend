/* ======================================================
   PASSWORD RESET TOKEN SECURITY UPDATE
   Replaces plain token storage with hashed token
   ====================================================== */

-- Remove plain token column if it exists
ALTER TABLE public.template_password_reset_tokens
    DROP COLUMN IF EXISTS token;

-- Add hashed token column
ALTER TABLE public.template_password_reset_tokens
    ADD COLUMN IF NOT EXISTS token_hash VARCHAR(255);

-- Ensure column is NOT NULL
ALTER TABLE public.template_password_reset_tokens
    ALTER COLUMN token_hash SET NOT NULL;

-- Add uniqueness constraint
ALTER TABLE public.template_password_reset_tokens
    ADD CONSTRAINT uq_template_password_reset_token_hash
        UNIQUE (token_hash);

-- Index for fast lookup
CREATE INDEX IF NOT EXISTS idx_template_password_reset_token_hash
    ON public.template_password_reset_tokens (token_hash);