-- Migration to create the login_audit table in the public schema

CREATE TABLE IF NOT EXISTS public.login_audit (
    id UUID PRIMARY KEY,
    user_id UUID,
    email VARCHAR(255),
    ip_address VARCHAR(100),
    user_agent TEXT,
    success BOOLEAN,
    created_at TIMESTAMP NOT NULL
);

-- Indexes for the login_audit table
CREATE INDEX IF NOT EXISTS idx_login_user ON public.login_audit (user_id);
CREATE INDEX IF NOT EXISTS idx_login_email ON public.login_audit (email);
CREATE INDEX IF NOT EXISTS idx_login_created ON public.login_audit (created_at);
CREATE INDEX IF NOT EXISTS idx_login_success ON public.login_audit (success);