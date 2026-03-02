-- Adiciona a coluna 'credentials_updated_at' na tabela 'users'
ALTER TABLE users
ADD COLUMN credentials_updated_at TIMESTAMP;