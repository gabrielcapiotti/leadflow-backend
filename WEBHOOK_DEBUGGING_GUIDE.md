# 🔧 Stripe Webhook Debugging Guide

## Common Issues & Solutions

### Issue 1: "Invalid Stripe session object" Log

**Symptom**:
```
ERROR: Invalid Stripe session object
```

**Causes**:
1. Event object is not actually a Checkout Session
2. EventDataObjectDeserializer failed to parse
3. Stripe event type mismatch

**Debug Steps**:
```java
private void handleCheckoutCompleted(Event event) {
    
    // ADD THIS DEBUG LOG
    log.debug("Event Type: {}", event.getType());
    log.debug("Event ID: {}", event.getId());
    
    EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
    StripeObject object = deserializer.getObject().orElse(null);
    
    // ADD THIS DEBUG LOG
    log.debug("Deserialized object class: {}", 
        object != null ? object.getClass().getName() : "NULL");
    
    if (!(object instanceof Session)) {
        log.error("Invalid Stripe session object: expected Session, got {}", 
            object != null ? object.getClass().getSimpleName() : "null");
        return;
    }
    
    Session session = (Session) object;
    log.debug("Successfully cast to Session: {}", session.getId());
    subscriptionService.activateAccount(session);
}
```

---

### Issue 2: "Subscription already processed" (Expected in Retries)

**Symptom** (Normal):
```
WARN: Subscription already processed: sub_12345
```

**This is correct behavior** ✅
- Indicates webhook was received and processed once
- Stripe is retrying due to network timeout
- Idempotency check is working

**Action**: No action needed - system is working correctly

---

### Issue 3: "Could not deserialize to Stripe entity" Exception

**Symptom**:
```
com.stripe.exception.StripeException: Could not deserialize to Stripe entity
```

**Root Cause**:
- Stripe API response changed
- Stripe Java SDK version mismatch
- Event payload corrupted

**Fix**:
```bash
# Check Stripe SDK version
mvn dependency:tree | grep stripe

# Update if needed in pom.xml
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>25.0.0</version>  <!-- Ensure version matches -->
</dependency>

# Rebuild
mvn clean compile
```

---

### Issue 4: NPE in activateAccount()

**Symptom**:
```
java.lang.NullPointerException at SubscriptionService.activateAccount()
```

**Common Causes**:

#### Cause A: Missing email in Session
```java
// BAD: No null check
String email = session.getCustomerEmail();  // Could be null
log.info("Stripe checkout completed for {}", email);
```

**Fix**:
```java
String email = session.getCustomerEmail();
if (email == null || email.isBlank()) {
    log.error("Customer email is not set in Stripe session");
    return;
}
log.info("Stripe checkout completed for {}", email);
```

#### Cause B: Missing subscription ID
```java
// BAD: No null check
String stripeSubscriptionId = session.getSubscription();  // Could be null
```

**Fix**:
```java
String stripeSubscriptionId = session.getSubscription();
if (stripeSubscriptionId == null || stripeSubscriptionId.isBlank()) {
    log.error("Stripe subscription ID is not set in session");
    return;
}
```

#### Cause C: Plan service returns null
```java
// BAD: No null check
Plan plan = planService.getActivePlan();  // Could return null
subscription.setPlan(plan);  // NPE if plan is null
```

**Fix**:
```java
Plan plan = planService.getActivePlan();
if (plan == null) {
    log.error("No active plan found in database");
    throw new IllegalStateException("Active plan not configured");
}
subscription.setPlan(plan);
```

---

### Issue 5: Signature Verification Fails

**Symptom**:
```
com.stripe.exception.SignatureVerificationException: No signatures found matching the expected signature
```

**Root Causes**:
1. Wrong webhook signing secret
2. Webhook URL mismatch
3. Request body modified after signature calculation

**Debug in Controller**:
```java
@PostMapping("/webhook")
public ResponseEntity<String> handleWebhook(HttpServletRequest request) throws IOException {
    
    // DO NOT MODIFY payload after reading!
    String payload = request.getReader()
            .lines()
            .collect(Collectors.joining());
    
    // IMPORTANT: Read signature AFTER payload
    String signature = request.getHeader("Stripe-Signature");
    
    log.debug("Payload length: {} bytes", payload.length());
    log.debug("Signature header: {}", signature != null ? "present" : "missing");
    
    try {
        Event event = stripeService.constructWebhookEvent(payload, signature);
        // ...
    } catch (SignatureVerificationException e) {
        log.error("Signature verification failed. Check webhook secret in StripeService");
        return ResponseEntity.status(400).build();
    }
}
```

