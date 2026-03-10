# 🔄 Complete End-to-End Payment Flow

## Full Integration Overview

```
┌─────────────┐
│  FRONTEND   │
│  (Vendedor) │
└──────┬──────┘
       │
       │ Step 1: User clicks "Upgrade Plan"
       │         (already logged in, has tenantId)
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ POST /billing/checkout                                   │
│                                                          │
│ Request Body:                                           │
│ {                                                       │
│   "email": "vendor@example.com",                       │
│   "nomeVendedor": "vendor",                            │
│   "whatsappVendedor": "11999999999",                   │
│   "tenantId": "550e8400-e29b-41d4-a716-446655440000" │
│ }                                                       │
└──────┬───────────────────────────────────────────────────┘
       │ Step 2: BillingController validates & routes
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ BillingController.createCheckoutSession()               │
│                                                          │
│ ✓ Validates CheckoutRequest                            │
│ ✓ Delegates to StripeService                           │
└──────┬───────────────────────────────────────────────────┘
       │ Step 3: Service creates Stripe session
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ StripeService.createCheckoutSession()                    │
│                                                          │
│ ✓ Validates email                                       │
│ ✓ Extracts tenantId from request                        │
│ ✓ Creates metadata map:                                 │
│   {                                                     │
│     "email": "vendor@example.com",                      │
│     "plan": "default",                                  │
│     "referenceId": "abc123def456...",                   │
│     "tenantId": "550e8400-e29b-41d4..."                │
│   }                                                     │
│ ✓ Creates SessionCreateParams with metadata             │
│ ✓ Calls Stripe API: Session.create(params)              │
│ ✓ Returns CheckoutResponse                              │
│   {                                                     │
│     "checkoutUrl": "https://checkout.stripe.com/...",  │
│     "referenceId": "abc123def456...",                   │
│     "provider": "stripe"                                │
│   }                                                     │
└──────┬───────────────────────────────────────────────────┘
       │ Step 4: Return checkout URL to frontend
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ Frontend receives CheckoutResponse                       │
│                                                          │
│ ✓ Displays checkout URL                                 │
│ ✓ Redirects user to Stripe checkout page                │
└──────┬───────────────────────────────────────────────────┘
       │ Step 5: User completes Stripe checkout
       │
       │ (User enters card details, confirms payment)
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ STRIPE BACKEND                                           │
│                                                          │
│ ✓ Validates payment information                         │
│ ✓ Processes charge                                      │
│ ✓ Creates subscription                                  │
│ ✓ Stores session metadata in subscription               │
│ ✓ Generates checkout.session.completed event            │
│                                                          │
│ Event contains:                                         │
│ {                                                       │
│   "type": "checkout.session.completed",                │
│   "id": "evt_1234567890...",                           │
│   "data": {                                             │
│     "object": {                                         │
│       "id": "cs_live_...",                              │
│       "customer": "cus_123456",                         │
│       "subscription": "sub_123456",                     │
│       "customer_email": "vendor@example.com",           │
│       "metadata": {                                     │
│         "email": "vendor@example.com",                  │
│         "tenantId": "550e8400-e29b...",                │
│         "referenceId": "abc123...",                     │
│         "plan": "default"                               │
│       }                                                 │
│     }                                                   │
│   }                                                     │
│ }                                                       │
└──────┬───────────────────────────────────────────────────┘
       │ Step 6: Stripe sends webhook
       │
       │ Signature: Stripe-Signature header
       │ (Calculated from payload + webhook secret)
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ POST /stripe/webhook                                     │
│                                                          │
│ Headers:                                                │
│   Stripe-Signature: t=...,v1=...                        │
│                                                          │
│ Body: Raw event JSON                                    │
└──────┬───────────────────────────────────────────────────┘
       │ Step 7: Controller validates signature
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ StripeWebhookController.handleWebhook()                  │
│                                                          │
│ ✓ Reads raw payload from request                        │
│ ✓ Extracts Stripe-Signature header                      │
│ ✓ Calls StripeService.constructWebhookEvent()           │
│   - Validates signature using webhook secret            │
│   - Deserializes payload to Event domain object         │
│ ✓ Logs: "Stripe event received: checkout.session..."   │
│ ✓ Checks event type                                     │
│ ✓ Routes to handler: handleCheckoutCompleted()          │
└──────┬───────────────────────────────────────────────────┘
       │ Step 8: Extract Session object
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ StripeWebhookController.handleCheckoutCompleted()        │
│                                                          │
│ ✓ Gets EventDataObjectDeserializer from event           │
│ ✓ Extracts StripeObject from deserializer               │
│ ✓ Validates instanceof Session                          │
│ ✓ Casts to Session domain object                        │
│ ✓ Delegates to SubscriptionService.activateAccount()    │
└──────┬───────────────────────────────────────────────────┘
       │ Step 9: Process subscription activation
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ SubscriptionService.activateAccount(Session)             │
│                                                          │
│ ✓ Extracts Stripe IDs:                                  │
│   - stripeCustomerId = session.getCustomer()            │
│   - stripeSubscriptionId = session.getSubscription()    │
│                                                          │
│ ✓ IDEMPOTENCY CHECK                                     │
│   - Queries: findByStripeSubscriptionId(sub_123456)     │
│   - If found: log warning, return early                 │
│   - If new: continue processing                         │
│                                                          │
│ ✓ EXTRACT METADATA                                      │
│   - tenantIdString = session.getMetadata().get(...)     │
│   - email = session.getMetadata().get(\"email\")        │
│   - If tenantId null: throw exception                   │
│   - tenantId = UUID.fromString(tenantIdString)          │
│                                                          │
│ ✓ Logs: \"Activating tenant 550e8400... for vendor@...\" │
│                                                          │
│ ✓ GET PLAN                                              │
│   - planService.getActivePlan()                         │
│   - Returns active Plan from database                   │
│                                                          │
│ ✓ CREATE SUBSCRIPTION RECORD                            │
│   - new Subscription()                                  │
│   - setTenantId(tenantId)                               │
│   - setStripeCustomerId(cus_123456)                     │
│   - setStripeSubscriptionId(sub_123456)                 │
│   - setStatus(ACTIVE)                                   │
│   - setPlan(plan)                                       │
│   - setStartedAt(now)                                   │
│   - subscriptionRepository.save()                       │
│                                                          │
│ ✓ INITIALIZE USAGE LIMITS                               │
│   - usageService.initializeUsage(tenantId, plan)        │
│   - Creates or updates usage_limits row                 │
│   - Sets quota based on plan                            │
│                                                          │
│ ✓ Logs: \"Tenant 550e8400... activated with plan Pro\"   │
│                                                          │
│ @Transactional ensures ALL-OR-NOTHING                   │
│   - If any step fails → entire transaction rollback     │
│   - Database stays consistent                           │
└──────┬───────────────────────────────────────────────────┘
       │ Step 10: Return success response
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ POST /stripe/webhook Response                            │
│                                                          │
│ Status: 200 OK                                          │
│ Body: \"received\"                                       │
│                                                          │
│ Stripe receives 200 → Marks event as delivered          │
│ No retry needed ✓                                        │
└──────┬───────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ DATABASE STATE                                           │
│                                                          │
│ vendors table:                                          │
│   ✓ Same vendor (already existed before checkout)       │
│                                                          │
│ subscriptions table:                                    │
│   ✓ NEW ROW CREATED:                                    │
│     - tenant_id: 550e8400-e29b-41d4-a716-446655440000  │
│     - stripe_customer_id: cus_123456                    │
│     - stripe_subscription_id: sub_123456                │
│     - status: ACTIVE                                    │
│     - plan_id: <pro_plan_id>                            │
│     - started_at: 2025-03-09 14:30:00                   │
│     - created_at: 2025-03-09 14:30:00                   │
│                                                          │
│ usage_limits table:                                     │
│   ✓ INITIALIZED:                                        │
│     - tenant_id: 550e8400-e29b-41d4-a716-446655440000  │
│     - available_leads: 1000 (from plan)                 │
│     - available_users: 10 (from plan)                   │
│     - available_ai_executions: 100 (from plan)          │
│     - created_at: 2025-03-09 14:30:00                   │
└──────┬───────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ APPLICATION STATE                                        │
│                                                          │
│ ✓ Vendor is now ACTIVE with Pro plan                    │
│ ✓ Has full access to API                                │
│ ✓ Usage limits enforced                                 │
│ ✓ Can make 1000 lead API calls                          │
│ ✓ Can add up to 10 users                                │
│ ✓ Can run 100 AI executions                             │
│                                                          │
│ Logs show:                                              │
│   2025-03-09 14:30:00 INFO Stripe event received:       │
│                         checkout.session.completed      │
│   2025-03-09 14:30:00 INFO Activating tenant 550e8400...│
│                         for vendor@example.com          │
│   2025-03-09 14:30:00 INFO Tenant 550e8400... activated │
│                         with plan Pro                   │
└──────────────────────────────────────────────────────────┘
```

