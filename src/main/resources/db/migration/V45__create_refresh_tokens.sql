/* ======================================================
   TEMPLATE: REFRESH TOKENS
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.template_refresh_tokens (

    id UUID NOT NULL,

    user_id UUID NOT NULL,

    token_hash VARCHAR(255) NOT NULL,

    device_fingerprint VARCHAR(255) NOT NULL,

    expires_at TIMESTAMPTZ NOT NULL,

    revoked BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_template_refresh_tokens
        PRIMARY KEY (id),

    CONSTRAINT uq_template_refresh_tokens_hash
        UNIQUE (token_hash)
);


/* ======================================================
   INDEXES
   ====================================================== */

CREATE INDEX IF NOT EXISTS idx_template_refresh_tokens_user
    ON public.template_refresh_tokens(user_id);

CREATE INDEX IF NOT EXISTS idx_template_refresh_tokens_fingerprint
    ON public.template_refresh_tokens(device_fingerprint);

CREATE INDEX IF NOT EXISTS idx_template_refresh_tokens_expires
    ON public.template_refresh_tokens(expires_at);