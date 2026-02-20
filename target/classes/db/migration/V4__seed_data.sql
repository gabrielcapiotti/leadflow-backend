/* ======================================================
   TENANTS (GLOBAL - PUBLIC)
   ====================================================== */

INSERT INTO public.tenants (name, schema_name, created_at, updated_at)
VALUES ('Default Tenant', 'public', NOW(), NOW())
ON CONFLICT (schema_name) DO NOTHING;

INSERT INTO public.tenants (name, schema_name, created_at, updated_at)
VALUES ('Tenant A', 'tenant_a', NOW(), NOW())
ON CONFLICT (schema_name) DO NOTHING;

INSERT INTO public.tenants (name, schema_name, created_at, updated_at)
VALUES ('Tenant B', 'tenant_b', NOW(), NOW())
ON CONFLICT (schema_name) DO NOTHING;



/* ======================================================
   ROLES (GLOBAL - PUBLIC)
   ====================================================== */

INSERT INTO public.roles (name, created_at, updated_at)
VALUES ('ADMIN', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.roles (name, created_at, updated_at)
VALUES ('USER', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;



/* ======================================================
   DEFAULT TENANT DATA (schema: public)
   ====================================================== */

SET search_path TO public;



/* ======================================================
   USERS
   ====================================================== */

-- ADMIN USER
INSERT INTO users (
    name,
    email,
    password,
    role_id,
    created_at,
    updated_at
)
SELECT 
    'Admin User',
    LOWER('admin@leadflow.ai'),
    '$2a$10$7QJ1lH8U.F6Qk7Q9d1PpW.u8x0oWgYH1kJ0m6xkT7CwJbZsXhP5bG',
    r.id,
    NOW(),
    NOW()
FROM public.roles r
WHERE LOWER(r.name) = LOWER('ADMIN')
AND NOT EXISTS (
    SELECT 1
    FROM public.users u
    WHERE LOWER(u.email) = LOWER('admin@leadflow.ai')
      AND u.deleted_at IS NULL
);

-- REGULAR USER
INSERT INTO users (
    name,
    email,
    password,
    role_id,
    created_at,
    updated_at
)
SELECT 
    'Regular User',
    LOWER('user@leadflow.ai'),
    '$2a$10$7QJ1lH8U.F6Qk7Q9d1PpW.u8x0oWgYH1kJ0m6xkT7CwJbZsXhP5bG',
    r.id,
    NOW(),
    NOW()
FROM public.roles r
WHERE LOWER(r.name) = LOWER('USER')
AND NOT EXISTS (
    SELECT 1
    FROM public.users u
    WHERE LOWER(u.email) = LOWER('user@leadflow.ai')
      AND u.deleted_at IS NULL
);



/* ======================================================
   LEADS
   ====================================================== */

-- LEAD 1
INSERT INTO leads (
    name,
    email,
    phone,
    status,
    user_id,
    created_at,
    updated_at
)
SELECT
    'John Doe',
    LOWER('john.doe@example.com'),
    NULL,
    'NEW',
    u.id,
    NOW(),
    NOW()
FROM users u
WHERE LOWER(u.email) = LOWER('admin@leadflow.ai')
AND u.deleted_at IS NULL
AND NOT EXISTS (
    SELECT 1
    FROM leads l
    WHERE LOWER(l.email) = LOWER('john.doe@example.com')
      AND l.user_id = u.id
      AND l.deleted_at IS NULL
);

-- LEAD 2
INSERT INTO leads (
    name,
    email,
    phone,
    status,
    user_id,
    created_at,
    updated_at
)
SELECT
    'Jane Smith',
    LOWER('jane.smith@example.com'),
    NULL,
    'NEW',
    u.id,
    NOW(),
    NOW()
FROM users u
WHERE LOWER(u.email) = LOWER('admin@leadflow.ai')
AND u.deleted_at IS NULL
AND NOT EXISTS (
    SELECT 1
    FROM leads l
    WHERE LOWER(l.email) = LOWER('jane.smith@example.com')
      AND l.user_id = u.id
      AND l.deleted_at IS NULL
);



/* ======================================================
   RESTORE DEFAULT SEARCH PATH
   ====================================================== */

RESET search_path;