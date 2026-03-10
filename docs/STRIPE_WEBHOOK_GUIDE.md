# 🔐 Stripe Webhook Configuration - LeadFlow

Complete guide to configure Stripe webhooks for production.

## 📋 Quick Start (5 min)

### Development (Testing Only)
```bash
# 1. Get test webhook secret from Stripe Dashboard
# Stripe → Developers → Webhooks → Click endpoint → Copy Secret

# 2. Add to .env
STRIPE_WEBHOOK_SECRET=whsec_test_XXXXXX

# 3. Use Stripe CLI for local testing
stripe listen --forward-to localhost:8080/api/webhooks/stripe
```

### Production
```bash
# 1. Configure webhook endpoint in Stripe Dashboard
# 2. Select events: subscription.*, invoice.*, payment_intent.*
# 3. Copy secret to environment (AWS Secrets Manager, etc)
# 4. Monitor webhook deliveries in dashboard
```

---

## 1️⃣ Development Setup with Stripe CLI

### Prerequisites
- Stripe Account (free for testing)
- [Stripe CLI](https://stripe.com/docs/stripe-cli) installed

### Step-by-Step

#### A. Install Stripe CLI
```bash
# macOS
brew install stripe/stripe-cli/stripe

# Windows (Chocolatey)
choco install stripe

# Windows (Or download directly)
# https://github.com/stripe/stripe-cli/releases
```

#### B. Login to Stripe
```bash
stripe login
# Completes authentication in browser
# Returns: API key (keep secret!)
```

#### C. Get Test Webhook Secret
```bash
stripe listen --forward-to localhost:8080/api/webhooks/stripe
```

**Output:**
```
Starting configured Stripe CLI
Ready! Your webhook signing secret is: whsec_test_1234567890abcdef

Forwarding events to: http://localhost:8080/api/webhooks/stripe
```

#### D. Save to .env
```bash
STRIPE_WEBHOOK_SECRET=whsec_test_1234567890abcdef
STRIPE_API_KEY=sk_test_YOUR_TEST_KEY
```

#### E. Test Webhook
```bash
# In another terminal
stripe trigger payment_intent.succeeded
# This sends test event to your local server
```

### ✅ Verify Setup
1. Check Stripe CLI output > No errors
2. Check server logs > Webhook received
3. Check database > Event recorded

---

## 2️⃣ Production Setup

### Prerequisites
- Production Stripe Account (live keys)
- Public HTTPS endpoint (required!)
- API key with webhook permissions

### Step-by-Step

#### A. Navigate to Webhook Settings
1. Login to [Stripe Dashboard](https://dashboard.stripe.com)
2. Go to `Developers` → `Webhooks`
3. Click `Add endpoint`

#### B. Configure Endpoint
```
Endpoint URL: https://api.leadflow.com/api/webhooks/stripe
Version: Latest API version (currently 2024-04-10)
```

**Screenshot:**
```
┌─────────────────────────────────────────┐
│ Webhook endpoint URL                    │
├─────────────────────────────────────────┤
│ https://api.leadflow.com/api/webhooks/stripe
│                                         │
│ API Version: 2024-04-10 ✓              │
│                                         │
│ [Create endpoint]                       │
└─────────────────────────────────────────┘
```

#### C. Select Events to Receive

**Recommended Events:**

1. **Subscription Events**
   - [ ] customer.subscription.created
   - [ ] customer.subscription.updated
   - [ ] customer.subscription.deleted

2. **Payment Events**
   - [ ] payment_intent.succeeded
   - [ ] payment_intent.payment_failed
   - [ ] invoice.created
   - [ ] invoice.payment_succeeded
   - [ ] invoice.payment_failed

3. **Charge Events** (Optional)
   - [ ] charge.succeeded
   - [ ] charge.failed
   - [ ] charge.refunded

**Minimum Events (required):**
```
✓ customer.subscription.created
✓ invoice.payment_failed
✓ invoice.payment_succeeded
✓ charge.refunded
```

#### D. Copy Webhook Secret
1. **Revealing Signing Secret**
   - At bottom of endpoint config
   - Click `Reveal` next to signing secret
   - Copy it (starts with `whsec_live_`)

2. **Store Securely**
   ```bash
   # AWS Secrets Manager
   aws secretsmanager create-secret \
     --name leadflow/stripe-webhook-secret \
     --secret-string "whsec_live_XXXXX"
   
   # Or use .env in development
   STRIPE_WEBHOOK_SECRET=whsec_live_XXXXX
   ```

#### E. Test Production Endpoint
```bash
# Send test event from Stripe Dashboard
# Click endpoint → "Send test event"
# Select event type (e.g., customer.subscription.created)
# Click Send test webhook event
```

**Check response:**
- Status: should be `200 OK`
- Check logs: webhook processed

---

## 3️⃣ Webhook Handler Implementation

### Controller
```java
@PostMapping("/webhooks/stripe")
public ResponseEntity<String> handleStripeWebhook(
    @RequestBody String payload,
    @RequestHeader("Stripe-Signature") String signatureHeader) {
    
    try {
        // Validate signature (HMAC-SHA256)
        Event event = webhookValidator.validateAndParseEvent(
            payload, 
            signatureHeader, 
            webhookSecret
        );
        
        // Record metrics
        metrics.recordWebhookReceived(event.getType());
        
        // Process event
        handleEvent(event);
        
        // Log success
        logger.info("Webhook processed: {}", event.getId());
        
        return ResponseEntity.ok("Received");
        
    } catch (InvalidSignatureException e) {
        logger.warn("Invalid webhook signature");
        metrics.recordSignatureValidationFailure();
        return ResponseEntity.badRequest().body("Invalid signature");
    }
}
```

### Webhook Validator
```java
@Component
public class StripeWebhookValidator {
    
    private final String webhookSecret;
    
    public Event validateAndParseEvent(
        String payload, 
        String signatureHeader, 
        String secret) {
        
        // Signature format: t=1234567890,v1=signature
        String timestamp = extractTimestamp(signatureHeader);
        String signature = extractSignature(signatureHeader);
        
        // Validate timestamp (prevent replay attacks)
        if (!isRecentTimestamp(timestamp)) {
            throw new InvalidSignatureException("Timestamp too old");
        }
        
        // Compute expected signature
        String signedContent = timestamp + "." + payload;
        String expectedSig = computeHMAC(signedContent, secret);
        
        // Compare signatures (constant-time)
        if (!constantTimeEquals(signature, expectedSig)) {
            throw new InvalidSignatureException("Signature mismatch");
        }
        
        // Parse event
        return Gson.fromJson(payload, Event.class);
    }
    
    private boolean isRecentTimestamp(String timestamp) {
        long ts = Long.parseLong(timestamp);
        long age = System.currentTimeMillis() / 1000 - ts;
        return age < 300; // 5 minutes tolerance
    }
    
    private String computeHMAC(String data, String secret) {
        // HMAC-SHA256
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
            secret.getBytes(UTF_8), 
            "HmacSHA256"
        ));
        return Hex.encodeHexString(mac.doFinal(data.getBytes(UTF_8)));
    }
    
    private boolean constantTimeEquals(String a, String b) {
        // Prevent timing attacks
        return MessageDigest.isEqual(
            a.getBytes(UTF_8), 
            b.getBytes(UTF_8)
        );
    }
}
```

---

## 4️⃣ Test Webhook Endpoint

### Option A: Postman (UI)
```
1. Open Postman Collection
2. Go to "Webhooks" folder
3. Send "Webhook - Subscription Created"
4. Expected response: 200 OK
```

### Option B: cURL (CLI)
```bash
# Generate signature
TIMESTAMP=$(date +%s)
SECRET="your-webhook-secret"
PAYLOAD='{"type":"customer.subscription.created"}'
SIGNED_CONTENT="$TIMESTAMP.$PAYLOAD"
SIGNATURE=$(echo -n "$SIGNED_CONTENT" | \
  openssl dgst -sha256 -hmac "$SECRET" | \
  sed 's/^.* //')

# Send webhook
curl -X POST https://api.leadflow.com/api/webhooks/stripe \
  -H "Content-Type: application/json" \
  -H "Stripe-Signature: t=$TIMESTAMP,v1=$SIGNATURE" \
  -d "$PAYLOAD"
```

### Option C: Stripe CLI
```bash
# Listen for local webhooks
stripe listen --forward-to localhost:8080/api/webhooks/stripe

# Send test event (in another terminal)
stripe trigger customer.subscription.created
stripe trigger invoice.payment_failed
stripe trigger charge.refunded
```

---

## 5️⃣ Event Processing

### Supported Events

#### subscription.created
```json
{
  "type": "customer.subscription.created",
  "data": {
    "object": {
      "id": "sub_1234567890",
      "customer": "cus_0000000000",
      "status": "active",
      "current_period_end": 1704067200,
      "items": {
        "data": [{
          "price": {
            "id": "price_1234567890",
            "unit_amount": 2999,
            "currency": "usd"
          }
        }]
      }
    }
  }
}
```

**Handler:**
```java
private void handleSubscriptionCreated(Event event) {
    Subscription subscription = (Subscription) event.getDataObjectDeserializer()
        .getObject()
        .orElse(null);
    
    // Save to database
    Lead lead = leadRepository.findByStripeCustomerId(
        subscription.getCustomer()
    );
    lead.setSubscriptionStatus(subscription.getStatus());
    lead.setNextBillingDate(
        Instant.ofEpochSecond(subscription.getCurrentPeriodEnd())
    );
    leadRepository.save(lead);
    
    // Send confirmation email
    emailService.sendSubscriptionConfirmation(lead);
    
    // Record metric
    metrics.recordSubscriptionCreated();
}
```

#### invoice.payment_failed
```json
{
  "type": "invoice.payment_failed",
  "data": {
    "object": {
      "id": "in_1234567890",
      "customer": "cus_0000000000",
      "subscription": "sub_1234567890",
      "amount_due": 2999,
      "attempt_count": 1,
      "next_payment_attempt": 1704153600
    }
  }
}
```

**Handler:**
```java
private void handlePaymentFailed(Event event) {
    Invoice invoice = (Invoice) event.getDataObjectDeserializer()
        .getObject()
        .orElse(null);
    
    Lead lead = leadRepository.findByStripeCustomerId(
        invoice.getCustomer()
    );
    
    // Send failure notification
    emailService.sendPaymentFailedNotification(lead, invoice);
    
    // Increment retry count
    lead.setPaymentRetryCount(lead.getPaymentRetryCount() + 1);
    leadRepository.save(lead);
    
    // Record metric
    metrics.recordPaymentFailed();
    
    // Alert (if retry count > threshold)
    if (lead.getPaymentRetryCount() > 3) {
        alertService.sendPaymentAlert(lead);
    }
}
```

#### charge.refunded
```java
private void handleChargeRefunded(Event event) {
    Charge charge = (Charge) event.getDataObjectDeserializer()
        .getObject()
        .orElse(null);
    
    // Find related invoice
    Invoice invoice = invoiceRepository.findByChargeId(charge.getId());
    
    // Mark as refunded
    invoice.setStatus("refunded");
    invoice.setRefundedAt(Instant.now());
    invoiceRepository.save(invoice);
    
    // Send confirmation
    emailService.sendRefundConfirmation(invoice);
    
    // Record metric
    metrics.recordRefund(charge.getAmount());
}
```

---

## 6️⃣ Monitoring & Debugging

### View Webhook Activity
1. Stripe Dashboard → Developers → Webhooks
2. Click endpoint → View all events
3. Check status (✅ succeeded, ❌ failed)
4. View response body (errors logged)

### Enable Webhook Logging
```properties
# application-prod.yml
logging:
  level:
    com.leadflow.backend.webhook: DEBUG
    com.leadflow.backend.billing: DEBUG
```

### Webhook Delivery Report
```bash
# Every webhook delivery is recorded with:
{
  "timestamp": "2026-03-10T12:30:45Z",
  "event_id": "evt_1234567890",
  "status": "succeeded",  // or "failed"
  "response_status": 200,
  "duration_ms": 145,
  "attempt": 1,
  "error": null
}
```

### Prometheus Metrics
```
# Important metrics
webhook_processing_duration_seconds
webhook_processing_success_total
webhook_processing_failure_total
webhook_signature_validation_total
webhook_timestamp_validation_total

# Example query
rate(webhook_processing_success_total[5m])
```

---

## 7️⃣ Troubleshooting

### Webhook Not Received
```
❌ Problem: Webhook sent from Stripe but not processed
❌ Possible causes:
  1. Endpoint URL changed
  2. Server down during delivery
  3. Network firewall blocking

✅ Solution:
  - Check endpoint URL in dashboard
  - Verify server is running (HTTP 200)
  - Re-send test event from dashboard
  - Check logs: tail -f logs/leadflow.log
```

### Signature Validation Failed
```
❌ Error: "Invalid webhook signature"
❌ Possible causes:
  1. Wrong webhook secret
  2. Payload corrupted
  3. Clock skew (server time wrong)

✅ Solution:
  - Verify STRIPE_WEBHOOK_SECRET is correct
  - Check server system time (ntpdate)
  - Check request body not modified
  - Compare v1 signature with computed value
```

###Webhook Processed but Event Not Recorded
```
❌ Problem: Webhook received (200 OK) but no action taken
❌ Possible causes:
  1. Event type not handled
  2. Event data parser error
  3. Database transaction failed

✅ Solution:
  - Verify event type is supported
  - Check logs: grep "Unhandled event" logs/leadflow.log
  - Verify database connection
  - Monitor resource usage (disk, memory)
```

### Duplicate Webhook Processing
```
❌ Problem: Same event processed multiple times
❌ Causes:
  - Stripe retries if doesn't receive 200
  - idempotency key not used

✅ Solution:
  - Store event ID in database (seen_events table)
  - Skip if already processed
  - Use database constraints to prevent duplicates
  
  CREATE TABLE webhook_events (
    event_id VARCHAR(100) PRIMARY KEY,
    event_type VARCHAR(100),
    processed_at TIMESTAMP,
    UNIQUE (event_id)
  );
```

---

## 8️⃣ Security Best Practices

### ✅ DO:
- Always validate signature (HMAC-SHA256)
- Check timestamp (prevent replay attacks)
- Use HTTPS for endpoint
- Store webhook secret securely
- Log all webhook activity
- Monitor failed deliveries
- Rotate webhook secret quarterly

### ❌ DON'T:
- Hardcode webhook secret in code
- Log webhook payloads (PII/secrets)
- Trust Stripe signature in testing
- Disable signature validation
- Process old events (> 5 min)
- Share webhook secret via email/chat

### Additional Security

```java
// Add rate limiting
@RateLimiter(limit = 1000, duration = "1m")
@PostMapping("/webhooks/stripe")
public ResponseEntity<String> handleWebhook(...) { ... }

// Add request validation
@PostMapping("/webhooks/stripe")
public ResponseEntity<String> handleWebhook(
    @RequestBody @Valid WebhookPayload payload,
    @RequestHeader("Stripe-Signature") String signature) { ... }

// Log sensitive operations
logger.info("Webhook received: type={}, id={}", 
    event.getType(), event.getId());
```

---

## 9️⃣ Monitoring Dashboard

### Key Metrics to Track
```
1. Webhook delivery success rate (target: 99.9%)
2. Average processing time (target: < 500ms)
3. Failed events (track daily)
4. Event lag (delay from Stripe to processing)
5. Signature validation failures
```

### Grafana Dashboard
```json
{
  "title": "Stripe Webhooks",
  "panels": [
    {
      "title": "Webhook Success Rate",
      "targets": [{"expr": "sum(rate(webhook_processing_success_total[5m])) / sum(rate(webhook_processing_total[5m]))"}]
    },
    {
      "title": "Processing Duration (P95)",
      "targets": [{"expr": "histogram_quantile(0.95, webhook_processing_duration_seconds)"}]
    },
    {
      "title": "Failed Webhooks (5m)",
      "targets": [{"expr": "sum(rate(webhook_processing_failure_total[5m]))"}]
    }
  ]
}
```

---

_Last updated: March 2026_
