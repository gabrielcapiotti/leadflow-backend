# 📨 Stripe Webhook Events - Complete Integration

## Events Handled by Leadflow Backend

The Stripe webhook integration handles **3 critical events**:

### 1️⃣ **checkout.session.completed** - New Account Activation
### 2️⃣ **invoice.payment_succeeded** - Automatic Renewal
### 3️⃣ **customer.subscription.deleted** - Cancellation

---

## 1️⃣ Event: `checkout.session.completed` - NEW ACCOUNT

### 🎯 Purpose
User completes payment during checkout → activate account with full provisioning

### 📨 Webhook Data Structure
```json
{
  "type": "checkout.session.completed",
  "id": "evt_1234567890",
  "data": {
    "object": {
      "id": "cs_test_xxxxx",
      "customer": "cus_12345",
      "subscription": "sub_12345",
      "customer_email": "user@example.com"
    }
  }
}
```

### 🔄 Processing Flow
```
StripeWebhookController.handleWebhook()
  ↓ (detect type: checkout.session.completed)
StripeWebhookController.handleCheckoutCompleted(event, payload)
  ├─ Deserialize event → extract Session object
  ├─ Verify instanceof Session (type safety)
  └─ Call subscriptionService.activateAccount(session)
       ↓
SubscriptionService.activateAccount(Session session)
  ├─ IDEMPOTENCY CHECK: findByStripeSubscriptionId()
  │  └─ If exists: log warning, return (webhook retry protection)
  │
  ├─ CREATE VENDOR/TENANT
  │  └─ VendorService.createVendor(email)
  │
  ├─ GET PLAN
  │  └─ PlanService.getActivePlan()
  │
  ├─ CREATE SUBSCRIPTION
  │  └─ Set all Stripe IDs, status=ACTIVE
  │
  └─ INITIALIZE USAGE
     └─ UsageService.initializeUsage(tenantId, plan)
```

### ✅ Idempotency Protection
```java
// Check if subscription already exists
Optional<Subscription> existing = 
    subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);

if (existing.isPresent()) {
    log.warn("Subscription already processed. stripeSubscriptionId={}", 
             stripeSubscriptionId);
    return;  // ← Exit, don't duplicate
}
```

**Problem it solves:**
- Stripe may retry webhook if no 200 ACK received
- Multiple retries would create duplicate accounts
- Query prevents duplicate creation

### 📋 Expected Server Logs

```log
[INFO] Stripe event received: checkout.session.completed
[INFO] Processing Stripe checkout for user@example.com
[INFO] Subscription activated for tenant=550e8400-e29b-41d4-a716-446655440000, email=user@example.com
[INFO] Provision started for tenant 550e8400-e29b-41d4-a716-446655440000
[INFO] Schema created for tenant...
[INFO] Admin user created...
```

### 🗄️ Database Changes

**vendors** table:
```sql
INSERT INTO vendors (id, user_email, nome_vendedor, subscription_status, ...)
VALUES ('550e8400-e29b-...', 'user@example.com', 'user', 'TRIAL', ...);
-- id = tenantId for multi-tenancy
```

**subscriptions** table:
```sql
INSERT INTO subscriptions 
  (tenant_id, stripe_customer_id, stripe_subscription_id, status, ...)
VALUES (
  '550e8400-e29b-...',
  'cus_12345',
  'sub_12345',
  'ACTIVE',
  ...
);
-- Links Stripe IDs to Leadflow tenant
```

**usage_limits** table:
```sql
INSERT INTO usage_limits 
  (tenant_id, available_leads, available_users, available_ai_executions)
VALUES (
  '550e8400-e29b-...',
  <plan.leadLimit>,
  <plan.userLimit>, 
  <plan.aiLimit>
);
-- Quotas initialized based on plan
```

---

## 2️⃣ Event: `invoice.payment_succeeded` - AUTOMATIC RENEWAL

### 🎯 Purpose
Renew active subscription when payment succeeds on recurring billing

