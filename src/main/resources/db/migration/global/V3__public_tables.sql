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

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT uq_tenants_name UNIQUE (name),
    CONSTRAINT uq_tenants_schema_name UNIQUE (schema_name)
);

-- Index for fast schema resolution during tenant routing
CREATE INDEX IF NOT EXISTS idx_tenants_schema_name
    ON public.tenants (schema_name);

-- Index for soft delete queries
CREATE INDEX IF NOT EXISTS idx_tenants_deleted_at
    ON public.tenants (deleted_at);



-- ======================================================
-- ROLES
-- ======================================================

CREATE TABLE IF NOT EXISTS public.roles (
    id UUID PRIMARY KEY,

    name VARCHAR(50) NOT NULL UNIQUE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for role lookups (Spring Security / authorization checks)
CREATE INDEX IF NOT EXISTS idx_roles_name
    ON public.roles (name);