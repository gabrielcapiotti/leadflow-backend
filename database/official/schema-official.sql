-- Leadflow Official PostgreSQL Schema
-- Consolidated script equivalent to flyway-clean/V1..V6

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'subscription_status') THEN
    CREATE TYPE subscription_status AS ENUM ('TRIAL', 'ACTIVE', 'SUSPENDED', 'CANCELED', 'EXPIRED');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'lead_stage') THEN
    CREATE TYPE lead_stage AS ENUM ('NEW', 'QUALIFIED', 'CONTACTED', 'NEGOTIATION', 'WON', 'LOST');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'quota_type') THEN
    CREATE TYPE quota_type AS ENUM ('AI_REQUESTS', 'LEADS', 'USERS');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'risk_level') THEN
    CREATE TYPE risk_level AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL');
  END IF;
END
$$;

CREATE TABLE IF NOT EXISTS roles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(50) NOT NULL UNIQUE,
  permissions JSONB NOT NULL DEFAULT '[]'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  name VARCHAR(150) NOT NULL,
  role_id UUID NOT NULL,
  credentials_updated_at TIMESTAMPTZ,
  failed_attempts INT NOT NULL DEFAULT 0,
  lock_until TIMESTAMPTZ,
  last_login_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMPTZ,
  CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS vendors (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_user_id UUID,
  user_email VARCHAR(255) NOT NULL UNIQUE,
  nome_vendedor VARCHAR(200) NOT NULL,
  whatsapp_vendedor VARCHAR(30) NOT NULL,
  nome_empresa VARCHAR(200),
  logo_url TEXT,
  cor_destaque VARCHAR(7),
  mensagem_boas_vindas TEXT,
  slug VARCHAR(120) UNIQUE,
  subscription_status subscription_status NOT NULL DEFAULT 'TRIAL',
  subscription_started_at TIMESTAMPTZ,
  subscription_expires_at TIMESTAMPTZ,
  email_invalid BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMPTZ,
  CONSTRAINT fk_vendors_owner_user FOREIGN KEY (owner_user_id) REFERENCES users(id),
  CONSTRAINT ck_vendors_cor_destaque CHECK (cor_destaque IS NULL OR cor_destaque ~ '^#[0-9A-Fa-f]{6}$')
);

CREATE TABLE IF NOT EXISTS vendor_leads (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  vendor_id UUID NOT NULL,
  nome_completo VARCHAR(200) NOT NULL,
  whatsapp VARCHAR(30) NOT NULL,
  tipo_consorcio VARCHAR(100),
  valor_credito VARCHAR(100),
  urgencia VARCHAR(50),
  stage lead_stage NOT NULL DEFAULT 'NEW',
  score INT NOT NULL DEFAULT 0,
  owner_email VARCHAR(255),
  observacoes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMPTZ,
  CONSTRAINT fk_vendor_leads_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id),
  CONSTRAINT ck_vendor_leads_score CHECK (score BETWEEN 0 AND 100)
);

CREATE TABLE IF NOT EXISTS vendor_lead_stage_history (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  vendor_lead_id UUID NOT NULL,
  previous_stage lead_stage,
  new_stage lead_stage NOT NULL,
  changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  changed_by VARCHAR(255),
  CONSTRAINT fk_stage_history_lead FOREIGN KEY (vendor_lead_id) REFERENCES vendor_leads(id)
);

CREATE TABLE IF NOT EXISTS vendor_usage (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  vendor_id UUID NOT NULL,
  quota_type quota_type NOT NULL,
  used INT NOT NULL DEFAULT 0,
  period_start TIMESTAMPTZ NOT NULL,
  period_end TIMESTAMPTZ NOT NULL,
  alert_80_sent BOOLEAN NOT NULL DEFAULT FALSE,
  alert_100_sent BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_vendor_usage_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id),
  CONSTRAINT ck_vendor_usage_used_non_negative CHECK (used >= 0),
  CONSTRAINT ck_vendor_usage_period_valid CHECK (period_end > period_start),
  CONSTRAINT uk_vendor_usage_period UNIQUE (vendor_id, quota_type, period_start)
);

CREATE TABLE IF NOT EXISTS vendor_risk_alerts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  vendor_id UUID NOT NULL,
  score INT NOT NULL,
  level risk_level NOT NULL,
  reason TEXT,
  resolved BOOLEAN NOT NULL DEFAULT FALSE,
  resolved_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_vendor_risk_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id),
  CONSTRAINT ck_vendor_risk_score_valid CHECK (score BETWEEN 0 AND 100)
);

CREATE TABLE IF NOT EXISTS subscription_history (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  vendor_id UUID NOT NULL,
  previous_status subscription_status,
  new_status subscription_status NOT NULL,
  changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  reason VARCHAR(255),
  changed_by VARCHAR(255),
  CONSTRAINT fk_subscription_history_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id)
);

CREATE TABLE IF NOT EXISTS email_events (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) NOT NULL,
  event_type VARCHAR(80) NOT NULL,
  reason VARCHAR(255),
  metadata JSONB,
  occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS vendor_audit_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  vendor_id UUID NOT NULL,
  user_email VARCHAR(255) NOT NULL,
  acao VARCHAR(120) NOT NULL,
  entity_type VARCHAR(120),
  entidade_id UUID,
  detalhes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_vendor_audit_logs_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id)
);

