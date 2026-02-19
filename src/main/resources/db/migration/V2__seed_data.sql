/* ======================================================
   TENANTS
   ====================================================== */

-- DEFAULT (sistema / bootstrap)
INSERT INTO tenants (name, schema_name, created_at, updated_at)
VALUES ('Default Tenant', 'public', NOW(), NOW())
ON CONFLICT (schema_name) DO NOTHING;

-- TENANTS PARA TESTE DE MULTI-TENANCY
INSERT INTO tenants (name, schema_name, created_at, updated_at)
VALUES ('Tenant A', 'tenant_a', NOW(), NOW())
ON CONFLICT (schema_name) DO NOTHING;

INSERT INTO tenants (name, schema_name, created_at, updated_at)
VALUES ('Tenant B', 'tenant_b', NOW(), NOW())
ON CONFLICT (schema_name) DO NOTHING;



/* ======================================================
   ROLES
   ====================================================== */

INSERT INTO roles (name, created_at, updated_at)
VALUES ('ADMIN', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name, created_at, updated_at)
VALUES ('USER', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;



/* ======================================================
   USERS (APENAS PARA DEFAULT TENANT)
   ====================================================== */

-- ADMIN USER
INSERT INTO users (
    name,
    email,
    password,
    role_id,
    tenant_id,
    created_at,
    updated_at
)
SELECT 
    'Admin User',
    LOWER('admin@leadflow.ai'),
    '$2a$10$7QJ1lH8U.F6Qk7Q9d1PpW.u8x0oWgYH1kJ0m6xkT7CwJbZsXhP5bG',
    r.id,
    t.id,
    NOW(),
    NOW()
FROM roles r
JOIN tenants t ON LOWER(t.schema_name) = LOWER('public')
WHERE LOWER(r.name) = LOWER('ADMIN')
AND NOT EXISTS (
    SELECT 1 FROM users 
    WHERE LOWER(email) = LOWER('admin@leadflow.ai')
      AND deleted_at IS NULL
);

-- REGULAR USER
INSERT INTO users (
    name,
    email,
    password,
    role_id,
    tenant_id,
    created_at,
    updated_at
)
SELECT 
    'Regular User',
    LOWER('user@leadflow.ai'),
    '$2a$10$7QJ1lH8U.F6Qk7Q9d1PpW.u8x0oWgYH1kJ0m6xkT7CwJbZsXhP5bG',
    r.id,
    t.id,
    NOW(),
    NOW()
FROM roles r
JOIN tenants t ON LOWER(t.schema_name) = LOWER('public')
WHERE LOWER(r.name) = LOWER('USER')
AND NOT EXISTS (
    SELECT 1 FROM users 
    WHERE LOWER(email) = LOWER('user@leadflow.ai')
      AND deleted_at IS NULL
);



/* ======================================================
   LEADS (APENAS DEFAULT TENANT)
   ====================================================== */

-- LEAD 1
INSERT INTO leads (
    name,
    email,
    phone,
    status,
    user_id,
    tenant_id,
    created_at,
    updated_at
)
SELECT
    'John Doe',
    LOWER('john.doe@example.com'),
    NULL,
    'NEW',
    u.id,
    u.tenant_id,
    NOW(),
    NOW()
FROM users u
JOIN tenants t ON t.id = u.tenant_id
WHERE LOWER(u.email) = LOWER('admin@leadflow.ai')
AND LOWER(t.schema_name) = LOWER('public')
AND u.deleted_at IS NULL
AND NOT EXISTS (
    SELECT 1 FROM leads l
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
    tenant_id,
    created_at,
    updated_at
)
SELECT
    'Jane Smith',
    LOWER('jane.smith@example.com'),
    NULL,
    'NEW',
    u.id,
    u.tenant_id,
    NOW(),
    NOW()
FROM users u
JOIN tenants t ON t.id = u.tenant_id
WHERE LOWER(u.email) = LOWER('admin@leadflow.ai')
AND LOWER(t.schema_name) = LOWER('public')
AND u.deleted_at IS NULL
AND NOT EXISTS (
    SELECT 1 FROM leads l
    WHERE LOWER(l.email) = LOWER('jane.smith@example.com')
      AND l.user_id = u.id
      AND l.deleted_at IS NULL
);
