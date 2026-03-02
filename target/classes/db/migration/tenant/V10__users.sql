/* ======================================================
   USERS TABLE
   ====================================================== */

CREATE TABLE IF NOT EXISTS users (

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
    lock_until TIMESTAMP,
    credentials_updated_at TIMESTAMP,

    /* ========== AUDITORIA ========== */

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    /* ========== CONSTRAINTS ========== */

    CONSTRAINT pk_users PRIMARY KEY (id),

    CONSTRAINT fk_users_role
        FOREIGN KEY (role_id)
        REFERENCES public.roles(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_users_email UNIQUE (email)

);

/* ========== ÍNDICES ========== */

CREATE INDEX IF NOT EXISTS idx_users_email
    ON users (email);

CREATE INDEX IF NOT EXISTS idx_users_role
    ON users (role_id);

CREATE INDEX IF NOT EXISTS idx_users_deleted_at
    ON users (deleted_at);