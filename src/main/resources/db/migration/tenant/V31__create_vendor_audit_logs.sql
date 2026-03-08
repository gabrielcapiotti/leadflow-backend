/* ======================================================
   VENDOR AUDIT LOGS
   Tracks actions performed within a vendor context
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.vendor_audit_logs (

    id UUID PRIMARY KEY,

    vendor_id UUID NOT NULL,

    user_email VARCHAR(255) NOT NULL,

    acao VARCHAR(100) NOT NULL,

    entity_type VARCHAR(100) NOT NULL,

    entidade_id UUID NOT NULL,

    detalhes TEXT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_vendor_audit_logs_vendor
        FOREIGN KEY (vendor_id)
        REFERENCES public.vendors(id)
        ON DELETE CASCADE
);


/* ======================================================
   INDEXES
   ====================================================== */

-- Lookup logs by vendor
CREATE INDEX IF NOT EXISTS idx_vendor_audit_logs_vendor
    ON public.vendor_audit_logs (vendor_id);

-- Filtering by entity type
CREATE INDEX IF NOT EXISTS idx_vendor_audit_logs_entity_type
    ON public.vendor_audit_logs (entity_type);

-- Lookup by entity id
CREATE INDEX IF NOT EXISTS idx_vendor_audit_logs_entity_id
    ON public.vendor_audit_logs (entidade_id);

-- Timeline queries
CREATE INDEX IF NOT EXISTS idx_vendor_audit_logs_created
    ON public.vendor_audit_logs (created_at DESC);

-- Lookup by actor
CREATE INDEX IF NOT EXISTS idx_vendor_audit_logs_user_email
    ON public.vendor_audit_logs (user_email);