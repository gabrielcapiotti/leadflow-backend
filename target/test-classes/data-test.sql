CREATE TABLE roles (
    id INT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

INSERT INTO roles (id, name) VALUES (1, 'USER');
INSERT INTO roles (id, name) VALUES (2, 'ADMIN');