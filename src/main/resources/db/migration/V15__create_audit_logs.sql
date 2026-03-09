/* ======================================================
   AUDIT LOGS
   Stores generic audit events across the system
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.audit_logs (

    id UUID PRIMARY KEY,

    action VARCHAR(100) NOT NULL,

    actor_email VARCHAR(255),

    entity_type VARCHAR(100) DEFAULT 'UNKNOWN' NOT NULL,
    entity_id VARCHAR(100),

    details TEXT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);


/* ======================================================
   INDEXES
   ====================================================== */

CREATE INDEX IF NOT EXISTS idx_audit_logs_action
    ON public.audit_logs (action);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_email
    ON public.audit_logs (actor_email);

CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at
    ON public.audit_logs (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_logs_entity_type
    ON public.audit_logs (entity_type);