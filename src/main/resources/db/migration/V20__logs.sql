/* ======================================================
   LOGS TABLE
   Stores system and user action logs
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.logs (

    id UUID NOT NULL,

    level VARCHAR(20) NOT NULL,
    action VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    user_id UUID,

    CONSTRAINT pk_logs PRIMARY KEY (id)
);


/* ======================================================
   INDEXES
   ====================================================== */

-- Timeline queries for logs
CREATE INDEX IF NOT EXISTS idx_logs_created_at
    ON public.logs (created_at DESC);

-- Filtering logs by level
CREATE INDEX IF NOT EXISTS idx_logs_level
    ON public.logs (level);

-- Filtering logs by action
CREATE INDEX IF NOT EXISTS idx_logs_action
    ON public.logs (action);

-- Lookup logs related to a specific user
CREATE INDEX IF NOT EXISTS idx_logs_user
    ON public.logs (user_id);