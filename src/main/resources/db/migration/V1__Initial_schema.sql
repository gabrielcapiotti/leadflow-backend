/* ======================================================
   EXTENSIONS
   ====================================================== */

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


/* ======================================================
   TENANTS
   ====================================================== */

CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    name VARCHAR(100) NOT NULL,
    schema_name VARCHAR(100) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT uq_tenants_name UNIQUE (name),
    CONSTRAINT uq_tenants_schema_name UNIQUE (schema_name)
);


/* ======================================================
   ROLES (GLOBAL)
   ====================================================== */

CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    name VARCHAR(50) NOT NULL UNIQUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


/* ======================================================
   USERS (MULTI-TENANT)
   ====================================================== */

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    tenant_id UUID NOT NULL,

    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,

    role_id UUID NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_users_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_users_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenants(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_users_email_tenant
        UNIQUE (email, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_users_tenant
ON users (tenant_id);

CREATE INDEX IF NOT EXISTS idx_users_email
ON users (email);


/* ======================================================
   LEADS (MULTI-TENANT)
   ====================================================== */

CREATE TABLE IF NOT EXISTS leads (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    tenant_id UUID NOT NULL,

    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),

    status VARCHAR(50) NOT NULL DEFAULT 'NEW',

    user_id UUID NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT chk_lead_status
        CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'CLOSED')),

    CONSTRAINT fk_leads_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_leads_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenants(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_leads_email_user_tenant
        UNIQUE (email, user_id, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_leads_tenant
ON leads (tenant_id);

CREATE INDEX IF NOT EXISTS idx_leads_user
ON leads (user_id);

CREATE INDEX IF NOT EXISTS idx_leads_status
ON leads (status);


/* ======================================================
   LEAD STATUS HISTORY (MULTI-TENANT)
   ====================================================== */

CREATE TABLE IF NOT EXISTS lead_status_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    lead_id UUID NOT NULL,
    tenant_id UUID NOT NULL,

    status VARCHAR(50) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by UUID,

    CONSTRAINT chk_lsh_status
        CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'CLOSED')),

    CONSTRAINT fk_lsh_lead
        FOREIGN KEY (lead_id)
        REFERENCES leads(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_lsh_user
        FOREIGN KEY (changed_by)
        REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_lsh_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenants(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_lsh_lead
ON lead_status_history (lead_id);

CREATE INDEX IF NOT EXISTS idx_lsh_tenant
ON lead_status_history (tenant_id);


/* ======================================================
   SETTINGS (MULTI-TENANT)
   ====================================================== */

CREATE TABLE IF NOT EXISTS settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,

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
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_settings_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenants(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_settings_user_tenant
        UNIQUE (user_id, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_settings_tenant
ON settings (tenant_id);


/* ======================================================
   LOGS (MULTI-TENANT)
   ====================================================== */

CREATE TABLE IF NOT EXISTS logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    level VARCHAR(20) NOT NULL,
    action VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    user_id UUID,
    tenant_id UUID NOT NULL,

    CONSTRAINT fk_logs_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_logs_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenants(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_logs_tenant
ON logs (tenant_id);

CREATE INDEX IF NOT EXISTS idx_logs_created_at
ON logs (created_at DESC);