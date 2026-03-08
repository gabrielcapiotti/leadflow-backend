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
