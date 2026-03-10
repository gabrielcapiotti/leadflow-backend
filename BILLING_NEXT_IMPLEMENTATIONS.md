# 🔐 Próximas Implementações Estruturais do Billing

## Fase 2: Validação Segura do Webhook Stripe + Configuração Centralizada

---

# 1. VALIDAÇÃO SEGURA DO WEBHOOK STRIPE

## 📋 Requisitos

Implementar validação robusta dos webhooks Stripe com:
- ✅ Verificação de assinatura HMAC-SHA256
- ✅ Validação de timestamp (evitar replay attacks)
- ✅ Tratamento de eventos com idempotência
- ✅ Logging completo de todos os eventos
- ✅ Retry automático com exponential backoff
- ✅ Armazenamento de eventos para auditoria

---

## 🏗️ Arquitetura Proposta

```
Stripe Webhook (POST)
    ↓
[StripeWebhookController]
    ├─ Validar assinatura HMAC
    ├─ Validar timestamp (±5 min)
    └─ Validar evento duplo (idempotência)
    ↓
[StripeWebhookProcessor]
    ├─ Deserializar evento
    ├─ Router por tipo de evento
    └─ Executar handler específico
    ↓
[Handlers Específicos]
    ├─ InvoicePaymentSucceededHandler
    ├─ InvoicePaymentFailedHandler
    ├─ SubscriptionDeletedHandler
    └─ SubscriptionUpdatedHandler
    ↓
[StripeEventRepository]
    └─ Registra evento para auditoria/replay
```

---

## 🔑 Variáveis de Configuração Necessárias

```yaml
# application.yml
stripe:
  api:
    secret-key: ${STRIPE_SECRET_KEY}          # sk_live_*** ou sk_test_***
    publishable-key: ${STRIPE_PUBLISHABLE_KEY}  # pk_live_*** ou pk_test_***
  webhook:
    secret: ${STRIPE_WEBHOOK_SECRET}          # whsec_*** para endpoint
    path: /stripe/webhook                      # Rota do webhook
    enabled: true
  retry:
    max-attempts: 3
    initial-delay-ms: 1000                    # 1 segundo
    multiplier: 2                             # exponential backoff
  timeout:
    connection-ms: 10000                      # 10 segundos
    read-ms: 30000                            # 30 segundos
  events:
    enabled: true
    max-age-days: 90                          # Manter por 90 dias
```

---

## 🛡️ Implementação do Webhook Validation

### Step 1: StripeWebhookValidator.java

```java
@Component
@Slf4j
public class StripeWebhookValidator {
    
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;
    
    @Value("${stripe.webhook.timestamp-tolerance-seconds:300}")
    private long timestampTolerance;
    
    /**
     * Valida a assinatura HMAC-SHA256 do webhook Stripe
     */
    public void validateSignature(String payload, String signature, String timestamp) 
            throws StripeSignatureVerificationException {
        
        try {
            // Construir signed content: "{timestamp}.{payload}"
            String signedContent = timestamp + "." + payload;
            
            // Computar HMAC-SHA256
            String computedSignature = computeHmacSha256(signedContent, webhookSecret);
            
            // Comparar assinaturas em tempo constante
            if (!computedSignature.equals(signature)) {
                log.warn("Invalid webhook signature. Expected: {}, Got: {}", 
                    computedSignature, signature);
                throw new StripeSignatureVerificationException("Webhook signature verification failed");
            }
            
            log.debug("Webhook signature verification successful");
            
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to compute HMAC-SHA256", e);
            throw new StripeSignatureVerificationException("Signature computation failed");
        }
    }
    
    /**
     * Valida o timestamp do webhook (evitar replay attacks)
     */
    public void validateTimestamp(String timestamp) throws StripeTimestampExpiredException {
        try {
            long webhookTimestamp = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis() / 1000; // em segundos
            long timeDifference = Math.abs(currentTime - webhookTimestamp);
            
            if (timeDifference > timestampTolerance) {
                log.warn("Webhook timestamp expired. Tolerance: {}s, Difference: {}s",
                    timestampTolerance, timeDifference);
                throw new StripeTimestampExpiredException("Webhook timestamp is too old");
            }
            
            log.debug("Webhook timestamp validation successful");
            
        } catch (NumberFormatException e) {
            log.error("Invalid timestamp format: {}", timestamp);
            throw new StripeTimestampExpiredException("Invalid timestamp format");
        }
    }
    
    private String computeHmacSha256(String data, String secret) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        mac.init(keySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes());
        
        // Converter para hexadecimal
        return bytesToHex(rawHmac);
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
```

