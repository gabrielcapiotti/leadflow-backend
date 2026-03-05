CREATE TABLE vendor_lead_conversations (
    id UUID PRIMARY KEY,
    vendor_lead_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);