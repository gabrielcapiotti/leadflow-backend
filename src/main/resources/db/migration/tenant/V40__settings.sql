/* ======================================================
   SETTINGS TABLE
   ====================================================== */

CREATE TABLE IF NOT EXISTS settings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    vendor_name VARCHAR(100) NOT NULL,
    whatsapp VARCHAR(15) NOT NULL,
    company_name VARCHAR(100),
    logo TEXT,
    welcome_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_settings_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_settings_user UNIQUE (user_id)
);