### Step 2: StripeWebhookController.java (Modificado)

```java
@RestController
@RequestMapping("/stripe/webhook")
@Slf4j
public class StripeWebhookController {
    
    private final StripeWebhookValidator webhookValidator;
    private final StripeWebhookProcessor webhookProcessor;
    private final StripeEventRepository eventRepository;
    
    @PostMapping
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        
        try {
            log.info("Received Stripe webhook");
            
            // Parse Stripe signature header format: "t={timestamp},v1={hash}"
            String timestamp = parseTimestamp(signature);
            String signatureHash = parseSignatureHash(signature, "v1");
            
            // 1. Validar assinatura HMAC
            webhookValidator.validateSignature(payload, signatureHash, timestamp);
            log.debug("Webhook signature validated");
            
            // 2. Validar timestamp (evitar replay)
            webhookValidator.validateTimestamp(timestamp);
            log.debug("Webhook timestamp validated");
            
            // 3. Deserializar evento
            Event event = Event.GSON.fromJson(payload, Event.class);
            log.info("Webhook event deserialized: type={}, id={}", 
                event.getType(), event.getId());
            
            // 4. Verificar idempotência (evento já processado?)
            if (isEventAlreadyProcessed(event.getId())) {
                log.warn("Duplicate webhook event: {}", event.getId());
                return ResponseEntity.ok().build(); // Retornar 200 mesmo assim
            }
            
            // 5. Processar evento
            webhookProcessor.process(event);
            log.info("Webhook event processed successfully: id={}", event.getId());
            
            // 6. Registrar evento processado
            recordWebhookEvent(event, true, null);
            
            return ResponseEntity.ok().build();
            
        } catch (StripeSignatureVerificationException e) {
            log.error("Webhook signature verification failed", e);
            recordWebhookEvent(null, false, e.getMessage());
            return ResponseEntity.status(401).build(); // Unauthorized
            
        } catch (StripeTimestampExpiredException e) {
            log.error("Webhook timestamp validation failed", e);
            recordWebhookEvent(null, false, e.getMessage());
            return ResponseEntity.status(401).build();
            
        } catch (Exception e) {
            log.error("Failed to process webhook", e);
            recordWebhookEvent(null, false, e.getMessage());
            return ResponseEntity.ok().build(); // Retornar 200 mesmo com erro (retry do Stripe)
        }
    }
    
    private String parseTimestamp(String signatureHeader) {
        // Format: "t=1614556800,v1=..."
        for (String pair : signatureHeader.split(",")) {
            if (pair.startsWith("t=")) {
                return pair.substring(2);
            }
        }
        throw new IllegalArgumentException("Missing timestamp in signature header");
    }
    
    private String parseSignatureHash(String signatureHeader, String version) {
        // Format: "t=1614556800,v1=..."
        for (String pair : signatureHeader.split(",")) {
            if (pair.startsWith(version + "=")) {
                return pair.substring(version.length() + 1);
            }
        }
        throw new IllegalArgumentException("Missing signature hash: " + version);
    }
    
    private boolean isEventAlreadyProcessed(String eventId) {
        return eventRepository.findByStripeEventId(eventId).isPresent() &&
               eventRepository.findByStripeEventId(eventId).get().isProcessed();
    }
    
    private void recordWebhookEvent(Event event, boolean success, String errorMessage) {
        StripeWebhookEvent webhookEvent = new StripeWebhookEvent();
        if (event != null) {
            webhookEvent.setStripeEventId(event.getId());
            webhookEvent.setEventType(event.getType());
            webhookEvent.setPayload(Event.GSON.toJson(event.getDataObjectDeserializer()));
        }
        webhookEvent.setProcessed(success);
        webhookEvent.setErrorMessage(errorMessage);
        webhookEvent.setReceivedAt(LocalDateTime.now());
        eventRepository.save(webhookEvent);
    }
}
```

