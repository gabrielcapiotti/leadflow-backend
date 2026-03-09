/* ======================================================
   SECURITY AUDIT LOGS
   Tracks authentication and security-related actions
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.security_audit_logs (

    id UUID PRIMARY KEY,

    action VARCHAR(50) NOT NULL,

    email VARCHAR(150) NOT NULL,
    tenant VARCHAR(100) NOT NULL,

    success BOOLEAN NOT NULL,

    ip_address VARCHAR(100),
    user_agent VARCHAR(255),

    correlation_id VARCHAR(100),

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);


/* ======================================================
   INDEXES
   ====================================================== */

-- Lookup by email
CREATE INDEX IF NOT EXISTS idx_security_audit_email
    ON public.security_audit_logs (email);

-- Tenant filtering (multi-tenant analysis)
CREATE INDEX IF NOT EXISTS idx_security_audit_tenant
    ON public.security_audit_logs (tenant);

-- Timeline queries
CREATE INDEX IF NOT EXISTS idx_security_audit_created_at
    ON public.security_audit_logs (created_at DESC);

-- Correlation tracking (request tracing)
CREATE INDEX IF NOT EXISTS idx_security_audit_correlation
    ON public.security_audit_logs (correlation_id);