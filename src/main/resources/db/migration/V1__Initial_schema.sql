/* ======================================================
   EXTENSIONS
   ====================================================== */

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


/* ======================================================
   GLOBAL TABLES (PUBLIC SCHEMA)
   ====================================================== */

CREATE TABLE IF NOT EXISTS public.tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
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


CREATE TABLE IF NOT EXISTS public.roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


/* ======================================================
   TENANT SCHEMA PROVISIONING
   ====================================================== */

CREATE OR REPLACE FUNCTION public.create_tenant_schema(schema_name TEXT)
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN

    -- Segurança extra contra schema injection
    IF schema_name !~ '^[a-z0-9_]+$' THEN
        RAISE EXCEPTION 'Invalid schema name: %', schema_name;
    END IF;

    EXECUTE format('CREATE SCHEMA IF NOT EXISTS %I', schema_name);


    /* ======================================================
       USERS
       ====================================================== */

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.users (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            name VARCHAR(100) NOT NULL,
            email VARCHAR(100) NOT NULL,
            password VARCHAR(255) NOT NULL,
            role_id UUID NOT NULL,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP,
            CONSTRAINT fk_users_role
                FOREIGN KEY (role_id)
                REFERENCES public.roles(id)
                ON DELETE RESTRICT,
            CONSTRAINT uq_users_email UNIQUE (email)
        )
    ', schema_name);

    EXECUTE format(
        'CREATE INDEX IF NOT EXISTS idx_%I_users_email ON %I.users (email);',
        schema_name, schema_name
    );


    /* ======================================================
       LEADS
       ====================================================== */

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.leads (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            name VARCHAR(100) NOT NULL,
            email VARCHAR(100) NOT NULL,
            phone VARCHAR(20),
            status VARCHAR(30) NOT NULL DEFAULT ''NEW'',
            user_id UUID NOT NULL,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP,
            CONSTRAINT chk_lead_status
                CHECK (status IN (''NEW'', ''CONTACTED'', ''QUALIFIED'', ''CLOSED'')),
            CONSTRAINT fk_leads_user
                FOREIGN KEY (user_id)
                REFERENCES %I.users(id)
                ON DELETE CASCADE,
            CONSTRAINT uq_leads_email_user
                UNIQUE (email, user_id)
        )
    ', schema_name, schema_name);

    EXECUTE format(
        'CREATE INDEX IF NOT EXISTS idx_%I_leads_user ON %I.leads (user_id);',
        schema_name, schema_name
    );

    EXECUTE format(
        'CREATE INDEX IF NOT EXISTS idx_%I_leads_status ON %I.leads (status);',
        schema_name, schema_name
    );


    /* ======================================================
       LEAD STATUS HISTORY
       ====================================================== */

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.lead_status_history (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            lead_id UUID NOT NULL,
            status VARCHAR(30) NOT NULL,
            changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            changed_by UUID,
            CONSTRAINT chk_lsh_status
                CHECK (status IN (''NEW'', ''CONTACTED'', ''QUALIFIED'', ''CLOSED'')),
            CONSTRAINT fk_lsh_lead
                FOREIGN KEY (lead_id)
                REFERENCES %I.leads(id)
                ON DELETE CASCADE,
            CONSTRAINT fk_lsh_user
                FOREIGN KEY (changed_by)
                REFERENCES %I.users(id)
                ON DELETE SET NULL
        )
    ', schema_name, schema_name, schema_name);

    EXECUTE format(
        'CREATE INDEX IF NOT EXISTS idx_%I_lsh_lead ON %I.lead_status_history (lead_id);',
        schema_name, schema_name
    );


    /* ======================================================
       SETTINGS
       ====================================================== */

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.settings (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            user_id UUID NOT NULL,
            vendor_name VARCHAR(100) NOT NULL,
            whatsapp VARCHAR(15) NOT NULL,
            company_name VARCHAR(100),
            logo TEXT,
            welcome_message TEXT,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP,
            CONSTRAINT fk_settings_user
                FOREIGN KEY (user_id)
                REFERENCES %I.users(id)
                ON DELETE CASCADE,
            CONSTRAINT uq_settings_user UNIQUE (user_id)
        )
    ', schema_name, schema_name);

    EXECUTE format(
        'CREATE INDEX IF NOT EXISTS idx_%I_settings_user ON %I.settings (user_id);',
        schema_name, schema_name
    );


    /* ======================================================
       LOGS
       ====================================================== */

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.logs (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            level VARCHAR(20) NOT NULL,
            action VARCHAR(100) NOT NULL,
            message TEXT NOT NULL,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            user_id UUID,
            CONSTRAINT fk_logs_user
                FOREIGN KEY (user_id)
                REFERENCES %I.users(id)
                ON DELETE SET NULL
        )
    ', schema_name, schema_name);

END;
$$;