### Step 3: StripeWebhookProcessor.java

```java
@Component
@Slf4j
public class StripeWebhookProcessor {
    
    @Resource(name = "stripeEventHandlers")
    private Map<String, StripeEventHandler<?>> handlers;
    
    public void process(Event event) throws Exception {
        String eventType = event.getType();
        log.info("Processing event type: {}", eventType);
        
        StripeEventHandler<?> handler = handlers.get(eventType);
        if (handler == null) {
            log.warn("No handler found for event type: {}", eventType);
            return;
        }
        
        try {
            handler.handle(event);
            log.info("Event processed successfully: {}", eventType);
        } catch (Exception e) {
            log.error("Error processing event: {}", eventType, e);
            throw e;
        }
    }
}
```

### Step 4: Event Handlers

```java
// Interface
public interface StripeEventHandler<T> {
    void handle(Event event) throws Exception;
}

// Handler: Invoice Payment Succeeded
@Component("invoice.payment_succeeded")
@Slf4j
public class InvoicePaymentSucceededHandler implements StripeEventHandler<Invoice> {
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Override
    public void handle(Event event) throws Exception {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject();
        log.info("Processing invoice payment success: invoiceId={}, amount={}", 
            invoice.getId(), invoice.getAmountPaid());
        
        // Atualizar subscription status
        String stripeCustomerId = invoice.getCustomer();
        subscriptionService.markPaymentSuccessful(stripeCustomerId, invoice.getId());
    }
}

// Handler: Subscription Deleted
@Component("customer.subscription.deleted")
@Slf4j
public class SubscriptionDeletedHandler implements StripeEventHandler<Subscription> {
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Override
    public void handle(Event event) throws Exception {
        Subscription subscription = (Subscription) event.getDataObjectDeserializer().getObject();
        log.info("Processing subscription deleted: subscriptionId={}", subscription.getId());
        
        // Atualizar subscription local
        subscriptionService.markAsDeletedFromStripe(subscription.getId());
    }
}

// Handler: Subscription Updated
@Component("customer.subscription.updated")
@Slf4j
public class SubscriptionUpdatedHandler implements StripeEventHandler<Subscription> {
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Override
    public void handle(Event event) throws Exception {
        Subscription subscription = (Subscription) event.getDataObjectDeserializer().getObject();
        log.info("Processing subscription updated: subscriptionId={}, status={}", 
            subscription.getId(), subscription.getStatus());
        
        // Sincronizar status com local
        subscriptionService.syncWithStripe(subscription);
    }
}
```

### Step 5: Exceções Customizadas

```java
public class StripeSignatureVerificationException extends RuntimeException {
    public StripeSignatureVerificationException(String message) {
        super(message);
    }
    
    public StripeSignatureVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class StripeTimestampExpiredException extends RuntimeException {
    public StripeTimestampExpiredException(String message) {
        super(message);
    }
    
    public StripeTimestampExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

# 2. CONFIGURAÇÃO CENTRALIZADA DAS VARIÁVEIS STRIPE

## 📋 Variáveis de Ambiente Necessárias

### Development (test keys)
```bash
STRIPE_SECRET_KEY=sk_test_xxxxxxxxxxxxxx
STRIPE_PUBLISHABLE_KEY=pk_test_xxxxxxxxxxxxxx
STRIPE_WEBHOOK_SECRET=whsec_test_xxxxxxxxxxxxxx
```

### Production (live keys)
```bash
STRIPE_SECRET_KEY=sk_live_xxxxxxxxxxxxxx
STRIPE_PUBLISHABLE_KEY=pk_live_xxxxxxxxxxxxxx
STRIPE_WEBHOOK_SECRET=whsec_live_xxxxxxxxxxxxxx
```

---

## 🔧 StripeProperties.java (Configuration Class)

```java
@Configuration
@ConfigurationProperties(prefix = "stripe")
@Getter
@Setter
@Validated
@Slf4j
public class StripeProperties {
    
    private Api api = new Api();
    private Webhook webhook = new Webhook();
    private Retry retry = new Retry();
    private Timeout timeout = new Timeout();
    private Events events = new Events();
    