### 📨 Webhook Data Structure
```json
{
  "type": "invoice.payment_succeeded",
  "id": "evt_9876543210",
  "data": {
    "object": {
      "subscription": "sub_12345",
      "customer": "cus_12345",
      "amount_paid": 2999,
      "currency": "usd"
    }
  }
}
```

### 🔄 Processing Flow
```
StripeWebhookController.handleWebhook()
  ↓ (detect type: invoice.payment_succeeded)
StripeWebhookController.handlePaymentSucceeded(event)
  ↓
SubscriptionService.handlePaymentSucceeded(Event event)
  ├─ Extract Invoice from event
  ├─ Find Vendor by Stripe IDs
  │  └─ Query: findByExternalSubscriptionId(sub_id) OR
  │     findByExternalCustomerId(cus_id)
  │
  ├─ Update lastPaymentAt = now()
  │
  └─ Transition to ATIVA (if not already)
     └─ safeTransition(vendor, ATIVA, 
                      "STRIPE_INVOICE_PAYMENT_SUCCEEDED", eventId)
```

### ✅ Idempotency Protection
```java
// Invoice data includes subscription ID + customer ID
// These are stored in Vendor entity
Vendor vendor = findVendorByStripeIds(
    invoice.getSubscription(),  // sub_12345
    invoice.getCustomer()       // cus_12345
).orElse(null);

if (vendor == null) {
    log.warn("No vendor found");
    return;
}
```

**Problem it solves:**
- Stripe may retry if no ACK received
- Same vendor already exists (not duplicate)
- Just updates payment timestamp + status

### 📋 Expected Server Logs

```log
[INFO] Stripe event received: invoice.payment_succeeded
[INFO] Processing invoice.payment_succeeded event
[INFO] Payment succeeded updated for vendor=550e8400-e29b-..., subscriptionId=sub_12345
```

### 🗄️ Database Changes

**vendors** table:
```sql
UPDATE vendors 
SET last_payment_at = <NOW>
WHERE external_subscription_id = 'sub_12345'
  AND subscription_status != 'ATIVA';
-- Also transitions status to ATIVA if in inactive state
```

**Why important:**
- Marks that vendor paid recently
- Ensures subscription is ATIVA state
- Unlocks API access if it was paused due to payment issue

---

## 3️⃣ Event: `customer.subscription.deleted` - CANCELLATION

### 🎯 Purpose
User or system cancelled subscription → disable all access

### 📨 Webhook Data Structure
```json
{
  "type": "customer.subscription.deleted",
  "id": "evt_1111111111",
  "data": {
    "object": {
      "id": "sub_12345",
      "customer": "cus_12345",
      "status": "canceled"
    }
  }
}
```

### 🔄 Processing Flow
```
StripeWebhookController.handleWebhook()
  ↓ (detect type: customer.subscription.deleted)
StripeWebhookController.handleSubscriptionCancelled(event)
  ↓
SubscriptionService.handleSubscriptionCancelled(Event event)
  ├─ Extract Stripe Subscription object from event
  ├─ Find Vendor by Stripe IDs
  │  └─ Query: findByExternalSubscriptionId(sub_id) OR
  │     findByExternalCustomerId(cus_id)
  │
  └─ Transition to CANCELADA
     └─ safeTransition(vendor, CANCELADA, 
                      "STRIPE_SUBSCRIPTION_CANCELLED", eventId)
```

### ✅ Idempotency Protection
```java
// Same lookup as payment_succeeded
// Multiple retries find same vendor → safe to call again
Vendor vendor = findVendorByStripeIds(
    stripeSubscription.getId(),    // sub_12345
    stripeSubscription.getCustomer() // cus_12345
).orElse(null);

if (vendor == null) {
    log.warn("No vendor found");
    return;
}
```

**Problem it solves:**
- Already transitioned to CANCELADA on first call
- Subsequent retries won't change anything (idempotent)
- Status can't transition from CANCELADA to something else

### 📋 Expected Server Logs

```log
[INFO] Stripe event received: customer.subscription.deleted
[INFO] Processing customer.subscription.deleted event
[INFO] Subscription cancelled for vendor=550e8400-e29b-..., stripeSubscriptionId=sub_12345
```

