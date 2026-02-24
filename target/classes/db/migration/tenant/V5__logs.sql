/* ======================================================
   LOGS TABLE
   ====================================================== */

CREATE TABLE IF NOT EXISTS %I.logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    level VARCHAR(20) NOT NULL,
    action VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id UUID,
    CONSTRAINT fk_logs_user
        FOREIGN KEY (user_id)
        REFERENCES %I.users(id)
        ON DELETE SET NULL
);