/* ======================================================
   LEADS TABLE
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.leads (

    id UUID PRIMARY KEY,

    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),

    status VARCHAR(30) NOT NULL DEFAULT 'NEW',

    user_id UUID NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_lead_status
        CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'CLOSED')),

    CONSTRAINT fk_leads_user
        FOREIGN KEY (user_id)
        REFERENCES public.users(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_leads_email_user
        UNIQUE (email, user_id)
);


/* ======================================================
   INDEXES
   ====================================================== */

-- Fast lookup by user (common CRM query)
CREATE INDEX IF NOT EXISTS idx_leads_user
    ON public.leads (user_id);

-- Filter by status for pipelines / dashboards
CREATE INDEX IF NOT EXISTS idx_leads_status
    ON public.leads (status);

-- Timeline queries
CREATE INDEX IF NOT EXISTS idx_leads_created_at
    ON public.leads (created_at);

-- Soft delete filtering
CREATE INDEX IF NOT EXISTS idx_leads_deleted_at
    ON public.leads (deleted_at);