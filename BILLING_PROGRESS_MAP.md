# 🗺️ Mapa de Progresso - Sistema de Billing LeadFlow

## Status Geral: ✅ FASE 1 COMPLETA | 🔄 FASE 2 PRONTA PARA IMPLEMENTAÇÃO

---

## 📊 Progresso Visual

```
FASE 1: Camadas de Validação
├── ✅ Exception Layer (SubscriptionInactiveException)
├── ✅ Interceptor Layer (BillingValidationInterceptor)
├── ✅ Service Layer (SubscriptionService)
│   ├── validateActiveSubscription()
│   ├── getSubscriptionByTenant()
│   └── cancelSubscription()
├── ✅ Controller Layer (BillingDashboardController)
│   ├── GET /billing/subscription
│   ├── GET /billing/usage
│   └── POST /billing/cancel
├── ✅ Entity Layer (Subscription, Plan)
│   └── Plan Limits: 500 leads, 10 users, 1000 AI executions
└── ✅ Configuration Layer (application.yml)

FASE 2: Webhook & Stripe Config
├── 🔄 Webhook Validation Security
│   ├── ⏳ StripeWebhookValidator (HMAC-SHA256)
│   ├── ⏳ StripeWebhookProcessor (Event Router)
│   ├── ⏳ Event Handlers (Invoice, Subscription)
│   └── ⏳ StripeEventRepository (Auditoria)
├── 🔄 Centralized Stripe Configuration
│   ├── ⏳ StripeProperties (@ConfigurationProperties)
│   ├── ⏳ .env.example (Variáveis de Ambiente)
│   └── ⏳ StripeConfigValidator (Startup Validation)
└── ⏳ Testing & Documentation

FASE 3: Extensões Administrativas
├── ⏳ Suspend Subscription (Force)
├── ⏳ Extend Subscription (Manual Renewal)
├── ⏳ View All Subscriptions (Admin)
├── ⏳ View Failed Events (Admin)
└── ⏳ Generate Billing Reports
```

---

## 🎯 Cronograma Recomendado

### Semana 1: Webhook Validation Security
- **Segunda**: Criar StripeWebhookValidator com HMAC e timestamp validation
- **Terça-Quarta**: Criar StripeWebhookProcessor e event handlers
- **Quinta**: CREATE StripeEventRepository e auditoria
- **Sexta**: Testar com Stripe CLI em development

### Semana 2: Centralized Configuration
- **Segunda**: Criar StripeProperties com @ConfigurationProperties
- **Terça**: Criar StripeConfigValidator para startup validation
- **Quarta**: Atualizar application.yml e criar .env.example
- **Quinta-Sexta**: Testar em staging (test mode)

### Semana 3: Testing & Production
- **Segunda-Quarta**: Implementar testes unitários e integração
- **Quinta**: Deploy em produção com live keys
- **Sexta**: Monitoramento e ajustes

---

## 📦 Arquivos a Criar/Modificar

### FASE 2A: Webhook Validation

| Arquivo | Tipo | Descrição | Prioridade |
|---------|------|-----------|-----------|
| `StripeWebhookValidator.java` | ✨ NEW | Validar HMAC-SHA256 e timestamp | HIGH |
| `StripeWebhookProcessor.java` | ✨ NEW | Router de eventos Stripe | HIGH |
| `StripeEventHandler.java` | ✨ NEW | Interface base para handlers | HIGH |
| `InvoicePaymentSucceededHandler.java` | ✨ NEW | Handle invoice.payment_succeeded | MEDIUM |
| `SubscriptionDeletedHandler.java` | ✨ NEW | Handle customer.subscription.deleted | MEDIUM |
| `SubscriptionUpdatedHandler.java` | ✨ NEW | Handle customer.subscription.updated | MEDIUM |
| `StripeWebhookEvent.java` | ✨ NEW | Entity para auditoria de webhooks | MEDIUM |
| `StripeEventRepository.java` | ✨ NEW | Repository para webhooks | MEDIUM |
| `StripeSignatureVerificationException.java` | ✨ NEW | Custom exception | LOW |
| `StripeTimestampExpiredException.java` | ✨ NEW | Custom exception | LOW |
| `StripeWebhookController.java` | 🔧 MODIFY | Integrar validador e processor | HIGH |

