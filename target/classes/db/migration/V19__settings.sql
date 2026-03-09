/* ======================================================
   TEMPLATE: SETTINGS TABLE
   Stores per-user CRM/vendor configuration
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.template_settings (

    id UUID NOT NULL,

    user_id UUID NOT NULL,

    vendor_name VARCHAR(100) NOT NULL,
    whatsapp VARCHAR(15) NOT NULL,

    company_name VARCHAR(100),
    logo TEXT,
    welcome_message TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT pk_template_settings PRIMARY KEY (id),

    CONSTRAINT uq_template_settings_user UNIQUE (user_id)
);


/* ======================================================
   INDEXES
   ====================================================== */

CREATE INDEX IF NOT EXISTS idx_template_settings_user
    ON public.template_settings (user_id);

CREATE INDEX IF NOT EXISTS idx_template_settings_deleted_at
    ON public.template_settings (deleted_at);