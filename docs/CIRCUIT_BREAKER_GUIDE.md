# 🛡️ Circuit Breaker Implementation Guide - LeadFlow

Complete guide to implementing Resilience4j Circuit Breaker pattern for fault-tolerant API integration.

## 📋 Quick Start

### Enable Circuit Breaker
```java
@Service
public class PaymentService {

    @CircuitBreaker(name = "stripe", fallbackMethod = "processPaymentFallback")
    @Retry(name = "stripe-retry")
    public boolean processPayment(String customerId, double amount) {
        // Call Stripe API
        return stripeClient.charge(customerId, amount);
    }

    // Fallback method - called when circuit opens
    public boolean processPaymentFallback(String customerId, double amount, Exception ex) {
        log.warn("Stripe API down - queuing payment for retry", ex);
        PaymentQueue.queue(customerId, amount);
        return false;
    }
}
```

### Fallback Strategies
- ✅ **Queue for Later** - Store failed requests for async retry
- ✅ **Use Cache** - Return cached data when API unavailable
- ✅ **Use Alternative** - Call backup service/API
- ✅ **Fail Fast** - Return error immediately to prevent cascading failures

---

## 🏗️ Architecture

### Circuit Breaker States

```
┌─────────────────────────────────────────────────┐
│                 CLOSED (Normal)                 │
│      ✅ Requests pass through                  │
│      ✅ Service is healthy                     │
└──────────────┬────────────────────────────────┘
               │
               │ Threshold exceeded (error rate > 50%)
               ↓
┌─────────────────────────────────────────────────┐
│                  OPEN (Failing)                 │
│      ✅ Fast fail - no requests sent            │
│      ✅ Return fallback immediately             │
│      ✅ Prevents cascading failures             │
└──────────────┬────────────────────────────────┘
               │
               │ Wait 30 seconds
               ↓
┌─────────────────────────────────────────────────┐
│              HALF_OPEN (Testing)               │
│      ✅ Allow limited requests (3 attempts)     │
│      ✅ Test if service recovered               │
│      ✅ If success → CLOSED                     │
│      ✅ If failure → OPEN (restart wait)        │
└──────────────┬────────────────────────────────┘
               │
               │ All tests pass
               ↓
              CLOSED
```

### Metrics Flow

```
API Call
   │
   ├→ Success: Increment success counter
   │           Record latency
   │           Calculate error rate
   │
   ├→ Failure: Increment failure counter
   │           Check error rate threshold
   │           Decide: Process or Open?
   │
   └→ Slow: Increment slow counter
             Check slow rate
             May trigger circuit open
```

---

## 🎛️ Configuration

### Stripe Circuit Breaker
```yaml
resilience4j:
  circuitbreaker:
    instances:
      stripe:
        failureRateThreshold: 50           # Open if 50%+ fail
        waitDurationInOpenState: 30000     # Wait 30s to retry
        permittedNumberOfCallsInHalfOpenState: 3  # Try 3 calls
        minimumNumberOfCalls: 5            # Need 5 calls to evaluate
        slowCallDurationThreshold: 2000    # > 2s is considered slow
```

**What This Means:**
- If 5 calls happen and 3+ fail → **Circuit Opens**
- Circuit stays open for **30 seconds** without letting requests through
- After 30 seconds → **Half-Open** state, tries 3 test calls
- If those 3 succeed → Circuit **Closes** (back to normal)
- If any fail → Back to **Open** state

### Email Circuit Breaker
```yaml
resilience4j:
  circuitbreaker:
    instances:
      email:
        failureRateThreshold: 60           # More lenient (60%)
        waitDurationInOpenState: 60000     # Wait 60s (SMTP is slower)
        slowCallDurationThreshold: 3000    # > 3s is slow
```

### Database Circuit Breaker
```yaml
resilience4j:
  circuitbreaker:
    instances:
      database:
        failureRateThreshold: 70           # Very lenient
        waitDurationInOpenState: 15000     # Quick recovery attempt
        permittedNumberOfCallsInHalfOpenState: 5  # More test calls
```

---

## 💻 Implementation Patterns

