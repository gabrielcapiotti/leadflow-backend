# Mapeamento de SQL - LeadFlow Backend

## 1. ESTRUTURA DE ENTIDADES E TABELAS

### 1.1 Role (Papel/Permissão)
**Entity**: `com.leadflow.backend.entities.user.Role`
**Table**: `roles`

```sql
CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,  -- Pre-normalized to UPPERCASE in entity
    created_at TIMESTAMP NOT NULL,     -- Mapeado de LocalDateTime
    updated_at TIMESTAMP NOT NULL      -- Mapeado de LocalDateTime
)
```

---

### 1.2 User (Usuário)
**Entity**: `com.leadflow.backend.entities.user.User`
**Table**: `users`

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role_id UUID NOT NULL REFERENCES roles(id),
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    lock_until TIMESTAMP,              -- Pode ser NULL
    credentials_updated_at TIMESTAMP,  -- Pode ser NULL
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP               -- Soft delete (pode ser NULL)
)

CREATE INDEX idx_users_email ON users(email)
CREATE CONSTRAINT uq_users_email UNIQUE (email)
```

---

### 1.3 UserSession (Sessão de Autenticação)
**Entity**: `com.leadflow.backend.entities.auth.UserSession`
**Table**: `user_sessions`

```sql
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    token_id VARCHAR(36) NOT NULL UNIQUE,
    ip_address VARCHAR(45),            -- NULL allowed
    user_agent VARCHAR(512),           -- NULL allowed
    active BOOLEAN NOT NULL DEFAULT true,
    initial_ip_address VARCHAR(45),    -- NULL allowed
    initial_user_agent VARCHAR(512),   -- NULL allowed
    last_access_at TIMESTAMP,          -- Mapeado de Instant (pode ser NULL)
    revoked_at TIMESTAMP,              -- Mapeado de Instant (pode ser NULL)
    suspicious BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL      -- Mapeado de Instant
)

CREATE INDEX idx_session_token_tenant ON user_sessions(token_id, tenant_id)
CREATE INDEX idx_session_user_tenant ON user_sessions(user_id, tenant_id)
CREATE INDEX idx_session_active ON user_sessions(active)
CREATE INDEX idx_session_last_access ON user_sessions(last_access_at)
```

⚠️ **IMPORTANTE**: UserSession usa `Instant` (instant in time), não `LocalDateTime`

---

### 1.4 RefreshToken (Token de Renovação JWT)
**Entity**: `com.leadflow.backend.entities.auth.RefreshToken`
**Table**: `refresh_tokens`

```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    device_fingerprint VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,     -- Mapeado de LocalDateTime
    revoked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL      -- Mapeado de LocalDateTime (CreationTimestamp)
)

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id)
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash)
CREATE INDEX idx_refresh_tokens_fingerprint ON refresh_tokens(device_fingerprint)
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at)
```

---

### 1.5 LoginAudit (Auditoria de Login)
**Entity**: `com.leadflow.backend.entities.auth.LoginAudit`
**Table**: `login_audit`

```sql
CREATE TABLE login_audit (
    id UUID PRIMARY KEY,
    user_id UUID,                      -- NULL (unknown user on failed login)
    tenant_id UUID NOT NULL,           -- 🔴 CRITICAL: Entity tem tenant_id!
    email VARCHAR(320) NOT NULL,       -- RFC 5321 max length
    ip_address VARCHAR(45),            -- NULL allowed (IPv6 compatible)
    user_agent VARCHAR(512),           -- NULL allowed
    success BOOLEAN NOT NULL,          -- Outcome: success or failure
    failure_reason VARCHAR(255),       -- NULL if success
    suspicious BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL      -- Mapeado de Instant
)

CREATE INDEX idx_login_user_tenant ON login_audit(user_id, tenant_id)
CREATE INDEX idx_login_email_tenant ON login_audit(email, tenant_id)
CREATE INDEX idx_login_created ON login_audit(created_at)
CREATE INDEX idx_login_success ON login_audit(success)
```

⚠️ **IMPORTANTE**: LoginAudit tem `tenant_id` e usa `Instant`, não `LocalDateTime`

---

### 1.6 SecurityAuditLog (Auditoria de Segurança)
**Entity**: `com.leadflow.backend.entities.audit.SecurityAuditLog`
**Table**: `security_audit_logs`

```sql
CREATE TABLE security_audit_logs (
    id UUID PRIMARY KEY,
    action VARCHAR(50) NOT NULL,       -- Enum: ERROR_PASSWORD, SUCCESSFUL_LOGIN, etc
    email VARCHAR(150) NOT NULL,       -- User email
    tenant VARCHAR(100) NOT NULL,      -- Tenant schema name
    success BOOLEAN NOT NULL,          -- Action outcome
    ip_address VARCHAR(100),           -- NULL allowed
    user_agent VARCHAR(255),           -- NULL allowed
    correlation_id VARCHAR(100),       -- Request tracking
    created_at TIMESTAMP NOT NULL      -- Mapeado de LocalDateTime
)