CREATE TABLE IF NOT EXISTS security_audit_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  action VARCHAR(50) NOT NULL,
  email VARCHAR(150) NOT NULL,
  tenant VARCHAR(100) NOT NULL,
  success BOOLEAN NOT NULL,
  ip_address VARCHAR(100),
  user_agent VARCHAR(255),
  correlation_id VARCHAR(100),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS tenants (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(100) NOT NULL,
  schema_name VARCHAR(50) NOT NULL UNIQUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS user_sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL,
  tenant_id UUID NOT NULL,
  token_id VARCHAR(36) NOT NULL UNIQUE,
  ip_address VARCHAR(100),
  user_agent VARCHAR(512),
  initial_ip_address VARCHAR(45),
  initial_user_agent VARCHAR(512),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  suspicious BOOLEAN NOT NULL DEFAULT FALSE,
  last_access_at TIMESTAMPTZ,
  revoked_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_user_sessions_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_user_sessions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL,
  token_hash VARCHAR(255) NOT NULL UNIQUE,
  device_fingerprint VARCHAR(255) NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  revoked BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS login_audit (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID,
  tenant_id UUID NOT NULL,
  email VARCHAR(320) NOT NULL,
  ip_address VARCHAR(45),
  user_agent VARCHAR(512),
  success BOOLEAN NOT NULL,
  failure_reason VARCHAR(255),
  suspicious BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS password_reset_token (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  token_hash VARCHAR(255) NOT NULL UNIQUE,
  user_id UUID NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  used BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_role_id ON users (role_id);
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users (deleted_at);
CREATE INDEX IF NOT EXISTS idx_users_credentials_updated_at ON users (credentials_updated_at);
CREATE INDEX IF NOT EXISTS idx_users_lock_until ON users (lock_until);

CREATE INDEX IF NOT EXISTS idx_vendors_owner_user_id ON vendors (owner_user_id);
CREATE INDEX IF NOT EXISTS idx_vendors_subscription_status ON vendors (subscription_status);
CREATE INDEX IF NOT EXISTS idx_vendors_deleted_at ON vendors (deleted_at);
CREATE INDEX IF NOT EXISTS idx_vendors_created_at ON vendors (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_tenants_schema_name ON tenants (schema_name);
CREATE INDEX IF NOT EXISTS idx_tenants_deleted_at ON tenants (deleted_at);

CREATE INDEX IF NOT EXISTS idx_vendor_leads_vendor_created ON vendor_leads (vendor_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_vendor_leads_vendor_stage ON vendor_leads (vendor_id, stage);
CREATE INDEX IF NOT EXISTS idx_vendor_leads_vendor_score ON vendor_leads (vendor_id, score DESC);
CREATE INDEX IF NOT EXISTS idx_vendor_leads_deleted_at ON vendor_leads (deleted_at);

CREATE INDEX IF NOT EXISTS idx_stage_history_lead_changed ON vendor_lead_stage_history (vendor_lead_id, changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_vendor_usage_vendor_type_period ON vendor_usage (vendor_id, quota_type, period_start DESC);
CREATE INDEX IF NOT EXISTS idx_vendor_usage_period_end ON vendor_usage (period_end DESC);

CREATE INDEX IF NOT EXISTS idx_vendor_risk_vendor_created ON vendor_risk_alerts (vendor_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_vendor_risk_resolved ON vendor_risk_alerts (resolved);

CREATE INDEX IF NOT EXISTS idx_subscription_history_vendor_changed ON subscription_history (vendor_id, changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_email_events_email_occurred ON email_events (email, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_email_events_type_occurred ON email_events (event_type, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_vendor_audit_logs_vendor_created ON vendor_audit_logs (vendor_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_vendor_audit_logs_entity ON vendor_audit_logs (entity_type, entidade_id);
CREATE INDEX IF NOT EXISTS idx_vendor_audit_logs_action_created ON vendor_audit_logs (acao, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_sessions_token_tenant ON user_sessions (token_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_tenant ON user_sessions (user_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_active ON user_sessions (active);
CREATE INDEX IF NOT EXISTS idx_user_sessions_last_access ON user_sessions (last_access_at DESC);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_revoked ON refresh_tokens (user_id, revoked);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_fingerprint ON refresh_tokens (device_fingerprint);

CREATE INDEX IF NOT EXISTS idx_security_audit_email ON security_audit_logs (email);
CREATE INDEX IF NOT EXISTS idx_security_audit_tenant ON security_audit_logs (tenant);
CREATE INDEX IF NOT EXISTS idx_security_audit_created ON security_audit_logs (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_login_audit_user_tenant ON login_audit (user_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_login_audit_email_tenant ON login_audit (email, tenant_id);
CREATE INDEX IF NOT EXISTS idx_login_audit_created ON login_audit (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_login_audit_success ON login_audit (success);

CREATE INDEX IF NOT EXISTS idx_password_reset_token_hash ON password_reset_token (token_hash);
CREATE INDEX IF NOT EXISTS idx_password_reset_token_user ON password_reset_token (user_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_token_expires ON password_reset_token (expires_at);
