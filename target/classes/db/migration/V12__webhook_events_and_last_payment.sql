-- Add last payment timestamp to vendors
ALTER TABLE public.vendors
    ADD COLUMN IF NOT EXISTS last_payment_at TIMESTAMP WITH TIME ZONE;

-- Table to store processed webhook events (idempotency protection)
CREATE TABLE IF NOT EXISTS public.webhook_events (
    event_id VARCHAR(255) PRIMARY KEY,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Optional index for event lookup by time
CREATE INDEX IF NOT EXISTS idx_webhook_events_received_at
    ON public.webhook_events(received_at);