    @PostConstruct
    public void init() {
        log.info("Initializing Stripe configuration");
        
        // Validar que as chaves necessárias estão configuradas
        if (api.secretKey == null || api.secretKey.isBlank()) {
            log.error("CRITICAL: stripe.api.secret-key is not configured!");
            throw new IllegalStateException("Stripe secret key is required");
        }
        
        if (api.publishableKey == null || api.publishableKey.isBlank()) {
            log.warn("stripe.api.publishable-key is not configured (optional for backend)");
        }
        
        if (webhook.secret == null || webhook.secret.isBlank()) {
            log.error("CRITICAL: stripe.webhook.secret is not configured!");
            throw new IllegalStateException("Stripe webhook secret is required");
        }
        
        // Inicializar SDK Stripe com secret key
        Stripe.apiKey = this.api.secretKey;
        log.info("Stripe SDK initialized successfully");
        
        // Configurar timeouts para requisições HTTP
        RequestOptions.RequestOptionsBuilder builder = RequestOptions.builder();
        builder.setConnectTimeout(this.timeout.connectionMs);
        builder.setReadTimeout(this.timeout.readMs);
        // RequestOptions não são globais, devem ser passadas por requisição
    }
    
    @Getter
    @Setter
    public static class Api {
        @NotBlank(message = "Stripe secret key is required")
        private String secretKey;
        
        @NotBlank(message = "Stripe publishable key is required")
        private String publishableKey;
    }
    
    @Getter
    @Setter
    public static class Webhook {
        @NotBlank(message = "Stripe webhook secret is required")
        private String secret;
        
        private String path = "/stripe/webhook";
        
        private boolean enabled = true;
        
        private long timestampToleranceSeconds = 300; // 5 minutos
    }
    
    @Getter
    @Setter
    public static class Retry {
        private int maxAttempts = 3;
        
        private long initialDelayMs = 1000;
        
        private double multiplier = 2.0;
        
        public long getDelayForAttempt(int attempt) {
            return (long) (initialDelayMs * Math.pow(multiplier, attempt - 1));
        }
    }
    
    @Getter
    @Setter
    public static class Timeout {
        private long connectionMs = 10000; // 10 segundos
        
        private long readMs = 30000; // 30 segundos
    }
    
    @Getter
    @Setter
    public static class Events {
        private boolean enabled = true;
        
        private int maxAgeDays = 90;
    }
}
```

---

## 📝 application.yml (Exemplo Completo)

```yaml
spring:
  application:
    name: leadflow-backend
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  
  datasource:
    url: jdbc:postgresql://localhost:5432/leadflow
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}

# ============================================
# STRIPE CONFIGURATION - CENTRALIZED
# ============================================
stripe:
  # API Configuration
  api:
    secret-key: ${STRIPE_SECRET_KEY:sk_test_xxxx}  # REQUIRED - Set via env var
    publishable-key: ${STRIPE_PUBLISHABLE_KEY:pk_test_xxxx}  # For frontend
  
  # Webhook Configuration
  webhook:
    secret: ${STRIPE_WEBHOOK_SECRET:whsec_test_xxxx}  # REQUIRED - Set via env var
    path: /stripe/webhook
    enabled: true
    timestamp-tolerance-seconds: 300  # 5 minutos para replay attack prevention
  
  # Retry Configuration
  retry:
    max-attempts: 3
    initial-delay-ms: 1000
    multiplier: 2.0  # exponential backoff: 1s, 2s, 4s
  
  # HTTP Timeout Configuration
  timeout:
    connection-ms: 10000  # 10 segundos para conectar
    read-ms: 30000       # 30 segundos para ler resposta
  
  # Events Configuration
  events:
    enabled: true
    max-age-days: 90

