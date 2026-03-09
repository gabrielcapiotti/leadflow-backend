/* ======================================================
   DEFAULT ADMIN USER SEED (FOR EACH TENANT)
   ====================================================== */

DO $$
DECLARE
    tenant_schema RECORD;
BEGIN
    FOR tenant_schema IN
        SELECT schema_name FROM public.tenants
    LOOP
        EXECUTE format(
            '
            INSERT INTO %I.users (
                id,
                name,
                email,
                password,
                role_id,
                failed_attempts,
                lock_until,
                credentials_updated_at,
                created_at,
                updated_at,
                deleted_at
            )
            SELECT
                ''00000000-0000-0000-0000-00000000A001''::uuid,
                ''Administrador'',
                ''admin@leadflow.local'',
                ''$2a$10$eORKR/wJbinHFh6u/KKta.3HX.tTl.GdmktKEVWtScZ2g/4YyAMiW'',
                r.id,
                0,
                NULL,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP,
                NULL
            FROM %I.roles r
            WHERE r.name = ''ROLE_ADMIN''
            AND NOT EXISTS (
                SELECT 1
                FROM %I.users u
                WHERE u.email = ''admin@leadflow.local''
            )
            ',
            tenant_schema.schema_name,
            tenant_schema.schema_name,
            tenant_schema.schema_name
        );
    END LOOP;
END $$;