/* ======================================================
   TEMPLATE: LEAD STATUS HISTORY
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.template_lead_status_history (

    id UUID NOT NULL,

    lead_id UUID NOT NULL,

    status VARCHAR(30) NOT NULL,

    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    changed_by UUID,

    CONSTRAINT pk_template_lsh PRIMARY KEY (id),

    CONSTRAINT chk_template_lsh_status
        CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'CLOSED'))
);


/* ======================================================
   INDEXES
   ====================================================== */

CREATE INDEX IF NOT EXISTS idx_template_lsh_lead_changed_at
    ON public.template_lead_status_history (lead_id, changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_template_lsh_status
    ON public.template_lead_status_history (status);

CREATE INDEX IF NOT EXISTS idx_template_lsh_changed_by
    ON public.template_lead_status_history (changed_by);