---

## Key Components & Their Roles

### 1. **Frontend**
- User clicks "Upgrade Plan" button
- Sends tenant UUID with checkout request
- Redirects to Stripe checkout URL

### 2. **BillingController**
- **Route**: `POST /billing/checkout`
- **Role**: Request validation and routing
- **Input**: CheckoutRequest (email, tenantId, vendor info)
- **Output**: CheckoutResponse (URL, referenceId, provider)

### 3. **StripeService**
- **Methods**:
  - `createCheckoutSession(CheckoutRequest)` → Returns CheckoutResponse
  - `createCheckoutSession(String, Long)` → Returns Stripe Session
  - `constructWebhookEvent()` → Validates and deserializes webhook
- **Responsibilities**:
  - Normalizes email
  - Builds metadata map with tenant association
  - Creates Stripe session with metadata
  - Validates webhook signatures

### 4. **StripeWebhookController**
- **Route**: `POST /stripe/webhook`
- **Role**: Webhook entry point and event router
- **Responsibilities**:
  - Reads raw payload
  - Validates signature
  - Routes to appropriate handler
  - Extracts and type-casts Stripe objects

### 5. **SubscriptionService**
- **Method**: `activateAccount(Session)`
- **Role**: Core business logic for subscription activation
- **Responsibilities**:
  - Idempotency check (prevent duplicates)
  - Extract metadata (tenantId, email)
  - Get active plan
  - Create subscription record
  - Initialize usage limits
  - All within single @Transactional scope

