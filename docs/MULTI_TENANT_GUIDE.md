# Multi-tenant Webhook Isolation Guide

## Overview

The **Multi-tenant Webhook Isolation System** ensures complete data separation between customers (tenants). Each tenant's webhook events, metrics, and retry queues are completely isolated from other tenants, preventing data leaks and cross-contamination.

This guide covers:
- Tenant context management
- Webhook isolation mechanisms
- API usage with tenant isolation
- Metrics per tenant
- Security best practices
- Troubleshooting isolation violations

## Architecture

### Components

**1. TenantContext (Thread-local Storage)**
- Stores current tenant ID for the request
- Automatic cleanup after request processing
- Scoped context support with try-with-resources

**2. TenantFilter (Servlet Filter)**
- Extracts tenant ID from HTTP requests
- Sets tenant context at beginning of request
- Clears context automatically after request

**3. WebhookTenantValidator**
- Validates webhooks belong to tenant
- Enforces quota limits per tenant
- Detects isolation violations

**4. Data Model**
- `FailedWebhookEvent.tenantId` - Associates webhook with tenant
- Repository queries filtered by tenant
- Metrics aggregation per tenant

## Tenant ID Extraction

The `TenantFilter` extracts tenant ID from multiple sources (in priority order):

### 1. HTTP Header (Recommended for APIs)

```http
GET /api/billing/webhooks/failed HTTP/1.1
X-Tenant-ID: acme-corp-tenant-123
```

**Use Case:** REST API calls from backend services

### 2. JWT Token Claim (Recommended for Web Apps)

```json
{
  "sub": "user-456",
  "tenant_id": "acme-corp-tenant-123",
  "exp": 1647000000
}
```

**Use Case:** Frontend making authenticated API calls

### 3. URL Path (For Path-based Tenancy)

```http
GET /api/tenants/acme-corp-tenant-123/billing/webhooks/failed
```

**Use Case:** Multi-URL tenant routing

### 4. Request Attribute (For Internal Services)

```java
request.setAttribute("tenant_id", "acme-corp-tenant-123");
```

**Use Case:** Service-to-service communication

## TenantContext Usage

### Basic Usage

```java
// Set tenant context
TenantContext.setCurrentTenant("acme-corp-tenant-123");

try {
    // All database queries now scoped to this tenant:
    Page<FailedWebhookEvent> webhooks = webhookService.getPendingWebhooks(0, 20);
    // Only returns webhooks for acme-corp-tenant-123
} finally {
    TenantContext.clear();
}
```

### Try-with-Resources Pattern (Recommended)

```java
// Automatic scope management
try (TenantContextScope scope = TenantContext.withTenant("acme-corp-tenant-123")) {
    Page<FailedWebhookEvent> webhooks = webhookService.getPendingWebhooks(0, 20);
    // Scope automatically cleared here
}
```

### Getting Current Tenant

```java
String tenantId = TenantContext.getCurrentTenant();
// Throws IllegalStateException if none set

// Or with default
String tenantId = TenantContext.getCurrentTenantOrDefault("default-tenant");

// Check if set
if (TenantContext.hasTenantContext()) {
    // Tenant is set
}
```

## Webhook Isolation

### Data Isolation

Each tenant's webhooks are completely isolated:

```sql
-- Only shows webhooks for tenant:
SELECT * FROM failed_webhook_events 
WHERE tenant_id = 'acme-corp-tenant-123'
AND status = 'PENDING';
```

### Validation

Before processing a webhook, validate tenant ownership:

```java
@Autowired
private WebhookTenantValidator tenantValidator;

@PostMapping("/webhooks/{webhookId}/replay")
public ResponseEntity<?> replayWebhook(
    @PathVariable String webhookId,
    @RequestHeader("X-Tenant-ID") String tenantId) {
    
    // Validates webhook belongs to tenant
    tenantValidator.validateFailedWebhookTenant(webhookId, tenantId);
    
    // Safe to process
    return webhookService.replayWebhook(webhookId);
}
```

