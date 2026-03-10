# 🎯 Próximos Passos - Fase 3 & 4

## 📊 O Que Foi Implementado (Fase 2) ✅

### Segurança do Webhook
- ✅ HMAC-SHA256 signature verification com constant-time comparison
- ✅ Timestamp validation (5 min tolerance) contra replay attacks
- ✅ Idempotency check (evita duplicatas)
- ✅ Event persistence para audit trail (90 dias)
- ✅ Custom exceptions com logging informativo
- ✅ Parse de Stripe-Signature header

### Configuração Centralizada
- ✅ StripeProperties.java com @ConfigurationProperties
- ✅ Suporte para retry (exponential backoff)
- ✅ Suporte para timeouts (connection + read)
- ✅ Suporte para event lifecycle management
- ✅ Validação automática na inicialização

### Event Processing
- ✅ StripeWebhookProcessor com handler registry
- ✅ 3 Event Handlers implementados:
  - InvoicePaymentSucceededHandler
  - SubscriptionDeletedHandler
  - SubscriptionUpdatedHandler
- ✅ Padrão extensível para novos handlers

---

## 🚀 Fase 3: Integração com Services (1-2 horas)

### O Que Fazer

Adicionar métodos em `SubscriptionService` que são chamados pelos handlers:

```java
// Método 1: Marcar pagamento como bem-sucedido
public void markPaymentSuccessful(String stripeSubscriptionId, String invoiceId) {
    Subscription subscription = subscriptionRepository
        .findByStripeSubscriptionId(stripeSubscriptionId)
        .orElseThrow(() -> new SubscriptionNotFoundException(stripeSubscriptionId));
    
    subscription.setLastPaymentDate(LocalDateTime.now());
    subscription.setPaymentStatus("PAID");
    subscriptionRepository.save(subscription);
    
    // Registrar auditoria
    recordAuditTrail(subscription, "PAYMENT_SUCCESSFUL", invoiceId);
}

// Método 2: Marcar como deletado do Stripe
public void markAsDeletedFromStripe(String stripeSubscriptionId) {
    Subscription subscription = subscriptionRepository
        .findByStripeSubscriptionId(stripeSubscriptionId)
        .orElseThrow(() -> new SubscriptionNotFoundException(stripeSubscriptionId));
    
    subscription.setStatus(SubscriptionStatus.CANCELLED);
    subscription.setCancelledAt(LocalDateTime.now());
    subscriptionRepository.save(subscription);
    
    // Registrar auditoria
    recordAuditTrail(subscription, "DELETED_FROM_STRIPE", "Stripe webhook notification");
}

// Método 3: Sincronizar com Stripe
public void syncWithStripe(Subscription subscription) {
    // Atualizar status local com dados do Stripe
    subscription.setStatus(SubscriptionStatus.fromString(subscription.getStatus()));
    subscription.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
    subscriptionRepository.save(subscription);
    
    // Registrar auditoria
    recordAuditTrail(subscription, "SYNCED_FROM_STRIPE", "Status updated from Stripe webhook");
}
```

### Depois

Uncomment os handlers para usar esses métodos:
```java
// Em InvoicePaymentSucceededHandler
subscriptionService.markPaymentSuccessful(stripeSubscriptionId, invoice.getId());

// Em SubscriptionDeletedHandler
subscriptionService.markAsDeletedFromStripe(subscription.getId());

// Em SubscriptionUpdatedHandler
subscriptionService.syncWithStripe(subscription);
```

**Time**: 30 minutos

---

## 🔧 Fase 4: Admin Endpoints (2 horas)

### Novos Endpoints para Administradores

