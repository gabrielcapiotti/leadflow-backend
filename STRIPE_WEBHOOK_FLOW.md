# Stripe Webhook Integration Flow - Checkout Completion

## 📊 Fluxo Completo: Stripe Payment → Provisioning

```
┌─────────────────────────────────────────────────────────────────┐
│  1. STRIPE CHECKOUT                                             │
│  ✓ User completes payment                                       │
│  ✓ Session created with customer email, customerId, subscriptionId
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│  2. STRIPE SENDS WEBHOOK                                        │
│  ✓ POST /stripe/webhook                                         │
│  ✓ Event type: checkout.session.completed                       │
│  ✓ Payload + Stripe-Signature header                            │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│  3. STRIPE WEBHOOK CONTROLLER                                   │
│  ✓ StripeWebhookController.handleWebhook()                      │
│  ✓ Validates signature via StripeService.constructWebhookEvent()│
│  ✓ Routes by event type (switch statement)                      │
│  ✓ Calls handleCheckoutCompleted(event)                         │
│    - Extracts EventDataObjectDeserializer                       │
│    - Verifies object is instanceof Session                      │
│    - Calls subscriptionService.activateAccount(session)         │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│  4. SUBSCRIPTION SERVICE - ACTIVATE ACCOUNT                     │
│  ✓ SubscriptionService.activateAccount(Session session)         │
│    @Transactional - atomic operation                            │
│                                                                  │
│  Step 4.1: IDEMPOTENCY CHECK                                    │
│    - Check if stripeSubscriptionId already exists               │
│    - If exists: log warning, return (webhook retry protection)  │
│    - If not: continue with account activation                   │
│                                                                  │
│  Step 4.2: CREATE VENDOR/TENANT                                 │
│    - Call VendorService.createVendor(email)                     │
│    - Returns: Vendor with UUID id (= tenantId)                  │
│    - Vendor fields set:                                         │
│      * userEmail (normalized)                                   │
│      * nomeVendedor (from email local part)                     │
│      * whatsappVendedor ("0000000000")                          │
│      * slug (auto-generated)                                    │
│      * subscriptionStatus (TRIAL)                               │
│                                                                  │
│  Step 4.3: GET ACTIVE PLAN                                      │
│    - Call PlanService.getActivePlan()                           │
│    - Returns: Plan entity with pricing/limits                   │
│                                                                  │
│  Step 4.4: CREATE SUBSCRIPTION RECORD                           │
│    - Create new Subscription entity                             │
│    - Set fields:                                                │
│      * tenantId = vendor.getId()                                │
│      * stripeCustomerId = session.getCustomer()                 │
│      * stripeSubscriptionId = session.getSubscription()         │
│      * email = session.getCustomerEmail()                       │
│      * plan = Plan (from step 4.3)                              │
│      * status = SubscriptionStatus.ACTIVE                       │
│      * startedAt = LocalDateTime.now()                          │
│    - Save via SubscriptionRepository.save()                     │
│                                                                  │
│  Step 4.5: INITIALIZE USAGE LIMITS                              │
│    - Call UsageService.initializeUsage(tenantId, plan)          │
│    - Creates usage limit records based on plan:                 │
│      * leads available                                          │
│      * users available                                          │
│      * AI executions available                                  │
│                                                                  │
│  Result: All data committed atomically @Transactional           │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│  5. FULL PROVISIONING (OPTIONAL)                                │
│  ✓ StripeService.processCheckoutCompletedEvent()                │
│  ✓ BillingTenantProvisioningService.provisionFromCheckout()     │
│    - Creates PostgreSQL schema                                  │
│    - Creates admin user                                         │
│    - Initializes tenant data                                    │
│    - Sets up multi-tenancy isolation                            │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│  6. RESPONSE TO STRIPE                                          │
│  ✓ Return: 200 OK "received"                                    │
│  ✓ Stripe marks webhook as delivered                            │
│  ✓ No retry needed (idempotent operation)                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🏗️ Implementation Details

### SubscriptionRepository
```java
@Repository
public interface SubscriptionRepository
        extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    // Used for idempotency check - prevents duplicate accounts
}
```

### StripeWebhookController
```java
private void handleCheckoutCompleted(Event event, String payload) {
    EventDataObjectDeserializer deserializer = 
            event.getDataObjectDeserializer();

    StripeObject object = deserializer.getObject().orElse(null);

    if (!(object instanceof Session)) {
        log.error("Invalid Stripe object received");
        return;
    }

    Session session = (Session) object;
    
    // Delegate all business logic to service layer
    subscriptionService.activateAccount(session);
    
    // Also trigger full provisioning (schema, admin user, etc.)
    stripeService.processCheckoutCompletedEvent(event, payload);
}
```

### SubscriptionService - activateAccount()
```java
@Transactional
public void activateAccount(Session session) {
    
    // Validate input
    if (session == null) {
        throw new IllegalArgumentException("Session cannot be null");
    }
    
    String email = session.getCustomerEmail();
    String stripeCustomerId = session.getCustomer();
    String stripeSubscriptionId = session.getSubscription();
    
    // Idempotency check - prevent duplicate processing
    Optional<Subscription> existing = 
        subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
    
    if (existing.isPresent()) {
        log.warn("Subscription already processed. stripeSubscriptionId={}", 
                stripeSubscriptionId);
        return;
    }
    
    // Step 1: Create Vendor/Tenant
    Vendor vendor = vendorService.createVendor(email);
    UUID tenantId = vendor.getId();
    
    // Step 2: Get active plan
    Plan plan = planService.getActivePlan();
    
    // Step 3: Create subscription record
    Subscription subscription = new Subscription();
    subscription.setTenantId(tenantId);
    subscription.setStripeCustomerId(stripeCustomerId);
    subscription.setStripeSubscriptionId(stripeSubscriptionId);
    subscription.setEmail(email);
    subscription.setPlan(plan);
    subscription.setStatus(SubscriptionStatus.ACTIVE);
    subscription.setStartedAt(LocalDateTime.now());
    
    subscriptionRepository.save(subscription);
    
    // Step 4: Initialize usage limits
    usageService.initializeUsage(tenantId, plan);
    
    log.info("Subscription activated for tenant={}, email={}", tenantId, email);
}
```

### VendorService - createVendor()
```java
@Transactional
public Vendor createVendor(String email) {
    Vendor vendor = new Vendor();
    vendor.setUserEmail(normalizeEmail(email));
    vendor.setNomeVendedor(localPart(email));
    vendor.setWhatsappVendedor("0000000000");
    vendor.setSlug(generateSlug(email));
    vendor.setSubscriptionStatus(SubscriptionStatus.TRIAL);
    return vendorRepository.save(vendor);
}
```

---

## ✅ Result After Payment Confirmation

When Stripe confirms the payment:

1. ✅ **Tenant Created**: Vendor with UUID id = tenantId
2. ✅ **Vendor Created**: Full vendor record with email and metadata
3. ✅ **Subscription Registered**: Subscription entity with Stripe IDs
4. ✅ **Usage Limits Initialized**: Lead, user, AI execution quotas set
5. ✅ **Idempotent**: Multiple webhook retries won't create duplicates

---

## 🔐 Safety & Consistency

- **Transactional**: All operations atomic (all-or-nothing)
- **Idempotent**: Checks `stripeSubscriptionId` before processing
- **Webhook Validation**: Signature verified by StripeService
- **Error Handling**: Exceptions rolled back, logged for debugging
- **Rate Limited**: Webhook endpoint protected but bypassed for Stripe

---

## 📝 Data Flow Summary

```
Session {
  customerEmail: "user@example.com"
  customer: "cus_xxxxx"  (Stripe Customer ID)
  subscription: "sub_xxxxx"  (Stripe Subscription ID)
}
    ↓ activateAccount()
Vendor {
  id: UUID (tenant identifier)
  userEmail: "user@example.com"
  subscriptionStatus: TRIAL → ATIVA
}
    ↓
Subscription {
  tenantId: vendor.id
  stripeCustomerId: "cus_xxxxx"
  stripeSubscriptionId: "sub_xxxxx"
  status: ACTIVE
  plan: Plan
}
    ↓
UsageLimit {
  tenantId: vendor.id
  leads: plan.leadLimit
  users: plan.userLimit
  aiExecutions: plan.aiLimit
}
```