### Pattern 1: Queue for Later Retry
```java
@Service
public class PaymentService {

    @CircuitBreaker(name = "stripe", fallbackMethod = "paymentFallback")
    public PaymentResult processPayment(Payment payment) {
        return stripeClient.createCharge(payment);
    }

    public PaymentResult paymentFallback(Payment payment, Exception ex) {
        log.warn("Stripe circuit open - queuing payment", ex);
        paymentQueue.add(payment);
        return PaymentResult.QUEUED;
    }
}

// Background job retries queued payments
@Scheduled(fixedDelay = 60000)
public void retryQueuedPayments() {
    List<Payment> queued = paymentQueue.getAll();
    for (Payment payment : queued) {
        try {
            PaymentResult result = paymentService.processPayment(payment);
            if (result.isSuccess()) {
                paymentQueue.remove(payment);
            }
        } catch (Exception e) {
            log.warn("Payment retry failed: {}", payment.getId());
        }
    }
}
```

### Pattern 2: Cache Fallback
```java
@Service
public class UserService {

    @CircuitBreaker(name = "external-api", fallbackMethod = "getUserFallback")
    public UserData getUser(String userId) {
        return externalApi.fetchUser(userId);
    }

    public UserData getUserFallback(String userId, Exception ex) {
        log.warn("External API down - returning cached user", ex);
        UserData cached = cacheService.get("user:" + userId);
        return cached != null ? cached : UserData.EMPTY;
    }
}
```

### Pattern 3: Alternative Service Fallback
```java
@Service
public class EmailService {

    @CircuitBreaker(name = "email", fallbackMethod = "sendEmailFallback")
    public boolean sendEmail(String to, String subject, String body) {
        return primarySmtpService.send(to, subject, body);
    }

    public boolean sendEmailFallback(String to, String subject, String body, Exception ex) {
        log.warn("Primary SMTP failed - trying backup provider", ex);
        try {
            return backupEmailService.send(to, subject, body);  // SendGrid fallback
        } catch (Exception backupEx) {
            log.error("Backup email also failed", backupEx);
            emailQueue.add(to, subject, body);
            return false;
        }
    }
}
```

### Pattern 4: Fail Fast
```java
@Service
public class AuthService {

    @CircuitBreaker(name = "stripe", fallbackMethod = "validatePlanFallback")
    public boolean validateUserPlan(String userId) {
        // Check subscription in Stripe
        return stripeApi.isValid(userId);
    }

    public boolean validatePlanFallback(String userId, Exception ex) {
        log.error("Cannot validate plan - Stripe API unavailable", ex);
        // Deny access - better safe than sorry
        return false;
    }
}
```

---

## 📊 Monitoring & Observability

### View Circuit Breaker Status
```bash
# Check health endpoint
curl http://localhost:8080/api/actuator/health/circuitbreakers

# Response
{
  "status": "UP",
  "details": {
    "stripe": {
      "status": "OPEN",
      "details": {
        "state": "OPEN",
        "failureRate": 65.2,
        "slowCallRate": 23.5
      }
    },
    "email": {
      "status": "CLOSED",
      "details": {
        "state": "CLOSED",
        "failureRate": 1.2
      }
    }
  }
}
```

### Prometheus Metrics
```
# Error rate (%)
resilience4j_circuitbreaker_failure_rate{name="stripe"} 65.2

# Success rate (%)
resilience4j_circuitbreaker_success_rate{name="stripe"} 34.8

# Number of calls
resilience4j_circuitbreaker_calls_total{name="stripe", outcome="success"} 51
resilience4j_circuitbreaker_calls_total{name="stripe", outcome="failure"} 99

# State (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
resilience4j_circuitbreaker_state{name="stripe"} 1

# Retry attempts
resilience4j_retry_calls_total{name="stripe-retry", kind="successful_with_retry"} 12
resilience4j_retry_calls_total{name="stripe-retry", kind="failed_with_retry"} 3
```

### Grafana Dashboard Queries

**Circuit Breaker State:**
```promql
resilience4j_circuitbreaker_state{name="stripe"}
```

**Failure Rate Trend:**
```promql
rate(resilience4j_circuitbreaker_calls_total{outcome="failure"}[5m])
```

**Success Count:**
```promql
increase(resilience4j_circuitbreaker_calls_total{outcome="success"}[5m])
```

---

## ⚠️ Common Issues & Solutions

