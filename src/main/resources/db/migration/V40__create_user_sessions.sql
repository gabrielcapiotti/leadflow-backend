/* ======================================================
   TEMPLATE: USER SESSIONS
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.template_user_sessions (

    id UUID NOT NULL,

    user_id UUID NOT NULL,

    tenant_id UUID,

    token_id VARCHAR(255) NOT NULL,

    active BOOLEAN DEFAULT TRUE,

    suspicious BOOLEAN DEFAULT FALSE,

    ip_address VARCHAR(100),

    initial_ip_address VARCHAR(100),

    user_agent TEXT,

    initial_user_agent TEXT,

    last_access_at TIMESTAMPTZ,

    revoked_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ,

    CONSTRAINT pk_template_user_sessions PRIMARY KEY (id)
);

-- =========================
-- Indexes
-- =========================

CREATE INDEX IF NOT EXISTS idx_template_user_sessions_user
ON public.template_user_sessions(user_id);

CREATE INDEX IF NOT EXISTS idx_template_user_sessions_token
ON public.template_user_sessions(token_id);

CREATE INDEX IF NOT EXISTS idx_template_user_sessions_user_tenant_active
ON public.template_user_sessions(user_id, tenant_id, active);

CREATE INDEX IF NOT EXISTS idx_template_user_sessions_last_access
ON public.template_user_sessions(last_access_at);

CREATE INDEX IF NOT EXISTS idx_template_user_sessions_active
ON public.template_user_sessions(active);

CREATE INDEX IF NOT EXISTS idx_template_user_sessions_suspicious
ON public.template_user_sessions(suspicious);