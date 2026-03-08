/* ======================================================
   EMAIL EVENTS - RETRY AND PROCESSING METADATA
   ====================================================== */

ALTER TABLE public.email_events
    ADD COLUMN IF NOT EXISTS subject VARCHAR(255),
    ADD COLUMN IF NOT EXISTS html_content TEXT,
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    ADD COLUMN IF NOT EXISTS attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS max_attempts INTEGER NOT NULL DEFAULT 5,
    ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS processed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;


/* ======================================================
   EMAIL EVENTS INDEXES
   ====================================================== */

CREATE INDEX IF NOT EXISTS idx_email_events_status_next_retry
    ON public.email_events (status, next_retry_at);


/* ======================================================
   VENDOR LEADS PERFORMANCE INDEXES
   ====================================================== */

CREATE INDEX IF NOT EXISTS idx_vendor_leads_vendor_stage
    ON public.vendor_leads (vendor_id, stage);

CREATE INDEX IF NOT EXISTS idx_vendor_leads_vendor_created_at
    ON public.vendor_leads (vendor_id, created_at);