**Total**: 10 arquivos novos + 1 modificação

### FASE 2B: Centralized Configuration

| Arquivo | Tipo | Descrição | Prioridade |
|---------|------|-----------|-----------|
| `StripeProperties.java` | ✨ NEW | @ConfigurationProperties | HIGH |
| `StripeConfigValidator.java` | ✨ NEW | Validar config no startup | HIGH |
| `application.yml` | 🔧 MODIFY | Adicionar section stripe completo | HIGH |
| `.env.example` | ✨ NEW | Template de variáveis de ambiente | MEDIUM |
| `pom.xml` | 🔧 MODIFY | Adicionar deps se necessário | LOW |

**Total**: 3 arquivos novos + 2 modificações

---

## 🧪 Testes Necessários

### Webhook Validation Tests

```java
// StripeWebhookValidatorTest.java
✓ testValidSignature_Success()
✓ testValidSignature_InvalidHash()
✓ testValidSignature_InvalidTimestamp()
✓ testValidTimestamp_Current()
✓ testValidTimestamp_Expired()
✓ testValidTimestamp_FutureProtection()
✓ testHmacComputation_Correctness()
```

### Webhook Processor Tests

```java
// StripeWebhookProcessorTest.java
✓ testProcessEventWithValidHandler()
✓ testProcessEventWithoutHandler()
✓ testProcessEventFailureHandling()
✓ testIdempotentProcessing()
✓ testEventSerialization()
```

### Configuration Tests

```java
// StripePropertiesTest.java
✓ testLoadFromEnvVariables()
✓ testLoadFromApplicationYml()
✓ testValidateRequiredFields()
✓ testInitializeStripeSDK()
✓ testRetryCalculation()
```

### Integration Tests

```java
// StripeWebhookIntegrationTest.java
✓ testEndToEndWebhookProcessing()
✓ testStripeCliSimulation()
✓ testEventRetryBehavior()
✓ testConcurrentWebhookHandling()
```

---

## 💻 Ambiente de Desenvolvimento

### Setup Local

```bash
# 1. Clone do repositório
cd leadflow-backend

# 2. Copiar .env.example para .env
cp .env.example .env

# 3. Configurar Stripe keys de teste
# Editar .env com suas chaves de teste (sk_test_...)

# 4. Instalar Stripe CLI
# macOS: brew install stripe/cli-v2/cli
# Linux: wget https://github.com/stripe/stripe-cli/releases/download/v1.x.x/stripe_linux_x86_64.tar.gz
# Windows: Usar chocolatey ou baixar direto

# 5. Logar no Stripe CLI
stripe login

# 6. Escutar webhooks locais
stripe listen --forward-to localhost:8080/stripe/webhook

# 7. Em outro terminal, trigger eventos de teste
stripe trigger customer.subscription.created
stripe trigger invoice.payment_succeeded

# 8. Verificar logs da aplicação
tail -f logs/stripe-webhooks.log
```

### Verificar Configuração

```bash
# Comands para verificar setup
mvn clean compile -DskipTests
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"

# Em outro terminal
curl -X GET http://localhost:8080/api/v1/billing/subscription \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Se retornar 402, validação do webhook está funcionando
```

---

## 🔍 Checklist de Verificação

### Antes de Implementar

- [ ] Ler documentação Stripe: https://stripe.com/docs/webhooks/securing
- [ ] Ler documentação Stripe Events: https://stripe.com/docs/api/events
- [ ] Entender HMAC-SHA256: https://en.wikipedia.org/wiki/HMAC
- [ ] Configurar ambiente de teste no Stripe Dashboard
- [ ] Gerar webhooks secret em https://dashboard.stripe.com/webhooks

### Durante Implementação

- [ ] Validar assinatura HMAC em tempo constante (não usar ==)
- [ ] Usar UUID.randomUUID() para idempotency keys
- [ ] Registrar todos os webhooks em StripeEventRepository
- [ ] Implementar retry com exponential backoff
- [ ] Adicionar logging abrangente
- [ ] Não expor secrets em logs ou stack traces
- [ ] Usar transações para consistência

