/* ======================================================
   VENDORS (TENANT SCHEMA)
   ====================================================== */

DO $$
BEGIN
    EXECUTE format(
        '
        CREATE TABLE IF NOT EXISTS %I.vendors (

            /* ========== IDENTIDADE ========== */

            id UUID NOT NULL,

            /* ========== DADOS DO VENDOR ========== */

            name VARCHAR(255) NOT NULL,
            description TEXT,

            /* ========== STATUS ========== */

            active BOOLEAN NOT NULL DEFAULT TRUE,

            /* ========== AUDITORIA ========== */

            created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP WITH TIME ZONE,

            /* ========== CONSTRAINTS ========== */

            CONSTRAINT pk_vendors PRIMARY KEY (id),

            CONSTRAINT uq_vendors_name UNIQUE (name)
        );

        /* ========== ÍNDICES ========== */

        CREATE INDEX IF NOT EXISTS idx_vendors_name
            ON %I.vendors (name);

        CREATE INDEX IF NOT EXISTS idx_vendors_deleted_at
            ON %I.vendors (deleted_at);

        CREATE INDEX IF NOT EXISTS idx_vendors_active
            ON %I.vendors (active);

        CREATE INDEX IF NOT EXISTS idx_vendors_created_at
            ON %I.vendors (created_at);
        ',
        current_schema(),
        current_schema(),
        current_schema(),
        current_schema(),
        current_schema()
    );
END $$;