### Quota Enforcement

Prevent resource exhaustion by enforcing quotas per tenant:

```java
@Value("${tenant.webhook.max-pending:1000}")
private int maxPendingWebhooks;

public void storeFailedWebhook(String eventId, String tenantId, String data) {
    // Validate quota
    tenantValidator.validateWebhookQuota(tenantId, maxPendingWebhooks);
    
    // Store webhook with tenant association
    webhookService.storeFailedWebhook(eventId, tenantId, data);
}
```

## API Endpoints with Tenant Isolation

### View Tenant Webhooks

```http
GET /api/billing/webhooks/failed?page=0&size=20
X-Tenant-ID: acme-corp-tenant-123
```

**Response:** Only webhooks for acme-corp-tenant-123

### Replay Tenant Webhook

```http
POST /api/billing/webhooks/{webhookId}/replay
X-Tenant-ID: acme-corp-tenant-123
```

**Behavior:**
- Validates webhook belongs to tenant
- Returns 403 Forbidden if webhook from different tenant
- Replays only if ownership confirmed

### Get Tenant Stats

```http
GET /api/billing/webhooks/stats
X-Tenant-ID: acme-corp-tenant-123
```

**Response:**
```json
{
  "pendingCount": 5,
  "successCount": 156,
  "failedCount": 1,
  "inProgressCount": 0,
  "tenantId": "acme-corp-tenant-123"
}
```

## Metrics Isolation

### Per-Tenant Metrics

All metrics are tagged with tenant ID for isolation:

```
webhook_pending_total{tenant_id="acme-corp-tenant-123"} 5
webhook_pending_total{tenant_id="beta-features-789"} 3

webhook_succeeded_total{tenant_id="acme-corp-tenant-123"} 156
webhook_succeeded_total{tenant_id="beta-features-789"} 42

webhook_latency_seconds{tenant_id="acme-corp-tenant-123",le="0.5"} 140
webhook_latency_seconds{tenant_id="acme-corp-tenant-123",le="1.0"} 154
```

### Prometheus Queries

Query metrics per tenant:

```promql
# Pending webhooks for specific tenant
webhook_pending_total{tenant_id="acme-corp-tenant-123"}

# All pending webhooks across all tenants
webhook_pending_total

# Compare tenants
webhook_succeeded_total / webhook_pending_total
```

### Grafana Dashboard

Dashboard panels showing per-tenant:
- Pending webhook count
- Success rate
- Error rate
- Latency (P50, P95, P99)
- Quota usage

## Alerting with Tenant Awareness

### Tenant-specific Alerts

```yaml
- alert: HighWebhookPending
  expr: webhook_pending_total{tenant_id!=""} > 100
  for: 10m
  annotations:
    summary: "{{ $labels.tenant_id }}: {{ $value }} pending webhooks"
    tenant: "{{ $labels.tenant_id }}"

- alert: TenantWebhookQuotaExceeded
  expr: webhook_quota_used{tenant_id!=""} > 0.9
  for: 5m
  annotations:
    summary: "Tenant {{ $labels.tenant_id }} exceeded webhook quota"
    action: "Contact support to increase limit"
```

## Integration Points

### Stripe Webhook Processing

When Stripe webhook arrives:

```
1. Stripe POST → /stripe/webhook
   ↓
2. TenantFilter sets tenant context from header/JWT
   ↓
3. StripeWebhookController processes event
   ↓
4. Store in failed_webhook_events with tenant_id
   ↓
5. Query operations automatically filtered by tenant
```

### Alert Processing

When alert is triggered:

```
1. AlertMonitor checks metrics for all tenants
   ↓
2. Filters alerts by tenant ownership
   ↓
3. Routes notification to tenant-specific channels
   ↓
4. Logs alert with tenant context
```

### Manual Replay

When ops team replays webhook:

```
1. GET /api/billing/webhooks/{webhookId}
2. Extract tenant from header or token
3. Validate webhook belongs to tenant
4. If valid: Replay webhook within tenant context
5. If invalid: Return 403 Forbidden
```

