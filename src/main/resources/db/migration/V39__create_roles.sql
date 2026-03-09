/* ======================================================
   CREATE ROLES TABLE FOR TENANT SCHEMAS
   ====================================================== */

DO $$
DECLARE
    tenant_schema RECORD;
BEGIN
    FOR tenant_schema IN
        SELECT schema_name
        FROM public.tenants
    LOOP
        EXECUTE format(
            '
            CREATE TABLE IF NOT EXISTS %I.roles (
                id UUID PRIMARY KEY,
                name VARCHAR(50) NOT NULL UNIQUE,
                created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
            );

            CREATE INDEX IF NOT EXISTS idx_roles_name
                ON %I.roles (name);

            CREATE INDEX IF NOT EXISTS idx_roles_created_at
                ON %I.roles (created_at);
            ',
            tenant_schema.schema_name,
            tenant_schema.schema_name,
            tenant_schema.schema_name
        );
    END LOOP;
END $$;