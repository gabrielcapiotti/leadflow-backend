/* ======================================================
   SEED ROLES FOR TENANT SCHEMAS
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

            INSERT INTO %I.roles (id, name) VALUES
            (''00000000-0000-0000-0000-000000000001'', ''ROLE_USER''),
            (''00000000-0000-0000-0000-000000000002'', ''ROLE_ADMIN'')
            ON CONFLICT DO NOTHING;
            ',
            tenant_schema.schema_name,
            tenant_schema.schema_name
        );
    END LOOP;
END $$;