**Verify Webhook Secret**:
```bash
# In StripeService.java, ensure:
Stripe.apiKey = "sk_live_...";  // Correct secret key
// Signature verification uses: endpoint_secret (from Stripe Dashboard)

# Test webhook in Stripe Dashboard:
# 1. Go to Settings → Webhooks
# 2. Click "Send test webhook"
# 3. Check logs for signature errors
```

---

### Issue 6: Transactional Rollback (No Data Created)

**Symptom**:
- Controller returns 200 OK
- Logs show "activated" message
- Database is EMPTY

**Cause**:
- Exception occurred after @Transactional started
- Spring rolled back the entire transaction

**Debug**:
```java
@Transactional
public void activateAccount(Session session) {
    try {
        // ... existing code ...
        
        log.info("DEBUG: After vendorService.createVendor()");
        log.info("DEBUG: tenantId = {}", tenantId);
        
        Plan plan = planService.getActivePlan();
        log.info("DEBUG: After planService.getActivePlan()");
        log.info("DEBUG: plan = {}", plan);
        
        // ... rest of code ...
        
    } catch (Exception e) {
        log.error("ERROR in activateAccount: ", e);
        throw e;  // Re-throw to trigger rollback
    }
}
```

**Common Exception Sources**:
1. Vendor email validation fails → VendorService throws exception
2. Plan query returns null → NullPointerException
3. Database uniqueness constraint violated → DataIntegrityViolationException

---

### Issue 7: Webhook Not Being Called

**Symptom**:
- POST /stripe/webhook never receives requests
- No logs from StripeWebhookController

**Debug Checklist**:
```bash
# 1. Verify webhook URL in Stripe Dashboard
# Settings → Webhooks → Endpoint

# Should be: https://your-domain.com/stripe/webhook

# 2. Check if endpoint is publicly accessible
curl -X POST https://your-domain.com/stripe/webhook \
  -H "Stripe-Signature: test" \
  -d '{"type":"checkout.session.completed"}'

# 3. Verify Spring routing
# Check: @RestController, @RequestMapping("/stripe"), @PostMapping("/webhook")

# 4. Check firewall/security
# Stripe IPs: https://stripe.com/files/ip-addresses

# 5. Check if server is running (obvious!)
curl http://localhost:8080/stripe/webhook \
  -X POST
```

---

### Issue 8: Event Type Not Matching

**Symptom**:
```
Stripe event received: checkout.session.completed
// But:
if ("checkout.session.completed".equals(event.getType())) {
    // THIS BLOCK NEVER EXECUTES!
}
```

**Debugging**:
```java
@PostMapping("/webhook")
public ResponseEntity<String> handleWebhook(HttpServletRequest request) throws IOException {
    
    String payload = request.getReader().lines().collect(Collectors.joining());
    String signature = request.getHeader("Stripe-Signature");
    
    Event event = stripeService.constructWebhookEvent(payload, signature);
    
    log.info("Stripe event received: {}", event.getType());
    
    // ADD THIS
    log.debug("Event type class: {}", event.getType().getClass().getName());
    log.debug("Event type value: '{}'", event.getType());
    log.debug("Event type length: {}", event.getType().length());
    
    // STRING COMPARISON WITH "".equals() IS CASE-SENSITIVE
    String eventType = event.getType().trim();
    log.debug("Event type after trim: '{}'", eventType);
    
    if ("checkout.session.completed".equals(eventType)) {
        log.info("MATCH: Event type matches checkout.session.completed");
        handleCheckoutCompleted(event);
    } else {
        log.warn("NO MATCH: Event type '{}' does not match", eventType);
    }
    
    return ResponseEntity.ok("received");
}
```

---

## Testing Locally

### 1. Using Stripe CLI (Recommended)

```bash
# Install Stripe CLI
# https://stripe.com/docs/stripe-cli

# Login to Stripe account
stripe login

# Listen for webhook events
stripe listen --events checkout.session.completed --forward-to http://localhost:8080/stripe/webhook

# Get webhook signing secret (appears in output)
# Export it to environment variable:
export STRIPE_WEBHOOK_SECRET=whsec_test_...

# In StripeService:
endpoint_secret = System.getenv("STRIPE_WEBHOOK_SECRET");
```

