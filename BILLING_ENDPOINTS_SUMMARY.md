# 📋 Endpoints Administrativos de Billing - IMPLEMENTADO

## Status: ✅ COMPLETO

Todos os três endpoints administrativos foram implementados no `BillingDashboardController.java`:

---

## 1️⃣ GET /api/v1/billing/subscription

**Descrição**: Retorna status da assinatura do tenant autenticado

**Autenticação**: `@PreAuthorize("isAuthenticated()")`

**Exemplo de Requisição**:
```bash
GET /api/v1/billing/subscription
Authorization: Bearer {jwt_token}
```

**Exemplo de Resposta (200 OK)**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "tenantId": "550e8400-e29b-41d4-a716-446655440001",
  "status": "ACTIVE",
  "plan": {
    "name": "Leadflow Standard",
    "maxLeads": 500,
    "maxUsers": 10,
    "maxAiExecutions": 1000
  },
  "stripeCustomerId": "cus_1a2b3c4d",
  "stripeSubscriptionId": "sub_1a2b3c4d",
  "startedAt": "2026-03-09T18:00:00",
  "expiresAt": "2026-04-09T18:00:00",
  "createdAt": "2026-03-09T18:00:00",
  "updatedAt": "2026-03-09T18:00:00"
}
```

**Código Implementado**:
```java
@GetMapping("/subscription")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<SubscriptionDetailsDTO> getMySubscription() {
    UUID tenantId = vendorContext.getCurrentVendorId();
    log.info("Fetching subscription for authenticated tenant: {}", tenantId);
    SubscriptionDetailsDTO details = billingDashboardService.getSubscriptionDetails(tenantId);
    return ResponseEntity.ok(details);
}
```

---

## 2️⃣ GET /api/v1/billing/usage

**Descrição**: Retorna consumo atual do plano para o tenant autenticado

**Autenticação**: `@PreAuthorize("isAuthenticated()")`

**Exemplo de Requisição**:
```bash
GET /api/v1/billing/usage
Authorization: Bearer {jwt_token}
```

**Exemplo de Resposta (200 OK)**:
```json
{
  "leadsUsed": 45,
  "leadsLimit": 500,
  "usersUsed": 2,
  "usersLimit": 10,
  "aiExecutionsUsed": 120,
  "aiExecutionsLimit": 1000,
  "percentageUsed": {
    "leads": 9,
    "users": 20,
    "aiExecutions": 12
  },
  "periodEnd": "2026-04-09T18:00:00"
}
```

**Código Implementado**:
```java
@GetMapping("/usage")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<BillingDashboardDTO.UsageStatisticsDTO> getMyUsage() {
    UUID tenantId = vendorContext.getCurrentVendorId();
    log.info("Fetching usage statistics for authenticated tenant: {}", tenantId);
    BillingDashboardDTO.UsageStatisticsDTO usage = billingDashboardService.getUsageStatistics(tenantId);
    return ResponseEntity.ok(usage);
}
```

---

## 3️⃣ POST /api/v1/billing/cancel

**Descrição**: Cancela a assinatura do tenant autenticado

**Autenticação**: `@PreAuthorize("isAuthenticated()")`

**Exemplo de Requisição**:
```bash
POST /api/v1/billing/cancel
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Exemplo de Resposta (200 OK)**:
```json
{
  "status": "subscription_cancelled",
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-03-09T18:08:30"
}
```

**Exemplo de Resposta (400 Bad Request)**:
```json
{
  "error": "cancellation_failed",
  "message": "Stripe cancellation failed: Invalid subscription ID"
}
```

**Código Implementado**:
```java
@PostMapping("/cancel")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<?> cancelSubscription() {
    UUID tenantId = vendorContext.getCurrentVendorId();
    log.warn("Cancelling subscription for tenant: {}", tenantId);
    
    try {
        subscriptionService.cancelSubscription(tenantId);
        
        return ResponseEntity.ok(Map.of(
            "status", "subscription_cancelled",
            "tenantId", tenantId.toString(),
            "timestamp", java.time.LocalDateTime.now()
        ));
    } catch (Exception e) {
        log.error("Failed to cancel subscription for tenant: {}", tenantId, e);
        return ResponseEntity.badRequest().body(Map.of(
            "error", "cancellation_failed",
            "message", e.getMessage()
        ));
    }
}
```

---

## Fluxo Completo de Uso

### 1. Frontend consulta status da assinatura
```
GET /api/v1/billing/subscription
↓
BillingValidationInterceptor valida subscription
↓
BillingDashboardController.getMySubscription()
↓
BillingDashboardService.getSubscriptionDetails()
↓
SubscriptionRepository.findByTenantId()
↓
HTTP 200 com SubscriptionDetailsDTO
```

### 2. Frontend consulta consumo
```
GET /api/v1/billing/usage
↓
BillingValidationInterceptor valida subscription
↓
BillingDashboardController.getMyUsage()
↓
BillingDashboardService.getUsageStatistics()
↓
VendorUsageRepository + PlanService
↓
HTTP 200 com UsageStatisticsDTO
```

### 3. Frontend inicia cancelamento
```
POST /api/v1/billing/cancel
↓
BillingValidationInterceptor valida subscription
↓
BillingDashboardController.cancelSubscription()
↓
SubscriptionService.cancelSubscription()
  ├─ Chama Stripe API para cancelar
  ├─ Atualiza status local para CANCELLED
  └─ Registra auditoria
↓
HTTP 200 com confirmação
```

---

## Tratamento de Erros

### HTTP 402 - Subscription Inactive
Se a subscription não estiver ACTIVE:
```json
{
  "error": "SUBSCRIPTION_INACTIVE",
  "message": "Subscription is CANCELLED. Expires at: null",
  "timestamp": "2026-03-09T18:08:30",
  "status": 402
}
```

### HTTP 401 - Not Authenticated
Se token JWT inválido/expirado:
```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired token"
}
```

### HTTP 400 - Cancellation Failed
Se falha ao cancelar no Stripe:
```json
{
  "error": "cancellation_failed",
  "message": "Stripe cancellation failed: Connection timeout"
}
```

---

## Verificação de Segurança

✅ **Autenticação JWT**: Todos os endpoints requerem `@PreAuthorize("isAuthenticated()")`
✅ **Isolamento de Tenant**: VendorContext garante que usuário só vê dados da sua subscription
✅ **Validação de Subscription**: BillingValidationInterceptor bloqueia acesso se subscription inativa
✅ **Auditoria**: Todos os cambios registrados em SubscriptionAudit
✅ **Logging**: Todos os eventos logados para debugging
✅ **Error Handling**: BillingExceptionHandler trata exceções gracefully

---

## Próximas Implementações

1. **Validação Segura do Webhook Stripe**
   - Verificar assinatura HMAC do webhook
   - Validar timestamp do evento
   - Implementar retry com idempotência
   - Registrar todos os eventos recebidos

2. **Configuração Centralizada das Variáveis Stripe**
   - Variáveis de ambiente para chaves
   - Configuração de timeout e retry
   - Modo test vs production
   - Logging de transações

3. **Endpoints Administrativos Avançados**
   - Suspender subscription (força)
   - Estender subscription (renovação manual)
   - Ver todas as subscriptions (admin)
   - Ver eventos falhados (admin)
