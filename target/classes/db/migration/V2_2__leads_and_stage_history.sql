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