# ============================================
# BILLING CONFIGURATION
# ============================================
billing:
  enabled: true
  
  # Subscription defaults
  subscription:
    trial-days: 7
    limits:
      max-leads: 500
      max-users: 10
      max-ai-executions: 1000
  
  # Pricing (pode ser consultado do Stripe API também)
  plans:
    free:
      stripe-product-id: ${STRIPE_PRODUCT_FREE_ID:prod_xxxx}
      stripe-price-id: ${STRIPE_PRICE_FREE_ID:price_xxxx}
      description: "Free Plan - 7 day trial"
    standard:
      stripe-product-id: ${STRIPE_PRODUCT_STANDARD_ID:prod_yyyy}
      stripe-price-id: ${STRIPE_PRICE_STANDARD_ID:price_yyyy}
      description: "Standard Plan - $29/month"
    enterprise:
      stripe-product-id: ${STRIPE_PRODUCT_ENTERPRISE_ID:prod_zzzz}
      stripe-price-id: ${STRIPE_PRICE_ENTERPRISE_ID:price_zzzz}
      description: "Enterprise Plan - Custom pricing"
```

---

## 🔐 .env.example (Para desenvolvimento local)

```bash
# Database
DB_USER=postgres
DB_PASSWORD=postgres
DB_URL=jdbc:postgresql://localhost:5432/leadflow

# JWT
JWT_SECRET=your-super-secret-jwt-key-here-minimum-256-bits-recommended
JWT_EXPIRATION_MS=86400000

# Stripe - Development (Test Keys)
STRIPE_SECRET_KEY=sk_test_51234567890abcdefghijklmnopqrst
STRIPE_PUBLISHABLE_KEY=pk_test_51234567890abcdefghijklmnopqrst
STRIPE_WEBHOOK_SECRET=whsec_test_1234567890abcdefghijklmnopqrst

# Stripe Product IDs (Test Mode)
STRIPE_PRODUCT_FREE_ID=prod_test_free_123
STRIPE_PRODUCT_STANDARD_ID=prod_test_standard_123
STRIPE_PRODUCT_ENTERPRISE_ID=prod_test_enterprise_123

# Stripe Price IDs (Test Mode)
STRIPE_PRICE_FREE_ID=price_test_free_123
STRIPE_PRICE_STANDARD_ID=price_test_standard_123
STRIPE_PRICE_ENTERPRISE_ID=price_test_enterprise_123

# Application
APP_ENV=development
APP_DEBUG=true
```

---

## 🛡️ StripeConfigValidator.java

```java
@Component
@Slf4j
public class StripeConfigValidator {
    
    private final StripeProperties stripeProperties;
    
    @Autowired
    public StripeConfigValidator(StripeProperties stripeProperties) {
        this.stripeProperties = stripeProperties;
        validateConfiguration();
    }
    
    public void validateConfiguration() {
        log.info("=== Validating Stripe Configuration ===");
        
        // Verificar se está em modo test ou live
        String mode = stripeProperties.getApi().getSecretKey().startsWith("sk_test_") 
            ? "TEST" : "LIVE";
        log.info("Stripe Mode: {}", mode);
        
        // Validar que securekey e webhook secret estão configurados
        if (stripeProperties.getApi().getSecretKey().isBlank()) {
            throw new IllegalStateException("STRIPE_SECRET_KEY environment variable is not set!");
        }
        
        if (stripeProperties.getWebhook().getSecret().isBlank()) {
            throw new IllegalStateException("STRIPE_WEBHOOK_SECRET environment variable is not set!");
        }
        
        // Validar formato de chaves
        validateKeyFormat(stripeProperties.getApi().getSecretKey(), "sk_");
        validateKeyFormat(stripeProperties.getApi().getPublishableKey(), "pk_");
        validateKeyFormat(stripeProperties.getWebhook().getSecret(), "whsec_");
        
        // Testar conexão com Stripe API
        testStripeConnection();
        
        log.info("✅ Stripe configuration validation completed successfully");
    }
    
    private void validateKeyFormat(String key, String prefix) {
        if (!key.startsWith(prefix)) {
            throw new IllegalStateException(
                String.format("Invalid Stripe key format. Expected prefix: %s, Got: %s", 
                    prefix, key.substring(0, Math.min(10, key.length())))
            );
        }
    }
    
