/* ======================================================
   TENANTS (GLOBAL - PUBLIC)
   ====================================================== */

INSERT INTO public.tenants (name, schema_name)
VALUES ('Default Tenant', 'public')
ON CONFLICT (schema_name) DO NOTHING;

INSERT INTO public.tenants (name, schema_name)
VALUES ('Tenant A', 'tenant_a')
ON CONFLICT (schema_name) DO NOTHING;

INSERT INTO public.tenants (name, schema_name)
VALUES ('Tenant B', 'tenant_b')
ON CONFLICT (schema_name) DO NOTHING;


/* ======================================================
   ENSURE TENANT SCHEMAS EXIST (DEV/TEST ONLY)
   ====================================================== */

CREATE SCHEMA IF NOT EXISTS tenant_a;
CREATE SCHEMA IF NOT EXISTS tenant_b;


/* ======================================================
   ROLES (GLOBAL)
   ====================================================== */

INSERT INTO public.roles (name)
VALUES ('ROLE_ADMIN')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.roles (name)
VALUES ('ROLE_USER')
ON CONFLICT (name) DO NOTHING;


/* ======================================================
   DEFAULT TENANT DATA (schema: public)
   ====================================================== */

/*
Senha padrão para ambos usuários:
123456
(hash bcrypt)
*/

-- ADMIN USER
INSERT INTO public.users (
    name,
    email,
    password,
    role_id
)
SELECT 
    'Admin User',
    LOWER('admin@leadflow.ai'),
    '$2a$10$7QJ1lH8U.F6Qk7Q9d1PpW.u8x0oWgYH1kJ0m6xkT7CwJbZsXhP5bG',
    r.id
FROM public.roles r
WHERE r.name = 'ROLE_ADMIN'
AND NOT EXISTS (
    SELECT 1
    FROM public.users u
    WHERE LOWER(u.email) = LOWER('admin@leadflow.ai')
      AND u.deleted_at IS NULL
);


-- REGULAR USER
INSERT INTO public.users (
    name,
    email,
    password,
    role_id
)
SELECT 
    'Regular User',
    LOWER('user@leadflow.ai'),
    '$2a$10$7QJ1lH8U.F6Qk7Q9d1PpW.u8x0oWgYH1kJ0m6xkT7CwJbZsXhP5bG',
    r.id
FROM public.roles r
WHERE r.name = 'ROLE_USER'
AND NOT EXISTS (
    SELECT 1
    FROM public.users u
    WHERE LOWER(u.email) = LOWER('user@leadflow.ai')
      AND u.deleted_at IS NULL
);


/* ======================================================
   LEADS (PUBLIC TENANT)
   ====================================================== */

-- LEAD 1
INSERT INTO public.leads (
    name,
    email,
    phone,
    status,
    user_id
)
SELECT
    'John Doe',
    LOWER('john.doe@example.com'),
    NULL,
    'NEW',
    u.id
FROM public.users u
WHERE LOWER(u.email) = LOWER('admin@leadflow.ai')
AND u.deleted_at IS NULL
AND NOT EXISTS (
    SELECT 1
    FROM public.leads l
    WHERE LOWER(l.email) = LOWER('john.doe@example.com')
      AND l.user_id = u.id
      AND l.deleted_at IS NULL
);


-- LEAD 2
INSERT INTO public.leads (
    name,
    email,
    phone,
    status,
    user_id
)
SELECT
    'Jane Smith',
    LOWER('jane.smith@example.com'),
    NULL,
    'NEW',
    u.id
FROM public.users u
WHERE LOWER(u.email) = LOWER('admin@leadflow.ai')
AND u.deleted_at IS NULL
AND NOT EXISTS (
    SELECT 1
    FROM public.leads l
    WHERE LOWER(l.email) = LOWER('jane.smith@example.com')
      AND l.user_id = u.id
      AND l.deleted_at IS NULL
);