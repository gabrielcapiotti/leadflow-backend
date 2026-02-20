-- V2__Tenant_schema.sql
-- Tenant domain tables (executado dentro do schema do tenant)

----------------------------------------------------------
-- USERS
----------------------------------------------------------

CREATE TABLE IF NOT EXISTS users (
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
);

CREATE INDEX IF NOT EXISTS idx_users_email
ON users(email);

----------------------------------------------------------
-- LEADS
----------------------------------------------------------

CREATE TABLE IF NOT EXISTS leads (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),

    status VARCHAR(50) NOT NULL DEFAULT 'NEW',

    user_id UUID NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT chk_leads_status
        CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'CLOSED')),

    CONSTRAINT fk_leads_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_leads_email ON leads(email);
CREATE INDEX IF NOT EXISTS idx_leads_status ON leads(status);
CREATE INDEX IF NOT EXISTS idx_leads_user ON leads(user_id);

----------------------------------------------------------
-- LEAD STATUS HISTORY
----------------------------------------------------------

CREATE TABLE IF NOT EXISTS lead_status_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    lead_id UUID NOT NULL,
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
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_lsh_lead ON lead_status_history(lead_id);

----------------------------------------------------------
-- SETTINGS
----------------------------------------------------------

CREATE TABLE IF NOT EXISTS settings (
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
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_settings_user UNIQUE (user_id)
);

CREATE INDEX IF NOT EXISTS idx_settings_user ON settings(user_id);

----------------------------------------------------------
-- LOGS
----------------------------------------------------------

CREATE TABLE IF NOT EXISTS logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    level VARCHAR(20) NOT NULL,
    action VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    user_id UUID,

    CONSTRAINT fk_logs_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_logs_user ON logs(user_id);