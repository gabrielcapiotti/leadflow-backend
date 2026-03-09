DO $$
DECLARE
    tenant_schema RECORD;
BEGIN
    FOR tenant_schema IN
        SELECT schema_name FROM public.tenants
    LOOP
        EXECUTE format(
            '
            INSERT INTO %I.roles (id, name, created_at, updated_at)
            VALUES
            (''00000000-0000-0000-0000-000000000001'',''ROLE_USER'',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
            (''00000000-0000-0000-0000-000000000002'',''ROLE_ADMIN'',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
            ON CONFLICT (name) DO UPDATE
            SET updated_at = EXCLUDED.updated_at
            ',
            tenant_schema.schema_name
        );
    END LOOP;
END $$;