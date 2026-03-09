/* ======================================================
   CREATE TENANTS TABLE (PUBLIC SCHEMA)
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.tenants (

    /* ========== IDENTIDADE ========== */

    id UUID NOT NULL,

    /* ========== DADOS DO TENANT ========== */

    name VARCHAR(100) NOT NULL,
    schema_name VARCHAR(100) NOT NULL,

    /* ========== AUDITORIA ========== */

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    /* ========== CONSTRAINTS ========== */

    CONSTRAINT pk_tenants PRIMARY KEY (id),

    CONSTRAINT uq_tenants_name UNIQUE (name),
    CONSTRAINT uq_tenants_schema_name UNIQUE (schema_name)
);

/* ======================================================
   INDEXES
   ====================================================== */

CREATE INDEX IF NOT EXISTS idx_tenants_schema_name
    ON public.tenants (schema_name);

CREATE INDEX IF NOT EXISTS idx_tenants_deleted_at
    ON public.tenants (deleted_at);

CREATE INDEX IF NOT EXISTS idx_tenants_created_at
    ON public.tenants (created_at);