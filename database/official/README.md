# Database Official Blueprint (Leadflow)

Este diretório consolida o schema oficial recomendado para produção.

## Conteúdo
- `ERD-official.mmd`: diagrama ER conceitual por domínio.
- `schema-official.sql`: DDL completa normalizada (PostgreSQL).
- `flyway-clean/`: baseline limpa, separada em fases (`V1..V6`).

## Objetivo
- Normalização e consistência por domínio.
- Índices para queries críticas.
- FKs explícitas para integridade.
- Padronização de timestamps (`created_at`, `updated_at`, `deleted_at`).
- Base pronta para rollout com Flyway sem acoplar no runtime atual.

## Query paths críticos cobertos
1. Leads por vendor (`vendor_id`, `created_at`, `stage`, `score`).
2. Update de stage (FK + índice em `vendor_lead_id`).
3. Contagem por estágio (`vendor_id, stage`).
4. Verificação de quota (`vendor_id, quota_type, period_start`).
5. Vendors ativos (`subscription_status`).
6. Soma de uso de IA (`vendor_id, quota_type, period_start`).

## Integração com segurança (revisão aplicada)
- `user_sessions` aderente ao runtime atual: `tenant_id`, `token_id`, `active`, `suspicious`, `last_access_at`, `revoked_at`.
- `refresh_tokens` com `device_fingerprint` para device binding e rotação segura.
- `security_audit_logs` compatível com `SecurityAuditService` (`action`, `email`, `tenant`, `success`, `correlation_id`).
- `login_audit` adicionado para trilha de autenticação e detecção de brute force por janela.
- `password_reset_token` adicionado para fluxo de reset de senha.
- `vendor_audit_logs` padronizado com os nomes reais usados na aplicação.
- `tenants` incluído para multi-tenancy e FKs de sessão/auditoria.

## Observação de rollout
Os scripts em `flyway-clean/` são baseline oficial e **não** estão ligados ao classpath Flyway atual da aplicação.
Para ativar, criar uma estratégia de cutover (ambiente novo ou migração controlada com backup e validação).
