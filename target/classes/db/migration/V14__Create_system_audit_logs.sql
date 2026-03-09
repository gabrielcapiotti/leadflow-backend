/* ======================================================
   SYSTEM AUDIT LOGS
   Stores global system audit events
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.system_audit_logs (

    id UUID PRIMARY KEY,

    action VARCHAR(100) NOT NULL,

    entity VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100),

    details TEXT,

    tenant VARCHAR(100),

    performed_by VARCHAR(150),

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);


/* ======================================================
   INDEXES
   ====================================================== */

-- Filtering by entity type
CREATE INDEX IF NOT EXISTS idx_system_audit_entity
    ON public.system_audit_logs (entity);

-- Timeline queries
CREATE INDEX IF NOT EXISTS idx_system_audit_created_at
    ON public.system_audit_logs (created_at DESC);

-- Tenant filtering (multi-tenant environments)
CREATE INDEX IF NOT EXISTS idx_system_audit_tenant
    ON public.system_audit_logs (tenant);