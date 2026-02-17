/* ======================================================
   ROLES
   ====================================================== */

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


/* ======================================================
   USERS
   ====================================================== */

INSERT INTO users (name, email, password, role_id, created_at, updated_at)
SELECT 
    'Admin User',
    LOWER('admin@leadflow.ai'),
    '$2a$10$7QJ1lH8U.F6Qk7Q9d1PpW.u8x0oWgYH1kJ0m6xkT7CwJbZsXhP5bG',
    r.id,
    NOW(),
    NOW()
FROM roles r
WHERE r.name = 'ADMIN'
AND NOT EXISTS (
    SELECT 1 FROM users 
    WHERE LOWER(email) = LOWER('admin@leadflow.ai')
      AND deleted_at IS NULL
);

INSERT INTO users (name, email, password, role_id, created_at, updated_at)
SELECT 
    'Regular User',
    LOWER('user@leadflow.ai'),
    '$2a$10$7QJ1lH8U.F6Qk7Q9d1PpW.u8x0oWgYH1kJ0m6xkT7CwJbZsXhP5bG',
    r.id,
    NOW(),
    NOW()
FROM roles r
WHERE r.name = 'USER'
AND NOT EXISTS (
    SELECT 1 FROM users 
    WHERE LOWER(email) = LOWER('user@leadflow.ai')
      AND deleted_at IS NULL
);


/* ======================================================
   LEADS
   ====================================================== */

INSERT INTO leads (name, email, phone, status, user_id, created_at, updated_at)
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
AND NOT EXISTS (
    SELECT 1 FROM leads l
    WHERE LOWER(l.email) = LOWER('john.doe@example.com')
      AND l.user_id = u.id
      AND l.deleted_at IS NULL
);

INSERT INTO leads (name, email, phone, status, user_id, created_at, updated_at)
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
AND NOT EXISTS (
    SELECT 1 FROM leads l
    WHERE LOWER(l.email) = LOWER('jane.smith@example.com')
      AND l.user_id = u.id
      AND l.deleted_at IS NULL
);