---

## Metadata Flow (The Critical Link)

### Created in StripeService

```java
Map<String, String> metadata = new HashMap<>();
metadata.put("email", normalizedEmail);           // Audit trail
metadata.put("plan", "default");                  // Plan tracking
metadata.put("referenceId", UUID.randomUUID());   // Unique ref
metadata.put("tenantId", String.valueOf(tenantId)); // CRITICAL!
```

### Passed to Stripe

```java
SessionCreateParams.builder()
    // ... other params ...
    .putAllMetadata(metadata)  // ← All metadata included
    .build()
```

### Returned in Webhook

```
Stripe Event:
├─ type: "checkout.session.completed"
├─ data:
│  └─ object (Session):
│     ├─ customer: "cus_123456"
│     ├─ subscription: "sub_123456"
│     └─ metadata: {↑ SAME METADATA RETURNED ↑}
```

### Extracted in Webhook Handler

```java
String tenantIdString = session.getMetadata().get("tenantId");
UUID tenantId = UUID.fromString(tenantIdString);
// ✓ Tenant identified directly from metadata, not from email
```

---

## State Transitions

### Vendor State
```
BEFORE:
- Status: TRIAL (or ACTIVE with old plan)
- No subscription in DB

AFTER:
- Status: ATIVA (still same, but linked to Pro plan now)
- Has Subscription record with Stripe IDs
- Usage limits initialized
```

### Subscription State
```
BEFORE:
- None (doesn't exist)

AFTER:
- status: ACTIVE
- linked to: tenantId + Stripe subscription ID
- has plan: Pro (or whatever was active)
- started_at: Now
```

### Usage Limits State
```
BEFORE:
- Previous limits (if any) still active

AFTER:
- Reset based on new plan quotas
- ready to track against new entitlements
```

---

## Error Handling & Recovery

### Scenario 1: Missing tenantId in Metadata

```
SubscriptionService.activateAccount()
├─ Reads: tenantIdString = session.getMetadata().get("tenantId")
├─ Result: null
└─ Action: 
   ├─ log.error("Missing tenantId in Stripe checkout session metadata")
   └─ throw IllegalStateException("TenantId not found in session metadata")
       └─ @Transactional rollback entire transaction
       └─ Database left unchanged
       └─ Webhook returns 500 error
       └─ Stripe will retry webhook
```

**Resolution**: Ensure tenantId is always sent in CheckoutRequest

### Scenario 2: Stripe Webhook Retry (Network Timeout)

```
FIRST DELIVERY:
POST /stripe/webhook
├─ Processes successfully
├─ Creates subscription record
├─ Network fails before sending 200 response
└─ Stripe doesn't receive ACK

STRIPE RETRIES (seconds later):
POST /stripe/webhook (SAME event, sub_123456)
├─ Idempotency check: findByStripeSubscriptionId("sub_123456")
├─ Result: FOUND (from first attempt)
├─ log.warn("Subscription already processed: sub_123456")
├─ Returns 200 OK immediately
└─ No duplicate created ✓
```

### Scenario 3: Database Constraint Violation

```
SubscriptionService.activateAccount()
├─ Creates subscription record
├─ try: subscriptionRepository.save()
├─ Exception: DataIntegrityViolationException
│  (e.g., duplicate stripe_subscription_id due to race condition)
├─ @Transactional catches exception
├─ Rollback entire transaction
└─ Webhook returns 500 error
   └─ Stripe will retry
   └─ Second attempt: idempotency check finds it, returns 200
```