### Issue 1: Circuit Always Open
```
❌ Problem: Circuit breaker stuck in OPEN state
❌ Symptoms: All requests fail, timeout in fallback
❌ Causes: 
   - API permanently down
   - Network misconfiguration
   - Threshold too low

✅ Solution:
1. Check if actual service is up: curl https://api.stripe.com
2. Increase minimumNumberOfCalls threshold
3. Decrease failureRateThreshold temporarily for testing
4. Check application logs for actual errors
```

### Issue 2: Fallback Not Called
```
❌ Problem: circuitBreaker annotation not working
❌ Symptoms: Original exception thrown, no fallback

✅ Solution:
1. Ensure fallback method signature matches
2. Include Exception parameter: (Exception ex)
3. Make sure @CircuitBreaker annotation present
4. Verify circuit breaker name exists in config
5. Check Spring AOP is enabled
```

### Issue 3: Too Many Retries
```
❌ Problem: Service overloaded with retry requests
❌ Symptoms: Circuit keeps retrying, service slower

✅ Solution:
1. Reduce maxAttempts in retry config
2. Increase waitDuration between retries
3. Use exponential backoff instead of fixed
4. Add timeout annotation to prevent hanging
```

### Issue 4: False Positives (Slow Calls)
```
❌ Problem: Circuit opens due to slow calls, not errors
❌ Symptoms: Low error rate but circuit still open

✅ Solution:
1. Increase slowCallDurationThreshold
2. Reduce slowCallRateThreshold
3. Add warmup period (don't count first requests)
4. Use different circuit breaker for slow operations
```

---

## 🔧 Implementation Checklist

- ✅ Add Resilience4j dependencies to `pom.xml`
- ✅ Create `Resilience4jConfiguration.java` config class
- ✅ Define circuit breaker instances (stripe, email, database)
- ✅ Configure thresholds in `application.yml`
- ✅ Add `@CircuitBreaker` annotations to service methods
- ✅ Implement fallback methods for each circuit
- ✅ Handle `CallNotPermittedException` (circuit open)
- ✅ Test circuit breaker state transitions
- ✅ Add metrics to monitoring/alerting system
- ✅ Document fallback strategies
- ✅ Set up Grafana dashboard for visibility

---

## 🚀 Best Practices

### Do's ✅
- ✅ Make fallback methods fast (don't retry in fallback)
- ✅ Use exponential backoff for retries
- ✅ Log circuit breaker state changes
- ✅ Monitor failure rates continuously
- ✅ Set appropriate thresholds per service
- ✅ Test circuit breaker scenarios
- ✅ Queue failures for later retry
- ✅ Use different thresholds for different APIs
- ✅ Implement health checks for dependencies
- ✅ Gracefully degrade functionality

### Don'ts ❌
- ❌ Don't retry indefinitely
- ❌ Don't ignore circuit breaker exceptions
- ❌ Don't use same thresholds for all APIs
- ❌ Don't make fallback slower than original
- ❌ Don't rely solely on circuit breaker
- ❌ Don't forget to test failure scenarios
- ❌ Don't set thresholds too low/high
- ❌ Don't block threads in fallback

---

## 📚 Integration Points

### BillingController
```java
@RestController
@RequestMapping("/api/billing")
public class BillingController {

    @Autowired
    private CircuitBreakerService cbService;

    @PostMapping("/charge")
    public ResponseEntity<?> chargeCustomer(@RequestBody ChargeRequest request) {
        try {
            boolean result = cbService.validateStripeWebhook(
                request.getTimestamp(),
                request.getSignature(),
                request.getBody(),
                request.getSecret()
            );
            // ... process payment ...
        } catch (CallNotPermittedException ex) {
            return ResponseEntity
                .status(503)
                .body("Service temporarily unavailable - try again later");
        }
    }
}
```

### Event Listener
```java
@Component
public class StripeWebhookListener {

    @Autowired
    private CircuitBreakerService cbService;

    @EventListener
    public void handleStripeEvent(StripeEvent event) {
        CompletableFuture<Void> future = cbService.processStripeEventAsync(event.getEvent());
        future.exceptionally(ex -> {
            log.error("Event processing failed", ex);
            return null;
        });
    }
}
```

---

## 📖 Further Reading

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Release It!: Design and Deploy Production-Ready Software](https://pragprog.com/titles/mnee2/release-it-second-edition/)
- [Microservices Patterns](https://microservices.io/patterns/reliability/circuit-breaker.html)

---

_Last updated: March 2026_
