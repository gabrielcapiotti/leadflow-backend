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