CREATE INDEX idx_audit_email ON security_audit_logs(email)
CREATE INDEX idx_audit_tenant ON security_audit_logs(tenant)
CREATE INDEX idx_audit_created_at ON security_audit_logs(created_at)
```

---

## 2. TIPOS DE DADOS JAVA ↔ POSTGRESQL

| Java Type | Hibernate Default | PostgreSQL | Test SQL |
|-----------|------------------|------------|----------|
| `LocalDateTime` | `TIMESTAMP` | TIMESTAMP WITHOUT TIMEZONE | `TIMESTAMP` |
| `Instant` | `TIMESTAMP WITH TIME ZONE` | TIMESTAMP WITH TIME ZONE | `TIMESTAMP` (compatible) |
| `UUID` | `UUID` | UUID | `UUID` |
| `boolean` | `BOOLEAN` | BOOLEAN | `BOOLEAN` |
| `Integer` | `INT` | INTEGER | `INTEGER` |
| `String` | `VARCHAR(n)` | VARCHAR(n) | `VARCHAR(n)` |

⚠️ **AVISO**: Não use `TIMESTAMPTZ` em testes simples - use `TIMESTAMP`

---

## 3. DEPENDÊNCIAS E ORDEM DE CRIAÇÃO

```
roles
    ↓
users (FK → roles.id)
    ↓
├─→ user_sessions (FK ← users.id)
├─→ refresh_tokens (FK → users.id)
└─→ login_audit (tenant_id, NO FK to users - email based)

security_audit_logs (independent - no FKs)
```

---

## 4. DADOS DE TESTE NECESSÁRIOS

### Setup Base
```sql
-- Insert Roles
INSERT INTO "schema".roles(id, name, created_at, updated_at)
VALUES (role_admin_uuid, 'ROLE_ADMIN', NOW(), NOW());

INSERT INTO "schema".roles(id, name, created_at, updated_at)
VALUES (role_user_uuid, 'ROLE_USER', NOW(), NOW());

-- Insert Users
INSERT INTO "schema".users(
    id, name, email, password, role_id,
    failed_attempts, credentials_updated_at, created_at, updated_at
)
VALUES (
    admin_uuid,
    'Admin Name',
    'admin@test.com',
    'encoded_password_hash',
    role_admin_uuid,
    0, NOW(), NOW(), NOW()
);
```

---

## 5. PROBLEMAS NO TESTE ATUAL

### ❌ Problema 1: LoginAudit sem `tenant_id`
```java
// ERRADO
INSERT INTO "schema".login_audit(id, user_id, email, success, ip_address, attempted_at)
VALUES(?, ?, ?, ?, ?, ?)

// CORRETO
INSERT INTO "schema".login_audit(id, user_id, tenant_id, email, success, created_at)
VALUES(?, ?, ?, ?, ?, ?)
```

### ❌ Problema 2: SecurityAuditLog com campos errados
```java
// ERRADO (campos genéricos que não existem)
INSERT INTO "schema".security_audit_logs(
    id, action, actor_id, resource_id, result, description, ip_address, created_at
)

// CORRETO (campos reais da entity)
INSERT INTO "schema".security_audit_logs(
    id, action, email, tenant, success, ip_address, user_agent, correlation_id, created_at
)
VALUES(?, 'ERROR_PASSWORD', ?, ?, ?, ?, ?, ?, NOW())
```

### ❌ Problema 3: RefreshToken sem foreign key
```java
// ERRADO (user_id não tem constraint)
// CORRETO
@JoinColumn(
    name = "user_id",
    nullable = false,
    foreignKey = @ForeignKey(name = "fk_refresh_tokens_user")
)
```

---

## 6. VERIFICAÇÃO DE SCHEMA NO BANCO

```sql
-- Para debugar durante testes:
\d "tenantadmintest".roles
\d "tenantadmintest".users
\d "tenantadmintest".user_sessions
\d "tenantadmintest".login_audit

-- Ver todas as tabelas de um schema
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'tenantadmintest'
```

