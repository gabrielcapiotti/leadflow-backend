/* ======================================================
   LEAD STATUS HISTORY TABLE
   ====================================================== */

CREATE TABLE IF NOT EXISTS %I.lead_status_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    lead_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by UUID,
    CONSTRAINT chk_lsh_status
        CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'CLOSED')),
    CONSTRAINT fk_lsh_lead
        FOREIGN KEY (lead_id)
        REFERENCES %I.leads(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_lsh_user
        FOREIGN KEY (changed_by)
        REFERENCES %I.users(id)
        ON DELETE SET NULL
);