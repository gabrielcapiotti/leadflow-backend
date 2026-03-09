/* ======================================================
   VENDOR LEAD STAGE HISTORY
   Tracks stage transitions of vendor leads
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.vendor_lead_stage_history (

    id UUID PRIMARY KEY,

    vendor_lead_id UUID NOT NULL,

    previous_stage VARCHAR(100) NOT NULL,
    new_stage VARCHAR(100) NOT NULL,

    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_vendor_lead_stage_history_lead
        FOREIGN KEY (vendor_lead_id)
        REFERENCES public.vendor_leads(id)
        ON DELETE CASCADE
);


/* ======================================================
   INDEXES
   ====================================================== */

-- Timeline queries per lead
CREATE INDEX IF NOT EXISTS idx_vendor_lead_stage_history_lead_changed
    ON public.vendor_lead_stage_history (vendor_lead_id, changed_at DESC);

-- Filtering by stage
CREATE INDEX IF NOT EXISTS idx_vendor_lead_stage_history_stage
    ON public.vendor_lead_stage_history (new_stage);