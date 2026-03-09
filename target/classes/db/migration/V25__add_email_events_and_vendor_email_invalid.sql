/* ======================================================
   VENDOR EMAIL VALIDATION
   ====================================================== */

ALTER TABLE public.vendors
    ADD COLUMN IF NOT EXISTS email_invalid BOOLEAN NOT NULL DEFAULT FALSE;


/* ======================================================
   EMAIL EVENTS
   Stores bounce, complaint and delivery events
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.email_events (

    id UUID PRIMARY KEY,

    email VARCHAR(255) NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,

    reason TEXT
);


/* ======================================================
   INDEXES
   ====================================================== */

-- Lookup by email
CREATE INDEX IF NOT EXISTS idx_email_events_email
    ON public.email_events (email);

-- Filtering by event type
CREATE INDEX IF NOT EXISTS idx_email_events_event_type
    ON public.email_events (event_type);

-- Timeline queries
CREATE INDEX IF NOT EXISTS idx_email_events_occurred_at
    ON public.email_events (occurred_at DESC);

-- Combined lookup (common query pattern)
CREATE INDEX IF NOT EXISTS idx_email_events_email_type
    ON public.email_events (email, event_type);