    private void testStripeConnection() {
        try {
            log.debug("Testing Stripe API connection...");
            Map<String, Object> params = new HashMap<>();
            params.put("limit", 1);
            // Esta chamada teste verifica se a key é válida
            com.stripe.model.Customer.list(params);
            log.info("✅ Stripe API connection successful");
        } catch (Exception e) {
            log.error("❌ Failed to connect to Stripe API", e);
            throw new IllegalStateException("Stripe API connection failed: " + e.getMessage());
        }
    }
}
```

---

## 🎯 Como Usar no Código

### 1. Injetar StripeProperties

```java
@Service
@Slf4j
public class SubscriptionService {
    
    private final StripeProperties stripeProperties;
    
    @Autowired
    public SubscriptionService(StripeProperties stripeProperties) {
        this.stripeProperties = stripeProperties;
    }
    
    public void createSubscription(String customerId) {
        // Criar subscription usando configuração centralizada
        String priceId = stripeProperties.getPlans().getStandard().getStripePriceId();
        
        // Usar configuraçõesentralizadas de retry/timeout
        RequestOptions options = RequestOptions.builder()
            .setConnectTimeout(stripeProperties.getTimeout().getConnectionMs())
            .setReadTimeout(stripeProperties.getTimeout().getReadMs())
            .build();
    }
    
    public boolean performRetryableOperation(StripeRetryable command) {
        int maxAttempts = stripeProperties.getRetry().getMaxAttempts();
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                command.execute();
                return true;
            } catch (StripeException e) {
                if (attempt < maxAttempts) {
                    long delay = stripeProperties.getRetry().getDelayForAttempt(attempt);
                    log.warn("Retry attempt {}/{} after {}ms", attempt, maxAttempts, delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    throw e;
                }
            }
        }
        return false;
    }
}

@FunctionalInterface
public interface StripeRetryable {
    void execute() throws StripeException;
}
```

### 2. Acessar Configurações

```java
// Webhook secret (no StripeWebhookValidator)
String webhookSecret = stripeProperties.getWebhook().getSecret();

// Max attempts para retry
int maxAttempts = stripeProperties.getRetry().getMaxAttempts();

// Timeout de leitura
long readTimeout = stripeProperties.getTimeout().getReadMs();

// Limite de leads
int maxLeads = stripeProperties.getPlans().getStandard().getMaxLeads();
```

---

## ✅ Checklist de Implementação

- [ ] Criar `StripeProperties.java` com @ConfigurationProperties
- [ ] Criar `StripeWebhookValidator.java` para validar HMAC e timestamp
- [ ] Criar `StripeWebhookProcessor.java` para router de eventos
- [ ] Criar handlers específicos (InvoicePaymentSucceededHandler, etc)
- [ ] Criar exceções customizadas (StripeSignatureVerificationException, etc)
- [ ] Modificar `StripeWebhookController.java` para usar validador
- [ ] Atualizar `application.yml` com configuração centralizada
- [ ] Criar `.env.example` com todas as variáveis necessárias
- [ ] Criar `StripeConfigValidator.java` para validar config no startup
- [ ] Criar `StripeEventRepository` para auditoria de eventos
- [ ] Atualizar `pom.xml` com dependências (se necessário)
- [ ] Testar webhook com Stripe CLI: `stripe listen --forward-to localhost:8080/stripe/webhook`
- [ ] Testar retry/timeout com simulação de falhas

---

## 🧪 Testing Webhook Locally

```bash
# 1. Instalar Stripe CLI
# https://stripe.com/docs/stripe-cli

# 2. Logar no Stripe
stripe login

# 3. Escutar eventos e forwarder para local
stripe listen --forward-to localhost:8080/stripe/webhook

# 4. Trigger eventos de teste
stripe trigger customer.subscription.updated
stripe trigger invoice.payment_succeeded
stripe trigger customer.subscription.deleted

# 5. Verificar que webhooks foram recebidos
stripe logs tail
```

---

## 📊 Monitoramento em Produção

```yaml
# application-prod.yml
logging:
  level:
    com.leadflow.backend.stripe: INFO
    com.stripe: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/stripe-webhooks.log
    max-size: 100MB
    max-history: 30

# Alertas
monitoring:
  stripe:
    webhook-failures-threshold: 5  # Alerta se 5+ falhas em 1 hora
    api-latency-threshold-ms: 5000  # Alerta se latência > 5s
    retry-exhausted-alert: true     # Alerta quando max retries atingido
```