### 2. Trigger Test Event

```bash
# In another terminal
stripe trigger checkout.session.completed

# CLI will show in first terminal:
# 2025-01-15 10:23:45  --> event_type: checkout.session.completed [evt_...]
# <--  [200] event_id: evt_...
```

### 3. Monitor Logs

```bash
# In your application terminal:
tail -f logs/application.log | grep Stripe

# Should see:
# INFO: Stripe event received: checkout.session.completed
# INFO: Stripe checkout completed for user@example.com
# INFO: Tenant 550e8400-e29b-41d4-a716-446655440000 activated with plan Premium
```

---

## Logging Configuration for Debugging

### application.properties or logback-spring.xml

```xml
<!-- logback-spring.xml -->
<logger name="com.leadflow.backend.controller.StripeWebhookController" level="DEBUG"/>
<logger name="com.leadflow.backend.service.vendor.SubscriptionService" level="DEBUG"/>
<logger name="com.leadflow.backend.service.stripe.StripeService" level="INFO"/>
```

### Check Logs Production

```bash
# Docker logs
docker logs leadflow-backend-container | grep Stripe

# File logs
tail -f /var/log/leadflow/application.log | grep Stripe

# Kubernetes logs
kubectl logs deployment/leadflow-backend-api | grep Stripe
```

---

## Database Verification

### 1. Check Vendor Created

```sql
SELECT id, user_email, created_at FROM vendors 
WHERE user_email = 'user@example.com'
ORDER BY created_at DESC 
LIMIT 1;

-- Should return UUID id, email, and recent timestamp
```

### 2. Check Subscription Created

```sql
SELECT tenant_id, stripe_customer_id, stripe_subscription_id, status, created_at 
FROM subscriptions 
WHERE stripe_subscription_id = 'sub_12345'
ORDER BY created_at DESC;

-- Should show:
-- tenant_id = vendor.id
-- status = ACTIVE
-- stripe_subscription_id = from Stripe
```

### 3. Check Usage Limits Created

```sql
SELECT tenant_id, available_leads, available_users, available_ai_executions 
FROM usage_limits 
WHERE tenant_id = '<vendor_id>'
ORDER BY created_at DESC;

-- Should show quotas initialized from plan
```

### 4. Full Verification Query

```sql
-- All created together in transaction
SELECT 
    v.id as tenant_id,
    v.user_email,
    s.stripe_subscription_id,
    s.status,
    p.name as plan_name,
    ul.available_leads,
    ul.available_users,
    ul.available_ai_executions
FROM vendors v
LEFT JOIN subscriptions s ON v.id = s.tenant_id
LEFT JOIN plans p ON s.plan_id = p.id
LEFT JOIN usage_limits ul ON v.id = ul.tenant_id
WHERE v.user_email = 'user@example.com'
ORDER BY v.created_at DESC;
```

---

## Performance Monitoring

### Add Metrics

```java
@Transactional
public void activateAccount(Session session) {
    long startTime = System.currentTimeMillis();
    
    try {
        // ... implementation ...
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("Tenant activation completed in {} ms", duration);
        
        // Optional: Send to metrics service
        // metricsService.recordDuration("webhook.checkout.completed", duration);
        
    } catch (Exception e) {
        long duration = System.currentTimeMillis() - startTime;
        log.error("Tenant activation failed after {} ms: {}", duration, e.getMessage());
        throw e;
    }
}
```

---

## Summary Checklist

When debugging webhook issues, verify in order:

- [ ] Webhook URL is publicly accessible
- [ ] Stripe signature verification works
- [ ] Event type matches "checkout.session.completed"
- [ ] Session object deserializes correctly
- [ ] Email, customer ID, subscription ID are not null
- [ ] VendorService creates vendor successfully
- [ ] PlanService returns active plan
- [ ] Subscription saves with all fields
- [ ] UsageService initializes limits
- [ ] Database records are created atomically
- [ ] Logs show all sections completed
- [ ] idempotency check works on retry

**If webhook still not working after checklist:** Check logs with DEBUG level and look for the exact exception message.