### 🗄️ Database Changes

**vendors** table:
```sql
UPDATE vendors 
SET subscription_status = 'CANCELADA'
WHERE id = '550e8400-e29b-...';

-- Also records audit
INSERT INTO vendor_subscription_audit 
  (vendor_id, from_status, to_status, reason, external_event_id)
VALUES 
  ('550e8400-e29b-...', 'ATIVA', 'CANCELADA', 
   'STRIPE_SUBSCRIPTION_CANCELLED', 'evt_1111111111');
```

**Impact:**
- Vendor loses all API access
- Leads cannot be fetched
- No AI operations allowed
- Read-only mode or complete block (depends on SubscriptionAccessLevel)

---

## 🔐 Idempotency & Safety Summary

### Stripe Retry Behavior
```
Scenario 1: Controller returns 200 OK immediately
  ✅ Stripe marks as delivered, no retry

Scenario 2: Network fails before 200 response
  Stripe will retry webhook (multiple times)
  Each retry needs to be idempotent
```

### Our Protection Mechanisms

| Event | Idempotency Check | What it prevents |
|-------|-------------------|------------------|
| `checkout.session.completed` | `findByStripeSubscriptionId()` | Duplicate vendors, duplicate subscriptions |
| `invoice.payment_succeeded` | Query vendor by Stripe IDs | Duplicate payments, incorrect status |
| `customer.subscription.deleted` | Query vendor by Stripe IDs, safeTransition() | Status regression (CANCELADA → ATIVA) |

### Safe Transition Rules
```java
private void safeTransition(Vendor vendor, SubscriptionStatus target, 
                           String reason, String externalEventId) {
    SubscriptionStatus current = vendor.getSubscriptionStatus();
    
    if (current == target) {
        return;  // Already in target state, nothing to do
    }
    
    if (current.canTransitionTo(target)) {
        transition(vendor, target, reason, externalEventId);
        return;
    }
    
    // Invalid transition ignored
    log.warn("Invalid transition ignored: {} → {}", current, target);
}
```

---

## 📊 Event Sequence Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│  INITIAL STATE: No vendor                                       │
└─────────────────────────────────────────────────────────────────┘

                          ↓

┌─────────────────────────────────────────────────────────────────┐
│  EVENT 1: checkout.session.completed                            │
│  ✅ Action: Create vendor, subscription, usage limits           │
│  📊 New state: vendor.status = TRIAL                            │
│             subscription.status = ACTIVE                        │
│             usage_limits initialized                            │
└─────────────────────────────────────────────────────────────────┘

                          ↓

┌─────────────────────────────────────────────────────────────────┐
│  EVENT 2: invoice.payment_succeeded (recurring)                 │
│  ✅ Action: Update lastPaymentAt, ensure ATIVA                  │
│  📊 New state: vendor.status = ATIVA                            │
│             vendor.lastPaymentAt = <NOW>                        │
│             subscription period renewed                         │
└─────────────────────────────────────────────────────────────────┘

                          ↓

┌─────────────────────────────────────────────────────────────────┐
│  EVENT 3: invoice.payment_succeeded (recurring again)           │
│  ✅ Action: Update timestamp again                              │
│  📊 State unchanged: ATIVA (already in correct state)           │
└─────────────────────────────────────────────────────────────────┘

                          ↓

┌─────────────────────────────────────────────────────────────────┐
│  EVENT 4: customer.subscription.deleted                         │
│  ✅ Action: Transition to CANCELADA                             │
│  📊 New state: vendor.status = CANCELADA                        │
│             All API access blocked                              │
└─────────────────────────────────────────────────────────────────┘

                          ↓

┌─────────────────────────────────────────────────────────────────┐
│  FINAL STATE: Subscription cancelled, all access revoked        │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🧪 Testing Idempotency

### Manual Test with Stripe CLI

```bash
# Simulate checkout completion
stripe trigger checkout.session.completed \
  --add checkout_session_id=cs_test_xxxxx

# Stripe sends webhook
# → Server processes and returns 200

# Stripe auto-retries (simulating network failure)
stripe trigger checkout.session.completed \
  --add checkout_session_id=cs_test_xxxxx

# → Server receives same webhook
# → findByStripeSubscriptionId() finds existing subscription
# → Returns with log warning (no duplicate created)

# ✅ Idempotency verified!
```