### Após Implementação

- [ ] Testar com Stripe CLI
- [ ] Testar replay de eventos (enviando 2x)
- [ ] Testar webhook expirado (>5 min)
- [ ] Testar assinatura inválida
- [ ] Testar handlers de erro
- [ ] Testar em staging com test keys
- [ ] Gerar relatório de cobertura de testes (>85%)
- [ ] Documentar endpoints internos no README
- [ ] Configurar monitoramento em produção

---

## 📚 Documentação Stripe Essencial

| Tópico | Link | Prioridade |
|--------|------|-----------|
| Webhook Security | https://stripe.com/docs/webhooks/securing | CRITICAL |
| API Events | https://stripe.com/docs/api/events | HIGH |
| Event Types | https://stripe.com/docs/api/event_types | HIGH |
| Stripe CLI | https://stripe.com/docs/stripe-cli | HIGH |
| Error Handling | https://stripe.com/docs/error-handling | MEDIUM |
| API Idempotency | https://stripe.com/docs/api/idempotent_requests | MEDIUM |
| Pagination | https://stripe.com/docs/api/pagination | LOW |

---

## 🎓 Referências de Código

### HMAC-SHA256 Pattern (Java)

```java
private String computeHmacSha256(String data, String secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
    mac.init(keySpec);
    byte[] rawHmac = mac.doFinal(data.getBytes());
    return bytesToHex(rawHmac);
}

// Comparação em tempo constante
MessageDigest.isEqual(expected.getBytes(), computed.getBytes());
```

### Exponential Backoff Pattern

```java
for (int attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
        return execute();
    } catch (StripeException e) {
        if (attempt < maxAttempts) {
            long delay = (long) (initialDelay * Math.pow(multiplier, attempt - 1));
            Thread.sleep(delay);
        } else {
            throw e;
        }
    }
}
```

### Event Router Pattern

```java
@Component
public class EventRouter {
    @Resource(name = "handlers")
    private Map<String, EventHandler> handlers;
    
    public void route(Event event) {
        EventHandler handler = handlers.get(event.getType());
        if (handler != null) {
            handler.handle(event);
        }
    }
}
```

---

## 📞 Suporte

Se encontrar problemas:

1. **Webhook não recebido**: Verificar se Stripe CLI está rodando
2. **Assinatura inválida**: Verificar se secret está correto
3. **Timeout**: Aumentar `stripe.timeout.read-ms`
4. **Rate limiting**: Implementar circuit breaker
5. **Eventos duplicados**: Verificar idempotência no handler

---

## 🚀 Próximas Etapas

Após completar Fase 2, priorizar em ordem:

1. **Admin Endpoints** (Semana 4)
   - Suspender subscription
   - Estender trial
   - Visualizar todas as subscriptions
   - Visualizar eventos falhados

2. **Email Notifications** (Semana 5)
   - Spring Boot Mail integration
   - Templates para eventos de billing
   - Notificações de vencimento próximo

3. **Usage Analytics** (Semana 6)
   - Dashboard de uso por tenant
   - Alertas de quota
   - Relatórios exportáveis

4. **Plan Upgrade/Downgrade** (Semana 7)
   - Change plan flow
   - Prorated billing
   - Confirmação do usuário

---

## 📝 Notas Importantes

⚠️ **SEGURANÇA**: Nunca commitar variáveis Stripe em git. Usar sempre variáveis de ambiente!

⚠️ **PRODUÇÃO**: Trocar sk_test_* por sk_live_* apenas após testar completamente em staging.

⚠️ **IDEMPOTÊNCIA**: Sempre verificar se evento já foi processado antes de atualizar estado.

⚠️ **LOGGING**: Não logar valores de chaves, tokens, ou dados sensíveis.

✅ **BEST PRACTICE**: Sempre usar try-catch em handlersde webhook e retornar HTTP 200.

✅ **BEST PRACTICE**: Registrar todos os webhooks em BD para auditoria e replay.

✅ **BEST PRACTICE**: Implementar circuit breaker para Stripe API para evitar cascata de erros.