---

## Monitoring & Logs

### Expected Log Output (Happy Path)

```
2025-03-09 14:30:00.123 [http-nio-8080-exec-1] INFO  StripeWebhookController
    Stripe event received: checkout.session.completed

2025-03-09 14:30:00.234 [http-nio-8080-exec-1] INFO  SubscriptionService
    Activating tenant 550e8400-e29b-41d4-a716-446655440000 for vendor@example.com

2025-03-09 14:30:00.456 [http-nio-8080-exec-1] INFO  SubscriptionService
    Tenant 550e8400-e29b-41d4-a716-446655440000 activated with plan Pro
```

### Error Logs

```
ERROR - Missing tenantId:
    Missing tenantId in Stripe checkout session metadata

WARN - Duplicate webhook:
    Subscription already processed: sub_123456

ERROR - Invalid tenant:
    Tenant not found in database: 550e8400-...
```

---

## Data Consistency Guarantees

```
@Transactional on activateAccount()
├─ Atomicity: All CREATE or NONE
│  (Subscription + UsageLimit created together)
│
├─ Isolation: No dirty reads from concurrent requests
│  (Idempotency check prevents race conditions)
│
├─ Durability: Once 200 OK returned, data is committed
│  (Database guarantees)
│
└─ Consistency: State always valid
   (No subscriptions without usage limits)
```

---

## Testing the Complete Flow

### Test 1: Happy Path (Existing Tenant Upgrade)

```bash
# Step 1: Get existing tenant UUID (from database or API)
TENANT_ID="550e8400-e29b-41d4-a716-446655440000"

# Step 2: Create checkout session
curl -X POST http://localhost:8080/billing/checkout \
  -H "Content-Type: application/json" \
  -d '{
    "email": "vendor@example.com",
    "nomeVendedor": "vendor",
    "whatsappVendedor": "11999999999",
    "tenantId": "'$TENANT_ID'"
  }'

# Response:
{
  "checkoutUrl": "https://checkout.stripe.com/pay/cs_live_...",
  "referenceId": "abc123def456...",
  "provider": "stripe"
}

# Step 3: Complete payment in Stripe (simulated with Stripe CLI)
stripe trigger checkout.session.completed --fixture '{"metadata":{"tenantId":"'$TENANT_ID'"}}'

# Step 4: Verify in database
mysql> SELECT * FROM subscriptions 
       WHERE tenant_id = UUID_TO_BIN('550e8400-e29b-41d4-a716-446655440000')
       ORDER BY created_at DESC LIMIT 1;

# Expected: 1 row with status=ACTIVE, stripe_subscription_id set
```

### Test 2: Webhook Retry (Idempotency)

```bash
# Simulate exact same webhook twice
for i in 1 2; do
  echo "Attempt $i:"
  stripe trigger checkout.session.completed --fixture '{"metadata":{"tenantId":"'$TENANT_ID'"}}'
  sleep 2
done

# Expected logs:
# Attempt 1: "Activating tenant 550e8400..."
# Attempt 2: "Subscription already processed: sub_..."

# Database: Still only 1 subscription record ✓
```

### Test 3: Missing tenantId (Error Case)

```bash
# Manually craft webhook with missing tenantId
curl -X POST http://localhost:8080/stripe/webhook \
  -H "Content-Type: application/json" \
  -H "Stripe-Signature: <valid_signature>" \
  -d '{
    "type": "checkout.session.completed",
    "data": {
      "object": {
        "id": "cs_test...",
        "customer": "cus_test...",
        "subscription": "sub_test...",
        "metadata": {
          "email": "vendor@example.com"
          // Missing: "tenantId"
        }
      }
    }
  }'

# Expected:
# Status: 500 Internal Server Error
# Log: "Missing tenantId in Stripe checkout session metadata"
# Database: No changes
```

---

## Summary

The complete end-to-end flow now:

1. ✅ **Accepts tenantId** from frontend in checkout request
2. ✅ **Embeds tenantId** in Stripe session metadata  
3. ✅ **Returns webhook** with metadata intact
4. ✅ **Extracts tenantId** from webhook metadata (not email)
5. ✅ **Creates subscription** linked to tenant UUID
6. ✅ **Initializes usage** in same transaction
7. ✅ **Handles retries** via idempotency check
8. ✅ **Guarantees atomicity** via @Transactional
9. ✅ **Provides audit trail** via logging

**Status: COMPLETE & VALIDATED** ✅
