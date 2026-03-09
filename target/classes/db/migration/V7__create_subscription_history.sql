/* ======================================================
   SUBSCRIPTION HISTORY
   Tracks vendor subscription status changes
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.subscription_history (
    id UUID PRIMARY KEY,

    vendor_id UUID NOT NULL,

    previous_status VARCHAR(32) NOT NULL,
    new_status VARCHAR(32) NOT NULL,

    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    reason VARCHAR(255),
    external_event_id VARCHAR(255),

    CONSTRAINT fk_subscription_history_vendor
        FOREIGN KEY (vendor_id)
        REFERENCES public.vendors (id)
        ON DELETE CASCADE
);

-- Index for vendor timeline queries
CREATE INDEX IF NOT EXISTS idx_subscription_history_vendor_changed
    ON public.subscription_history (vendor_id, changed_at DESC);

-- Index for webhook / billing event lookups
CREATE INDEX IF NOT EXISTS idx_subscription_history_external_event
    ON public.subscription_history (external_event_id);