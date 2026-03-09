/* ======================================================
   USER SESSIONS TEMPLATE - MULTI TENANT + DEVICE CONTROL
   ====================================================== */

-- ======================================================
-- 1. ADD TENANT COLUMN
-- ======================================================

ALTER TABLE public.template_user_sessions
ADD COLUMN IF NOT EXISTS tenant_id UUID;

-- ======================================================
-- 2. ADD ACTIVE COLUMN
-- ======================================================

ALTER TABLE public.template_user_sessions
ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;

-- ======================================================
-- 3. INDEXES
-- ======================================================

CREATE INDEX IF NOT EXISTS idx_template_user_sessions_user_tenant_active
ON public.template_user_sessions (user_id, tenant_id, active);

CREATE INDEX IF NOT EXISTS idx_template_user_sessions_token
ON public.template_user_sessions (token_id);