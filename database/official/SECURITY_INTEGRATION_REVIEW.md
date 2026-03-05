# Revisão de Integração: Segurança x Migração de Banco

## Resumo executivo
A migração oficial foi ajustada para aderir ao sistema de segurança já em produção no projeto. O foco foi evitar divergência entre entidades/repositórios de autenticação e o schema-alvo.

## O que foi integrado

### 1) Sessões por tenant (JWT session control)
- Sistema atual usa sessão ativa por `token_id + tenant_id`.
- Integração aplicada no schema:
  - tabela `user_sessions` com `tenant_id`, `token_id`, `active`, `suspicious`, `last_access_at`, `revoked_at`, `initial_ip_address`, `initial_user_agent`.
  - índices para lookup rápido de sessão ativa e limpeza.

### 2) Refresh token com device binding
- Sistema atual valida `device_fingerprint` para rotação e detecção de reuse.
- Integração aplicada no schema:
  - tabela `refresh_tokens` com `device_fingerprint` + `token_hash` único + `revoked` + `expires_at`.

### 3) Auditoria de segurança e login
- Sistema atual possui duas trilhas:
  - `security_audit_logs` (ações de segurança/correlação)
  - `login_audit` (sucesso/falha, suspeita, brute force)
- Integração aplicada no schema:
  - inclusão/adequação das duas tabelas e índices correspondentes.

### 4) Reset de senha
- Sistema atual usa `password_reset_token` (hash, expiração, usado).
- Integração aplicada no schema:
  - tabela `password_reset_token` com constraints e índices.

### 5) Auditoria de vendor
- Sistema atual usa `vendor_audit_logs` (não `audit_logs`).
- Integração aplicada no schema:
  - padronização do nome da tabela e campos usados no runtime (`acao`, `entidade_id`, `detalhes`).

### 6) Multi-tenancy estrutural
- Sistema atual depende da tabela `tenants` para resolver `tenant_id`.
- Integração aplicada no schema:
  - inclusão de `tenants` e FKs de dependência nas áreas de segurança.

## Lacunas ainda recomendadas (próxima fase)
1. Backfill planejado para ambientes com dados legados em tabelas com nomenclatura diferente.
2. Política de retenção: TTL operacional para `login_audit`, `security_audit_logs` e sessões revogadas.
3. Hardening adicional opcional:
   - particionamento temporal de logs (alto volume),
   - criptografia em repouso para campos sensíveis,
   - mascaramento de PII em relatórios de auditoria.

## Arquivos atualizados nesta revisão
- `database/official/flyway-clean/V1__core_identity_and_vendor.sql`
- `database/official/flyway-clean/V4__email_and_audit.sql`
- `database/official/flyway-clean/V5__security_sessions_and_tokens.sql`
- `database/official/flyway-clean/V6__indexes_and_performance.sql`
- `database/official/schema-official.sql`
- `database/official/ERD-official.mmd`
- `database/official/README.md`
