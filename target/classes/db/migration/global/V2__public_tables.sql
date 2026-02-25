/* ======================================================
   GLOBAL TABLES (PUBLIC SCHEMA)
   ====================================================== */

-- ======================================================
-- TENANTS
-- ======================================================

CREATE TABLE IF NOT EXISTS public.tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    schema_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT uq_tenants_name UNIQUE (name),
    CONSTRAINT uq_tenants_schema_name UNIQUE (schema_name)
);

CREATE INDEX IF NOT EXISTS idx_tenants_schema_name
    ON public.tenants (schema_name);


-- ======================================================
-- ROLES
-- ======================================================

CREATE TABLE IF NOT EXISTS public.roles (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);