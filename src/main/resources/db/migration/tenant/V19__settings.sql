/* ======================================================
   SETTINGS TABLE
   Stores per-user CRM/vendor configuration
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.settings (

    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    vendor_name VARCHAR(100) NOT NULL,
    whatsapp VARCHAR(15) NOT NULL,

    company_name VARCHAR(100),
    logo TEXT,
    welcome_message TEXT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_settings_user
        FOREIGN KEY (user_id)
        REFERENCES public.users(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_settings_user UNIQUE (user_id)
);


/* ======================================================
   INDEXES
   ====================================================== */

-- Fast lookup of settings by user (very common query)
CREATE INDEX IF NOT EXISTS idx_settings_user
    ON public.settings (user_id);

-- Soft delete filtering
CREATE INDEX IF NOT EXISTS idx_settings_deleted_at
    ON public.settings (deleted_at);