```java
// GET - Listar todos os eventos webhook
@GetMapping("/admin/webhook-events")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Page<StripeEventLog>> getWebhookEvents(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size) {
    
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    Page<StripeEventLog> events = eventLogRepository.findAll(pageable);
    return ResponseEntity.ok(events);
}

// GET - Detalhe de um evento específico
@GetMapping("/admin/webhook-events/{eventId}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<StripeEventLog> getWebhookEvent(@PathVariable String eventId) {
    StripeEventLog event = eventLogRepository.findByEventId(eventId)
        .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
    return ResponseEntity.ok(event);
}

// PUT - Reprocessar evento
@PutMapping("/admin/webhook-events/{eventId}/retry")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Map<String, String>> retryWebhookEvent(@PathVariable String eventId) {
    StripeEventLog event = eventLogRepository.findByEventId(eventId)
        .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
    
    try {
        // Parse and reprocess the event
        Event stripeEvent = Event.GSON.fromJson(
            event.getPayload(), 
            Event.class
        );
        webhookProcessor.process(stripeEvent);
        
        event.setStatus(StripeEventLog.EventProcessingStatus.SUCCESS);
        event.setProcessedAt(LocalDateTime.now());
        eventLogRepository.save(event);
        
        return ResponseEntity.ok(Map.of(
            "status", "retry_successful",
            "eventId", eventId
        ));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of(
            "status", "retry_failed",
            "error", e.getMessage()
        ));
    }
}

// GET - Estatísticas de webhooks
@GetMapping("/admin/webhook-stats")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Map<String, Object>> getWebhookStats() {
    return ResponseEntity.ok(Map.of(
        "total_events", eventLogRepository.count(),
        "successful", eventLogRepository.countByStatus(StripeEventLog.EventProcessingStatus.SUCCESS),
        "failed", eventLogRepository.countByStatus(StripeEventLog.EventProcessingStatus.FAILED),
        "pending", eventLogRepository.countByStatus(StripeEventLog.EventProcessingStatus.PENDING)
    ));
}
```

**Time**: 2 horas (including testes)

---

## 🔄 Fase 5: Async Processing (Opcional, 4 horas)

Se quiser processar eventos em background com retry automático:

```java
@Component
@Slf4j
public class WebhookEventProcessor {
    
    @Scheduled(fixedDelay = 5000)  // A cada 5 segundos
    public void processFailedEvents() {
        // Buscar eventos PENDING ou RETRY_PENDING
        List<StripeEventLog> pendingEvents = eventLogRepository
            .findByStatusesWithoutType(List.of(
                StripeEventLog.EventProcessingStatus.PENDING,
                StripeEventLog.EventProcessingStatus.RETRY_PENDING
            ));
        
        for (StripeEventLog log : pendingEvents) {
            try {
                Event event = Event.GSON.fromJson(log.getPayload(), Event.class);
                webhookProcessor.process(event);
                
                log.setStatus(StripeEventLog.EventProcessingStatus.SUCCESS);
                log.setProcessedAt(LocalDateTime.now());
                eventLogRepository.save(log);
                
            } catch (Exception e) {
                handleRetry(log, e);
            }
        }
    }
    
    private void handleRetry(StripeEventLog log, Exception e) {
        log.setRetryCount(log.getRetryCount() + 1);
        log.setLastError(e.getMessage());
        
        if (log.getRetryCount() >= log.getMaxRetries()) {
            log.setStatus(StripeEventLog.EventProcessingStatus.FAILED);
        } else {
            log.setStatus(StripeEventLog.EventProcessingStatus.RETRY_PENDING);
            long delay = (long) (1000 * Math.pow(2, log.getRetryCount() - 1));
            log.setNextRetryAt(LocalDateTime.now().plus(Duration.ofMillis(delay)));
        }
        
        eventLogRepository.save(log);
    }
}
```

**Time**: 4 horas

---

## 📧 Fase 6: Email Notifications (2 horas)

Enviar emails quando eventos importantes ocorrem:

```java
@Component
@Slf4j
public class SubscriptionEmailNotificationService {
    
    private final EmailService emailService;
    private final SubscriptionRepository subscriptionRepository;
    
    public void notifyPaymentSuccessful(String stripeSubscriptionId, Invoice invoice) {
        Subscription subscription = subscriptionRepository
            .findByStripeSubscriptionId(stripeSubscriptionId)
            .orElse(null);
        
        if (subscription != null && subscription.getContactEmail() != null) {
            emailService.send(
                subscription.getContactEmail(),
                "Payment Received",
                "payment-received.html",
                Map.of("invoice", invoice)
            );
        }
    }
    
    public void notifySubscriptionDeleted(String stripeSubscriptionId) {
        Subscription subscription = subscriptionRepository
            .findByStripeSubscriptionId(stripeSubscriptionId)
            .orElse(null);
        
        if (subscription != null && subscription.getContactEmail() != null) {
            emailService.send(
                subscription.getContactEmail(),
                "Subscription Cancelled",
                "subscription-cancelled.html",
                Map.of("subscription", subscription)
            );
        }
    }
}
```

Depois, chamar de dentro dos handlers:
```java
@Override
public void handle(Event event) throws Exception {
    Invoice invoice = (Invoice) event.getDataObjectDeserializer()...;
    new SubscriptionEmailNotificationService()
        .notifyPaymentSuccessful(invoice.getSubscription(), invoice);
}
```

**Time**: 2 horas

---

## 🗺️ Roadmap Recomendado

### Próxima Hora
- [ ] Implementar Phase 3 (Service Integration) - 30 min
- [ ] Testar com Stripe CLI

### Próximas 3 Horas
- [ ] Implementar Phase 4 (Admin Endpoints) - 2 horas
- [ ] Criar testes unitários - 1 hora

### Próximas 6 Horas
- [ ] Deploy em staging
- [ ] Testar com test keys do Stripe
- [ ] Implementar Phase 5 (Async) - 4 horas (opcional)

### Próximas 10 Horas
- [ ] Implementar Phase 6 (Email)  - 2 horas
- [ ] Final testing e deploy - 2 horas
- [ ] Documentação final - 1 hora

---

## 🧪 Teste com Stripe CLI

```bash
# Terminal 1: Forward webhooks
stripe listen --forward-to localhost:8081/api/stripe/webhook

# Copiar STRIPE_WEBHOOK_SECRET

# Terminal 2: Iniciar app
mvn spring-boot:run

# Terminal 3: Enviar eventos de teste
stripe trigger invoice.payment_succeeded
stripe trigger customer.subscription.deleted
stripe trigger customer.subscription.updated

# Verificar logs e banco de dados
SELECT * FROM stripe_event_logs ORDER BY created_at DESC;
```

---

## 📝 Checklist de Implementação

### Phase 3 (Hoje)
- [ ] Adicionar method `markPaymentSuccessful` em SubscriptionService
- [ ] Adicionar method `markAsDeletedFromStripe` em SubscriptionService
- [ ] Adicionar method `syncWithStripe` em SubscriptionService
- [ ] Uncomment chamadas nos handlers
- [ ] Testar com Stripe CLI
- [ ] Commit & Push

### Phase 4 (Amanhã)
- [ ] Criar admin endpoints no BillingAdminController
- [ ] Implementar paginação e filtros
- [ ] Criar testes unitários
- [ ] Testar endpoints com Postman
- [ ] Commit & Push

### Phase 5 (Opcional)
- [ ] Criar WebhookEventProcessor com @Scheduled
- [ ] Implementar retry automático
- [ ] Implementar circuit breaker
- [ ] Testar com simulação de falhas
- [ ] Commit & Push

---

## 🎓 Como Começar Phase 3 Agora?

Se quiser, posso implementar tudo em ~10 minutos:

### Opção A: Eu faço (recomendado)
```
Digite: "Implementa a Fase 3 agora"
Tempo: ~10 minutos
Resultado: Phase 3 + Phase 4 implementadas
```

### Opção B: Você faz
```
1. Abrar SubscriptionService.java
2. Copiar os 3 métodos do guia acima
3. Descomentar chamadas nos handlers
4. mvn clean compile -DskipTests
5. Testar com Stripe CLI
```

Qual você prefere? 👇
