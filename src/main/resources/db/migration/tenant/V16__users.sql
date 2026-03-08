/* ======================================================
   USERS TABLE
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.users (

    /* ========== IDENTIDADE ========== */

    id UUID NOT NULL,

    /* ========== DADOS BÁSICOS ========== */

    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,

    /* ========== CONTROLE DE ACESSO ========== */

    role_id UUID NOT NULL,

    /* ========== SEGURANÇA ========== */

    failed_attempts INTEGER NOT NULL DEFAULT 0,
    lock_until TIMESTAMP WITH TIME ZONE,
    credentials_updated_at TIMESTAMP WITH TIME ZONE,

    /* ========== AUDITORIA ========== */

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,

    /* ========== CONSTRAINTS ========== */

    CONSTRAINT pk_users PRIMARY KEY (id),

    CONSTRAINT fk_users_role
        FOREIGN KEY (role_id)
        REFERENCES public.roles(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_users_email UNIQUE (email)
);

/* ========== ÍNDICES ========== */

-- Lookup during authentication
CREATE INDEX IF NOT EXISTS idx_users_email
    ON public.users (email);

-- Role based queries (admin dashboards / authorization)
CREATE INDEX IF NOT EXISTS idx_users_role
    ON public.users (role_id);

-- Soft delete filtering
CREATE INDEX IF NOT EXISTS idx_users_deleted_at
    ON public.users (deleted_at);

-- Useful for account lock logic
CREATE INDEX IF NOT EXISTS idx_users_lock_until
    ON public.users (lock_until);