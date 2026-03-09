/* ======================================================
   PAYMENT EVENTS
   Stores processed payment/webhook events to ensure
   idempotent processing of external billing providers
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.payment_events (
    id UUID PRIMARY KEY,

    provider_event_id VARCHAR(255) NOT NULL,
    provider VARCHAR(100) NOT NULL,

    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_payment_events_provider_event
        UNIQUE (provider_event_id)
);

-- Index for chronological queries and cleanup jobs
CREATE INDEX IF NOT EXISTS idx_payment_events_processed_at
    ON public.payment_events (processed_at);

-- Index for fast provider lookups
CREATE INDEX IF NOT EXISTS idx_payment_events_provider
    ON public.payment_events (provider);