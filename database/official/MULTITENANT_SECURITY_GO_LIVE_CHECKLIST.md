# Multitenant + Security Go-Live Checklist

## 1) Pré-deploy (bloqueante)

- [ ] **Tenant header obrigatório**: confirmar envio de `X-Tenant-ID` em todos os clients (web, jobs, integrações).
- [ ] **Filtro de tenant primeiro**: validar ordem do `TenantFilter` antes de autenticação JWT.
- [ ] **Contexto único de tenant**: garantir que só existe `com.leadflow.backend.multitenancy.context.TenantContext` no código.
- [ ] **Sessão atrelada ao tenant**: validar que sessão/JWT rejeitam mismatch de tenant.
- [ ] **Migrações prontas**: revisar scripts Flyway aplicáveis ao ambiente de produção (sem `VXX__*` pendente).
- [ ] **Variáveis sensíveis**: revisar segredos (JWT, DB, webhook, SMTP/SendGrid) no ambiente.

## 2) Build e validações em staging

- [ ] `mvn clean test "-Dtest=TenantFilterIntegrationTest,TenantIsolationTest"` com sucesso.
- [ ] `mvn clean test "-Dtest=AuthControllerTest,AuthControllerNegativeTest,JwtServiceTest,UserSessionServiceTest"` com sucesso.
- [ ] Smoke manual: login com tenant correto funciona; tenant ausente/errado retorna erro esperado.
- [ ] Smoke manual: usuário de tenant A não acessa dados do tenant B.

## 3) Janela de deploy

- [ ] Aplicar migrações com backup válido e plano de rollback documentado.
- [ ] Subir aplicação e monitorar logs por 10–15 min (auth failures, tenant resolution, DB schema errors).
- [ ] Confirmar healthchecks e readiness sem erro de autenticação/tenant.

## 4) Pós-deploy imediato (0–2h)

- [ ] Executar testes de fumaça de autenticação por tenant (2 tenants reais + 1 cenário inválido).
- [ ] Monitorar taxa de 401/403 e erros de sessão inválida por tenant.
- [ ] Monitorar consultas/latência para possíveis regressões de isolamento.
- [ ] Validar ausência de vazamento cross-tenant em endpoints críticos (lead, auth, user session).

## 5) Critérios de rollback

- [ ] Rollback imediato se houver:
  - [ ] acesso cross-tenant confirmado;
  - [ ] falha massiva de login por tenant correto;
  - [ ] erro sistêmico de resolução de schema/tenant em produção.
- [ ] Registrar incidente com tenant afetado, endpoint, horário e evidências de log.

## 6) Evidências mínimas para aprovação

- [ ] Execução de testes com `BUILD SUCCESS` anexada.
- [ ] Checklist preenchido por responsável técnico.
- [ ] Aprovação de segurança/arquitetura registrada.
