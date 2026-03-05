# Runbook Operacional — Go-Live Multitenant + Security

## Objetivo

Executar o go-live com rastreabilidade operacional, critérios claros de avanço e rollback para multitenancy + segurança.

## Papéis

- **Release Manager (RM):** coordena janela, checkpoints e decisão de avanço/rollback.
- **Backend Owner (BE):** executa validações técnicas e smoke tests de API.
- **SRE/DevOps (SRE):** deploy, health checks, observabilidade e rollback técnico.
- **Security Owner (SEC):** valida eventos de segurança e aprova critério final.

## Timeline operacional

## T-1 dia (preparação)

### Etapa 1 — Freeze e baseline
- **Responsável:** RM
- **Ação:** confirmar commit/tag de release e janela de mudança aprovada.
- **Saída:** release candidata identificada e congelada.

### Etapa 2 — Pré-validação técnica
- **Responsável:** BE
- **Ações:**
  - garantir `TenantFilter` precedendo autenticação;
  - confirmar uso exclusivo de `com.leadflow.backend.multitenancy.context.TenantContext`;
  - revisar migrações Flyway sem arquivos placeholder (`VXX__*`).
- **Saída:** checklist técnico pré-deploy assinado.

### Etapa 3 — Testes de staging
- **Responsável:** BE
- **Comandos:**
  - `mvn clean test "-Dtest=TenantFilterIntegrationTest,TenantIsolationTest"`
  - `mvn clean test "-Dtest=AuthControllerTest,AuthControllerNegativeTest,JwtServiceTest,UserSessionServiceTest"`
- **Saída:** `BUILD SUCCESS` em ambos os blocos.

## T-2h (janela de mudança)

### Etapa 4 — Go/No-Go
- **Responsável:** RM + SRE + BE + SEC
- **Ação:** reunião rápida (5–10 min) com verificação dos bloqueantes.
- **Saída:** decisão formal de **GO**.

### Etapa 5 — Backup e plano de retorno
- **Responsável:** SRE
- **Ação:** validar backup utilizável e comando/procedimento de rollback pronto.
- **Saída:** evidência de backup + instrução de rollback disponível.

## T0 (deploy)

### Etapa 6 — Aplicar migrações e subir versão
- **Responsável:** SRE
- **Ação:** deploy da release e aplicação das migrações da versão.
- **Saída:** aplicação no ar, sem falhas de startup.

### Etapa 7 — Verificação imediata (0–15 min)
- **Responsável:** SRE + BE
- **Ações:**
  - health/readiness OK;
  - ausência de erros de tenant/schema/JWT nos logs;
  - taxa de erro HTTP estável.
- **Saída:** sistema estável no período inicial.

## T+15 a T+120 min (estabilização)

### Etapa 8 — Smoke funcional multitenant
- **Responsável:** BE
- **Ações mínimas:**
  - login com tenant válido (2 tenants reais);
  - login com tenant inválido/ausente (erro esperado);
  - tentativa de acesso cruzado entre tenants (deve falhar).
- **Saída:** isolamento de tenant validado em runtime.

### Etapa 9 — Monitoramento de segurança
- **Responsável:** SEC + SRE
- **Métricas/eventos:**
  - picos de `401/403` por tenant;
  - falha de validação de sessão/JWT com mismatch de tenant;
  - alertas de autenticação anômala.
- **Saída:** sem incidentes críticos de segurança.

### Etapa 10 — Aprovação final da janela
- **Responsável:** RM + SEC
- **Ação:** registrar decisão de encerramento da mudança.
- **Saída:** go-live concluído.

## Critérios de rollback (acionamento imediato)

Acionar rollback se qualquer condição ocorrer:

1. acesso cross-tenant confirmado;
2. falha massiva de autenticação para tenants válidos;
3. erros sistêmicos de resolução de tenant/schema em produção.

## Procedimento resumido de rollback

1. **RM** declara rollback.
2. **SRE** reverte aplicação para release anterior.
3. **SRE** aplica retorno de banco conforme plano aprovado (quando necessário).
4. **BE/SEC** executam smoke mínimo pós-rollback.
5. **RM** registra incidente com horário, impacto e evidências.

## Evidências obrigatórias (anexar ao ticket)

- logs ou prints dos testes com `BUILD SUCCESS`;
- horário de cada checkpoint (T-2h, T0, T+15, T+120);
- evidência de smoke multitenant;
- decisão final de GO-LIVE ou ROLLBACK assinada por RM/SEC.
