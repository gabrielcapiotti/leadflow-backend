-- Migration to add new fields to the vendors table

ALTER TABLE vendors
ADD COLUMN IF NOT EXISTS nome_empresa VARCHAR(255);

ALTER TABLE vendors
ADD COLUMN IF NOT EXISTS nome_vendedor VARCHAR(255);

ALTER TABLE vendors
ADD COLUMN IF NOT EXISTS mensagem_boas_vindas TEXT;

ALTER TABLE vendors
ADD COLUMN IF NOT EXISTS whatsapp_vendedor VARCHAR(50);