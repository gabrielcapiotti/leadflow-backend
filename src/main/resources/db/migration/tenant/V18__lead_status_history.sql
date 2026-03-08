/* ======================================================
   LEAD STATUS HISTORY TABLE
   Tracks the timeline of lead status changes
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.lead_status_history (

    id UUID PRIMARY KEY,

    lead_id UUID NOT NULL,

    status VARCHAR(30) NOT NULL,

    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    changed_by UUID,

    CONSTRAINT chk_lsh_status
        CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'CLOSED')),

    CONSTRAINT fk_lsh_lead
        FOREIGN KEY (lead_id)
        REFERENCES public.leads(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_lsh_user
        FOREIGN KEY (changed_by)
        REFERENCES public.users(id)
        ON DELETE SET NULL
);


/* ======================================================
   INDEXES
   ====================================================== */

-- Timeline queries for a lead
CREATE INDEX IF NOT EXISTS idx_lsh_lead_changed_at
    ON public.lead_status_history (lead_id, changed_at DESC);

-- Status filtering
CREATE INDEX IF NOT EXISTS idx_lsh_status
    ON public.lead_status_history (status);

-- Audit lookup by user
CREATE INDEX IF NOT EXISTS idx_lsh_changed_by
    ON public.lead_status_history (changed_by);