-- V1__Public_schema.sql
-- Executado apenas uma vez

----------------------------------------------------------
-- EXTENSIONS
----------------------------------------------------------

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


----------------------------------------------------------
-- TENANTS (GLOBAL REGISTRY)
----------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    name VARCHAR(100) NOT NULL,
    schema_name VARCHAR(100) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT uq_tenants_name UNIQUE (name),
    CONSTRAINT uq_tenants_schema_name UNIQUE (schema_name),

    -- Segurança adicional contra schema injection
    CONSTRAINT chk_tenants_schema_name
        CHECK (schema_name ~ '^[a-z0-9_]+$')
);

CREATE INDEX IF NOT EXISTS idx_tenants_schema_name
ON public.tenants(schema_name);

-- Índice parcial para soft delete
CREATE INDEX IF NOT EXISTS idx_tenants_active
ON public.tenants(schema_name)
WHERE deleted_at IS NULL;



----------------------------------------------------------
-- ROLES (GLOBAL)
----------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    name VARCHAR(50) NOT NULL UNIQUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

----------------------------------------------------------
-- SEED ROLES (IDEMPOTENTE)
----------------------------------------------------------

INSERT INTO public.roles (name)
VALUES ('ROLE_ADMIN')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.roles (name)
VALUES ('ROLE_USER')
ON CONFLICT (name) DO NOTHING;