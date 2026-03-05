/* ======================================================
   DEFAULT ADMIN USER SEED
   ====================================================== */

INSERT INTO users (
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
    '00000000-0000-0000-0000-00000000A001'::uuid,
    'Administrador',
    'admin@leadflow.local',
    '$2a$10$eORKR/wJbinHFh6u/KKta.3HX.tTl.GdmktKEVWtScZ2g/4YyAMiW',
    r.id,
    0,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    NULL
FROM public.roles r
WHERE r.name = 'ROLE_ADMIN'
ON CONFLICT (email) DO NOTHING;
