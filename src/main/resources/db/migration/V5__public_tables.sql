/* ======================================================
   GLOBAL TABLES (PUBLIC SCHEMA)
   ====================================================== */

-- ======================================================
-- TENANTS
-- ======================================================

CREATE TABLE IF NOT EXISTS public.tenants (

    id UUID NOT NULL,

    name VARCHAR(100) NOT NULL,
    schema_name VARCHAR(100) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT pk_tenants PRIMARY KEY (id),

    CONSTRAINT uq_tenants_name UNIQUE (name),
    CONSTRAINT uq_tenants_schema_name UNIQUE (schema_name)
);

CREATE INDEX IF NOT EXISTS idx_tenants_schema_name
    ON public.tenants (schema_name);

CREATE INDEX IF NOT EXISTS idx_tenants_deleted_at
    ON public.tenants (deleted_at);



-- ======================================================
-- GLOBAL ROLES
-- ======================================================

CREATE TABLE IF NOT EXISTS public.roles (

    id UUID NOT NULL,

    name VARCHAR(50) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE INDEX IF NOT EXISTS idx_roles_name
    ON public.roles (name);



/* ======================================================
   TEMPLATE TABLES FOR TENANT SCHEMA CLONING
   ====================================================== */

-- ======================================================
-- ROLES TEMPLATE
-- ======================================================

CREATE TABLE IF NOT EXISTS public.template_roles (

    id UUID NOT NULL,

    name VARCHAR(50) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_template_roles PRIMARY KEY (id),
    CONSTRAINT uq_template_roles_name UNIQUE (name)
);

CREATE INDEX IF NOT EXISTS idx_template_roles_name
    ON public.template_roles (name);



-- ======================================================
-- USERS TEMPLATE
-- ======================================================

CREATE TABLE IF NOT EXISTS public.template_users (

    id UUID NOT NULL,

    name VARCHAR(255),
    email VARCHAR(255),
    password VARCHAR(255),

    role_id UUID,

    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT pk_template_users PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_template_users_email
    ON public.template_users (email);

CREATE INDEX IF NOT EXISTS idx_template_users_role
    ON public.template_users (role_id);



-- ======================================================
-- LEADS TEMPLATE
-- ======================================================

CREATE TABLE IF NOT EXISTS public.template_leads (

    id UUID NOT NULL,

    user_id UUID,

    name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),

    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT pk_template_leads PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_template_leads_user_id
    ON public.template_leads (user_id);