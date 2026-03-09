/* ======================================================
   TEMPLATE: PASSWORD RESET TOKEN
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.template_password_reset_tokens (

    id UUID NOT NULL,

    user_id UUID NOT NULL,

    token VARCHAR(255) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,

    used BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_template_password_reset_tokens
        PRIMARY KEY (id),

    CONSTRAINT uq_template_password_reset_token
        UNIQUE (token)
);


/* ======================================================
   INDEXES
   ====================================================== */

CREATE INDEX IF NOT EXISTS idx_template_password_reset_token_user
    ON public.template_password_reset_tokens(user_id);

CREATE INDEX IF NOT EXISTS idx_template_password_reset_token_token
    ON public.template_password_reset_tokens(token);

CREATE INDEX IF NOT EXISTS idx_template_password_reset_token_expires
    ON public.template_password_reset_tokens(expires_at);