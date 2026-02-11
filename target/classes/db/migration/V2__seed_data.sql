INSERT INTO roles (id, name)
VALUES
(1, 'ADMIN'),
(2, 'USER');

INSERT INTO users (id, name, email, password, role_id)
VALUES
(1, 'Admin User', 'admin@leadflow.ai',
 '$2a$10$7QJ1lH8U.F6Qk7Q9d1PpW.u8x0oWgYH1kJ0m6xkT7CwJbZsXhP5bG', 1),
(2, 'Regular User', 'user@leadflow.ai',
 '$2a$10$7QJ1lH8U.F6Qk7Q9d1PpW.u8x0oWgYH1kJ0m6xkT7CwJbZsXhP5bG', 2);

INSERT INTO leads (id, name, email, status, user_id)
VALUES
(1, 'John Doe', 'john.doe@example.com', 'new', 1),
(2, 'Jane Smith', 'jane.smith@example.com', 'new', 1);
