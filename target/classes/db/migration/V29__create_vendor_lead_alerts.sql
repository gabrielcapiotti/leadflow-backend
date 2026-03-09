/* ======================================================
   VENDOR LEAD ALERTS
   Stores alerts related to vendor leads
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.vendor_lead_alerts (

    id UUID PRIMARY KEY,

    vendor_lead_id UUID NOT NULL,

    tipo VARCHAR(50) NOT NULL,

    mensagem TEXT NOT NULL,

    resolvido BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_vendor_lead_alerts_lead
        FOREIGN KEY (vendor_lead_id)
        REFERENCES public.vendor_leads(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_vendor_lead_alert_type
        CHECK (tipo IN ('RISK', 'FOLLOW_UP', 'AI_ALERT', 'SYSTEM'))
);


/* ======================================================
   INDEXES
   ====================================================== */

-- Lookup alerts for a lead
CREATE INDEX IF NOT EXISTS idx_vendor_lead_alerts_lead
    ON public.vendor_lead_alerts (vendor_lead_id);

-- Filtering unresolved alerts
CREATE INDEX IF NOT EXISTS idx_vendor_lead_alerts_resolved
    ON public.vendor_lead_alerts (resolvido);

-- Timeline queries
CREATE INDEX IF NOT EXISTS idx_vendor_lead_alerts_created
    ON public.vendor_lead_alerts (created_at DESC);