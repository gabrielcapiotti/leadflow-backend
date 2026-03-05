ALTER TABLE vendor_leads
ADD COLUMN score INTEGER NOT NULL DEFAULT 0;

ALTER TABLE vendor_leads
ADD COLUMN resumo_estrategico TEXT;