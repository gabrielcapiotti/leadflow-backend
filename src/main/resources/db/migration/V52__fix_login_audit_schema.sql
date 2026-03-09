-- Add missing columns to login_audit table
ALTER TABLE public.login_audit
ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(255);

ALTER TABLE public.login_audit
ADD COLUMN IF NOT EXISTS suspicious BOOLEAN DEFAULT FALSE;

ALTER TABLE public.login_audit
ADD COLUMN IF NOT EXISTS tenant_id UUID;

ALTER TABLE public.login_audit
ADD COLUMN IF NOT EXISTS user_agent TEXT;

-- Ensure table consistency
CREATE TABLE IF NOT EXISTS public.login_audit (
    id UUID PRIMARY KEY,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    email VARCHAR(255),

    failure_reason VARCHAR(255),

    ip_address VARCHAR(100),

    success BOOLEAN NOT NULL,

    suspicious BOOLEAN DEFAULT FALSE,

    tenant_id UUID,

    user_agent TEXT,

    user_id UUID
);

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_login_audit_user
ON public.login_audit(user_id);

CREATE INDEX IF NOT EXISTS idx_login_audit_tenant
ON public.login_audit(tenant_id);

CREATE INDEX IF NOT EXISTS idx_login_audit_created
ON public.login_audit(created_at);