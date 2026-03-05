CREATE TABLE vendor_lead_stage_history (
    id UUID PRIMARY KEY,
    vendor_lead_id UUID NOT NULL,
    previous_stage VARCHAR(100) NOT NULL,
    new_stage VARCHAR(100) NOT NULL,
    changed_at TIMESTAMP NOT NULL
);