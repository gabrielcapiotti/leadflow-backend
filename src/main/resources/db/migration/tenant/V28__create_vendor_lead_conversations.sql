/* ======================================================
   VENDOR LEAD CONVERSATIONS
   Stores messages exchanged with a lead
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.vendor_lead_conversations (

    id UUID PRIMARY KEY,

    vendor_lead_id UUID NOT NULL,

    role VARCHAR(20) NOT NULL,

    content TEXT NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_vendor_lead_conversations_lead
        FOREIGN KEY (vendor_lead_id)
        REFERENCES public.vendor_leads(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_vendor_lead_conversation_role
        CHECK (role IN ('USER','ASSISTANT','SYSTEM'))
);


/* ======================================================
   INDEXES
   ====================================================== */

-- Conversation timeline per lead
CREATE INDEX IF NOT EXISTS idx_vendor_lead_conversations_lead_created
    ON public.vendor_lead_conversations (vendor_lead_id, created_at DESC);

-- Filtering by role
CREATE INDEX IF NOT EXISTS idx_vendor_lead_conversations_role
    ON public.vendor_lead_conversations (role);

-- Fast lookup by lead only
CREATE INDEX IF NOT EXISTS idx_vendor_lead_conversations_lead
    ON public.vendor_lead_conversations (vendor_lead_id);