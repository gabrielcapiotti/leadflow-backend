ALTER TABLE vendor_audit_logs
    ADD COLUMN IF NOT EXISTS entity_type VARCHAR(100);

UPDATE vendor_audit_logs
SET entity_type = 'UNKNOWN'
WHERE entity_type IS NULL;

ALTER TABLE vendor_audit_logs
    ALTER COLUMN entity_type SET NOT NULL;