### Verification Queries

```sql
-- Check vendor created once
SELECT id, user_email, subscription_status 
FROM vendors 
WHERE user_email = 'user@example.com';
-- Should return exactly 1 row

-- Check subscription created once
SELECT id, stripe_subscription_id, status 
FROM subscriptions 
WHERE stripe_subscription_id = 'sub_12345';
-- Should return exactly 1 row

-- Check audit trail
SELECT from_status, to_status, reason, created_at
FROM vendor_subscription_audit
WHERE vendor_id = '550e8400-e29b-...'
ORDER BY created_at DESC;
-- Should show proper state transitions
```

---

## 📝 Log Pattern Reference

### All Three Events Together

```log
=== SCENARIO: Complete User Journey ===

[2026-03-09T10:30:00.123] [INFO] [StripeWebhookController] Stripe event received: checkout.session.completed
[2026-03-09T10:30:00.124] [INFO] [SubscriptionService] Processing Stripe checkout for user@example.com
[2026-03-09T10:30:00.456] [INFO] [SubscriptionService] Subscription activated for tenant=550e8400-e29b-41d4-a716-446655440000, email=user@example.com

  💰 User makes payment

[2026-03-09T10:30:15.789] [INFO] [StripeWebhookController] Stripe event received: invoice.payment_succeeded
[2026-03-09T10:30:15.790] [INFO] [SubscriptionService] Processing invoice.payment_succeeded event
[2026-03-09T10:30:15.801] [INFO] [SubscriptionService] Payment succeeded updated for vendor=550e8400-e29b-41d4-a716-446655440000, subscriptionId=sub_12345

  💳 Next month, automatic renewal

[2026-03-10T10:30:00.123] [INFO] [StripeWebhookController] Stripe event received: invoice.payment_succeeded
[2026-03-10T10:30:00.124] [INFO] [SubscriptionService] Processing invoice.payment_succeeded event
[2026-03-10T10:30:00.135] [INFO] [SubscriptionService] Payment succeeded updated for vendor=550e8400-e29b-41d4-a716-446655440000, subscriptionId=sub_12345

  ❌ User cancels subscription

[2026-03-11T14:22:00.456] [INFO] [StripeWebhookController] Stripe event received: customer.subscription.deleted
[2026-03-11T14:22:00.457] [INFO] [SubscriptionService] Processing customer.subscription.deleted event
[2026-03-11T14:22:00.468] [INFO] [SubscriptionService] Subscription cancelled for vendor=550e8400-e29b-41d4-a716-446655440000, stripeSubscriptionId=sub_12345

  🔒 All access revoked
```

---

## ✅ Implementation Checklist

- [x] Event 1: `checkout.session.completed` - CREATE ACCOUNT
  - [x] Idempotency via `findByStripeSubscriptionId()`
  - [x] Vendor creation
  - [x] Subscription creation
  - [x] Usage limits initialization
  - [x] Logging with email and tenant ID

- [x] Event 2: `invoice.payment_succeeded` - RENEW SUBSCRIPTION
  - [x] Deserialize Invoice
  - [x] Find vendor by Stripe IDs
  - [x] Update lastPaymentAt
  - [x] Transition to ATIVA
  - [x] Idempotent safe transition
  - [x] Logging with vendor ID and subscription ID

- [x] Event 3: `customer.subscription.deleted` - CANCEL
  - [x] Deserialize Subscription
  - [x] Find vendor by Stripe IDs
  - [x] Transition to CANCELADA
  - [x] Idempotent via safe transition
  - [x] Logging with vendor ID and subscription ID

---

## 🚀 Production Ready

All three Stripe webhook events are:
- ✅ Idempotent (safe to retry)
- ✅ Transactional (atomic operations)
- ✅ Well-logged (debugging visibility)
- ✅ Type-safe (instanceof checks)
- ✅ Error-handled (graceful returns)