## Configuration

### Application Properties

```yaml
tenant:
  # Webhook limits per tenant
  webhook:
    max-pending: 1000        # Max pending per tenant
    max-retries: 5           # Retry attempts per webhook
    batch-size: 10           # Webhooks per batch
  
  # Alerting per tenant
  alert:
    enabled: true
    per-tenant-channels: true
  
  # Metrics isolation
  metrics:
    isolate-by-tenant: true
    include-tenant-label: true
```

### Database Schema

```sql
-- Webhooks isolated by tenant
CREATE TABLE failed_webhook_events (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,  -- Tenant association
    stripe_event_id VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    -- ... other columns ...
    
    -- Indexes for tenant queries
    INDEX idx_tenant_status (tenant_id, status),
    INDEX idx_tenant_created (tenant_id, created_at),
    
    -- Unique constraint per tenant
    UNIQUE KEY unique_event_per_tenant (tenant_id, stripe_event_id)
);

-- Audit log isolated by tenant
CREATE TABLE webhook_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(36) NOT NULL,
    webhook_id VARCHAR(36) NOT NULL,
    action VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    
    INDEX idx_tenant_action (tenant_id, action),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);
```

## Best Practices

### 1. Always Extract Tenant ID

Never assume a default tenant. Always validate tenant context:

```java
// BAD - Uses first tenant
Page<FailedWebhookEvent> webhooks = webhookRepository.findAll(pageable);

// GOOD - Scoped to current tenant
String tenantId = TenantContext.getCurrentTenant();
Page<FailedWebhookEvent> webhooks = 
    webhookRepository.findByTenantId(tenantId, pageable);
```

### 2. Validate Tenant Ownership

Always verify a resource belongs to the requesting tenant:

```java
// BAD - No validation
FailedWebhookEvent webhook = repo.findById(webhookId).get();
webhook.setStatus(SUCCEEDED);

// GOOD - Validates tenant ownership
tenantValidator.validateFailedWebhookTenant(webhookId, tenantId);
FailedWebhookEvent webhook = repo.findById(webhookId).get();
webhook.setStatus(SUCCEEDED);
```

### 3. Use Tenant Scope Automatically

Leverage Spring Data filtering:

```java
// In repository interface
@Query("SELECT w FROM FailedWebhookEvent w " +
       "WHERE w.tenantId = :tenantId AND w.status = 'PENDING'")
Page<FailedWebhookEvent> findPendingForTenant(
    @Param("tenantId") String tenantId,
    Pageable pageable
);
```

### 4. Log Tenant Context

Always log with tenant context for audit trails:

```java
log.info("Webhook processed for tenant {}: {}",  
         TenantContext.getCurrentTenant(), 
         webhookId);
```

### 5. Clean Up Context

Always clear tenant context to prevent leaks:

```java
try {
    TenantContext.setCurrentTenant(tenantId);
    // ... operations ...
} finally {
    TenantContext.clear();  // Always clean up
}
```

## Security Considerations

### SQL Injection Prevention

Use parameterized queries (Spring Data does this automatically):

```java
// Safe - uses parameter binding
findByTenantId(tenantId, pageable)

// UNSAFE - string concatenation
query("SELECT * FROM webhooks WHERE tenant_id = '" + tenantId + "'")
```

### Cross-Tenant Access Prevention

Validate on every multi-tenant operation:

```java
public void updateWebhook(String webhookId, String tenantId, WebhookUpdate update) {
    // Validates ownership before update
    tenantValidator.validateFailedWebhookTenant(webhookId, tenantId);
    webhook.setFailureReason(update.getReason());
    save(webhook);
}
```

### Tenant ID Spoofing Prevention

Always extract tenant ID from:
- JWT claims (signed by server)
- Headers (validated by filter)
- NOT from user input directly

### Audit Trail

Log all cross-tenant access attempts:

```java
public void logTenantViolation(String webhookId, String attemptedTenant, String actualTenant) {
    log.warn("SECURITY: Cross-tenant access attempt " +
             "webhook={}, attempted={}, actual={}",
             webhookId, attemptedTenant, actualTenant);
    // Alert security team
}
```

## Troubleshooting

### Problem: "No tenant context set"

**Symptom:** `IllegalStateException: No tenant context set for current thread`

**Causes:**
1. TenantFilter not configured
2. Missing X-Tenant-ID header
3. JWT doesn't include tenant_id claim

**Solution:**
- Verify TenantFilter is registered in filter chain
- Check request includes tenant ID
- Validate JWT structure

### Problem: Cross-Tenant Access

**Symptom:** User A can see User B's webhooks

**Cause:** Missing tenant validation

**Solution:**
```java
// Add validation
tenantValidator.validateFailedWebhookTenant(webhookId, tenantId);
```

### Problem: Metrics Mixed Between Tenants

**Symptom:** Alerts trigger for wrong tenant

**Cause:** Missing tenant tags in metrics

**Solution:**
```java
meter.counter("webhook_count", "tenant_id", tenantId).increment();
```

### Problem: Slow Queries Across All Tenants

**Symptom:** Queries timeout

**Cause:** Missing indexes on tenant_id

**Solution:**
```sql
CREATE INDEX idx_tenant_status ON failed_webhook_events(tenant_id, status);
CREATE INDEX idx_tenant_created ON failed_webhook_events(tenant_id, created_at);
```

## Performance Optimization

### Index Strategy

```sql
-- Fast lookups per tenant
CREATE INDEX idx_tenant_pending 
ON failed_webhook_events(tenant_id, status, next_retry_at);

-- Fast aggregation per tenant
CREATE INDEX idx_tenant_event_type 
ON failed_webhook_events(tenant_id, event_type, status);
```

### Query Optimization

```java
// Efficient - uses index
Page<FailedWebhookEvent> webhooks = 
    repo.findByTenantIdAndStatus(tenantId, PENDING, pageable);

// Inefficient - full table scan
List<Event> allEvents = repo.findAll();
filter by tenantId in application
```

### Partition Strategy (Optional)

For very large deployments, partition by tenant:

```sql
CREATE TABLE failed_webhook_events (
    -- ... columns ...
) PARTITION BY LIST (HASH(MONTH(created_at))) (
    PARTITION p_acme VALUES IN ('acme-corp-tenant-123'),
    PARTITION p_beta VALUES IN ('beta-features-789'),
    PARTITION p_other VALUES IN (DEFAULT)
);
```

## Testing Multi-tenant Isolation

### Unit Test Example

```java
@Test
public void testWebhookIsolation() {
    try (TenantContextScope scope = TenantContext.withTenant("tenant-a")) {
        webhookService.storeFailedWebhook(...);
    }
    
    try (TenantContextScope scope = TenantContext.withTenant("tenant-b")) {
        Page<FailedWebhookEvent> webhooks = webhookService.getPendingWebhooks(0, 10);
        assertEquals(0, webhooks.getTotalElements());  // Shouldn't see tenant-a's webhooks
    }
}
```

### Integration Test Example

```java
@Test
public void testCrossTenantAccessDenied() {
    String webhook = storeWebhookForTenant("tenant-a");
    
    assertThrows(WebhookTenantViolationException.class, () -> {
        tenantValidator.validateFailedWebhookTenant(webhook, "tenant-b");
    });
}
```

## Summary

Multi-tenant webhook isolation provides:
- ✅ **Complete Data Isolation** - Each tenant sees only their data
- ✅ **Automatic Enforcement** - Filter and validation layers
- ✅ **Audit Trail** - All access logged with tenant context
- ✅ **Performance** - Indexed queries per tenant
- ✅ **Security** - Prevents cross-tenant access
- ✅ **Scalability** - Supports thousands of tenants

By implementing this system, you ensure that:
1. No data leaks between customers
2. Quotas are enforced per tenant
3. Metrics are isolated and accurate
4. Compliance requirements are met
5. Debugging is straightforward with tenant context
