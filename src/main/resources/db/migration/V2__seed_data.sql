-- ==========================
-- ROLES
-- ==========================

INSERT INTO roles (name, created_at, updated_at)
SELECT 'ADMIN', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE name = 'ADMIN'
);

INSERT INTO roles (name, created_at, updated_at)
SELECT 'USER', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE name = 'USER'
);

-- ==========================
-- USERS
-- ==========================

INSERT INTO users (name, email, password, role_id, created_at, updated_at)
SELECT 
    'Admin User',
    'admin@leadflow.ai',
    '$2a$10$7QJ1lH8U.F6Qk7Q9d1PpW.u8x0oWgYH1kJ0m6xkT7CwJbZsXhP5bG',
    r.id,
    NOW(),
    NOW()
FROM roles r
WHERE r.name = 'ADMIN'
AND NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@leadflow.ai'
);

INSERT INTO users (name, email, password, role_id, created_at, updated_at)
SELECT 
    'Regular User',
    'user@leadflow.ai',
    '$2a$10$7QJ1lH8U.F6Qk7Q9d1PpW.u8x0oWgYH1kJ0m6xkT7CwJbZsXhP5bG',
    r.id,
    NOW(),
    NOW()
FROM roles r
WHERE r.name = 'USER'
AND NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'user@leadflow.ai'
);

-- ==========================
-- LEADS
-- ==========================

INSERT INTO leads (name, email, phone, status, user_id, created_at, updated_at)
SELECT
    'John Doe',
    'john.doe@example.com',
    NULL,
    'NEW',
    u.id,
    NOW(),
    NOW()
FROM users u
WHERE u.email = 'admin@leadflow.ai'
AND NOT EXISTS (
    SELECT 1 FROM leads WHERE email = 'john.doe@example.com'
);

INSERT INTO leads (name, email, phone, status, user_id, created_at, updated_at)
SELECT
    'Jane Smith',
    'jane.smith@example.com',
    NULL,
    'NEW',
    u.id,
    NOW(),
    NOW()
FROM users u
WHERE u.email = 'admin@leadflow.ai'
AND NOT EXISTS (
    SELECT 1 FROM leads WHERE email = 'jane.smith@example.com'
);
