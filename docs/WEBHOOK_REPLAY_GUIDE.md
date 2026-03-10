# Webhook Replay Functionality Guide

## Overview

The **Webhook Replay System** ensures that no webhook events are lost due to temporary failures. Failed webhook events are automatically stored in the database and retried according to an exponential backoff schedule.

This guide covers:
- How webhooks are stored when they fail
- Automatic retry mechanism with exponential backoff
- Manual replay endpoints
- Monitoring and statistics
- Best practices for webhook handling

## Architecture

### Components

**1. FailedWebhookEvent (Entity)**
- Stores failed webhook events in the database
- Tracks retry count, status, and next retry time
- Indexed on `status`, `created_at`, and `next_retry_at` for efficient queries

**2. FailedWebhookRepository (Data Access)**
- JPA repository for CRUD operations
- Specialized queries for finding retryable webhooks
- Statistics methods for monitoring

**3. WebhookReplayService (Business Logic)**
- Stores failed webhooks when processing fails
- Automatically retries failed webhooks on schedule
- Calculates exponential backoff delays
- Tracks retry statistics

**4. WebhookReplayController (REST API)**
- Endpoints for viewing failed webhooks
- Manual replay endpoints
- Statistics endpoints

## Automatic Retry Mechanism

### Exponential Backoff Schedule

When a webhook fails, it's not immediately retried. Instead, it waits according to this schedule:

```
Attempt #1 (Initial): Immediate
Attempt #2 (Retry #1): 1 minute later
Attempt #3 (Retry #2): 5 minutes later
Attempt #4 (Retry #3): 30 minutes later
Attempt #5 (Retry #4): 2 hours later
Attempt #6+ (Retry #5+): 12 hours later (if max not reached)
```

**Default Max Retries:** 5 (configurable via `webhook.replay.max-retries`)

### Process Flow

```
Webhook arrives
    ↓
Processing fails (e.g., database error)
    ↓
Store in failed_webhook_events table with PENDING status
    ↓
Scheduled task runs every 30 seconds
    ↓
Find all PENDING webhooks where nextRetryAt ≤ now
    ↓
Retry up to batch-size webhooks
    ↓
If successful: Mark SUCCEEDED, store succeededAt timestamp
    ↓
If failed: 
    - Increment retryCount
    - Store failure reason
    - If retryCount < maxRetries: Calculate nextRetryAt with backoff
    - If retryCount ≥ maxRetries: Mark FAILED_PERMANENT
```

### Webhook Status States

| Status | Meaning | Next Action |
|--------|---------|-------------|
| PENDING | Waiting to be retried | Will retry at nextRetryAt |
| IN_PROGRESS | Currently being processed | Timeout after 60 seconds |
| SUCCEEDED | Successfully processed | None (complete) |
| FAILED_PERMANENT | Max retries exceeded | Manual action needed |

## REST API Endpoints

### View Pending Webhooks

Get webhooks waiting for retry:

```http
GET /api/billing/webhooks/failed?page=0&size=20
```

**Response:**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "stripeEventId": "evt_1Ivv0F2eZvKYlo2CwWMz4Hs3",
      "eventType": "charge.succeeded",
      "eventData": "{\"id\":\"evt_1Ivv0F2eZvKYlo2CwWMz4Hs3\",...}",
      "failureReason": "Connection timeout",
      "retryCount": 2,
      "maxRetries": 5,
      "status": "PENDING",
      "nextRetryAt": "2024-03-10T15:30:00Z",
      "originalReceivedAt": "2024-03-10T14:15:00Z",
      "createdAt": "2024-03-10T14:15:30Z",
      "updatedAt": "2024-03-10T15:10:00Z",
      "succeededAt": null,
      "tenantId": null
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "currentPage": 0
}
```

### View Permanently Failed Webhooks

Get webhooks that exceeded max retries:

```http
GET /api/billing/webhooks/failed/permanent?page=0&size=20
```

### View Recent Failures

Get webhooks that failed in the last 24 hours:

```http
GET /api/billing/webhooks/failed/recent?page=0&size=20
```

### Manually Replay a Webhook

Force immediate retry for a specific webhook:

```http
POST /api/billing/webhooks/{webhookId}/replay
```

**Example:**
```bash
curl -X POST https://api.example.com/api/billing/webhooks/550e8400-e29b-41d4-a716-446655440000/replay
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "stripeEventId": "evt_1Ivv0F2eZvKYlo2CwWMz4Hs3",
  "eventType": "charge.succeeded",
  "retryCount": 0,
  "status": "PENDING",
  "nextRetryAt": "2024-03-10T15:10:00Z",
  "failureReason": "Manual replay requested"
}
```

### Get Retry Statistics

Get overview of webhook queue:

```http
GET /api/billing/webhooks/stats
```

**Response:**
```json
{
  "pendingCount": 5,
  "successCount": 156,
  "failedCount": 3,
  "inProgressCount": 1
}
```

### Delete Webhook

Remove a webhook from the queue (use with caution):

```http
DELETE /api/billing/webhooks/{webhookId}
```

## Configuration

### Application Properties

Add to `application.yml`:

```yaml
webhook:
  replay:
    max-retries: 5                    # Max retry attempts (default: 5)
    batch-size: 10                    # Webhooks processed per cycle (default: 10)
    check-interval: 30000             # Check interval in ms (default: 30s)
```

### Database Migration

Create the `failed_webhook_events` table:

```sql
CREATE TABLE failed_webhook_events (
    id VARCHAR(36) PRIMARY KEY,
    stripe_event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    event_data LONGTEXT NOT NULL,
    failure_reason TEXT NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 5,
    status VARCHAR(20) NOT NULL,
    next_retry_at TIMESTAMP NOT NULL,
    original_received_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    succeeded_at TIMESTAMP NULL,
    tenant_id VARCHAR(36) NULL,
    
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_next_retry (next_retry_at)
);
```

## Integration Points

### Storing Failed Webhooks

When webhook processing fails, store it for retry:

```java
@RestController
@RequestMapping("/webhook")
public class WebhookController {
    
    @Autowired
    private WebhookReplayService replayService;
    
    @PostMapping("/stripe")
    public ResponseEntity<?> handleWebhook(@RequestBody String payload) {
        try {
            // Process webhook
            webhookProcessor.process(payload);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // Store for retry
            replayService.storeFailedWebhook(
                eventId,
                eventType,
                payload,
                e.getMessage()
            );
            return ResponseEntity.status(202).build();  // Accepted, will retry
        }
    }
}
```

### Response Codes

| Code | Meaning |
|------|---------|
| 202 | Webhook stored, will retry |
| 200 | Webhook processed successfully |
| 500 | Server error (will retry) |

## Monitoring

### Key Metrics to Watch

1. **Pending Count** - Should stay low (< 10)
   - If growing, there's a systemic issue

2. **Failed Count** - Should stay very low (< 5)
   - Each failed webhook needs investigation

3. **Success Rate** - Should be > 99%
   - Target: 1 out of every 100 webhooks retried successfully

### Prometheus Metrics

```
webhook_pending_total        # Current pending count
webhook_succeeded_total      # Successfully retried webhooks
webhook_failed_total         # Webhooks that exceeded max retries
webhook_pending_age_seconds  # Age of oldest pending webhook
```

### Alerting Rules

```yaml
# Alert if too many pending webhooks
- alert: HighWebhookPending
  expr: webhook_pending_total > 20
  for: 10m
  annotations:
    summary: "{{ $value }} webhooks pending retry"

# Alert if too many permanently failed
- alert: HighWebhookFailures
  expr: webhook_failed_total > 10
  for: 5m
  annotations:
    summary: "{{ $value }} webhooks permanently failed"

# Alert if old pending webhooks
- alert: OldPendingWebhooks
  expr: webhook_pending_age_seconds > 3600
  for: 5m
  annotations:
    summary: "Pending webhooks are {{ $value }}s old"
```

## Troubleshooting

### Problem: High Pending Count

**Symptoms:** Many webhooks stuck in PENDING status

**Investigation Steps:**
1. Check application logs for error patterns
2. Review failure reasons in database:
   ```sql
   SELECT failure_reason, COUNT(*) as count
   FROM failed_webhook_events
   WHERE status = 'PENDING'
   GROUP BY failure_reason
   ORDER BY count DESC;
   ```
3. Check if system resources are constrained:
   - Database connection pool
   - Memory usage
   - Network connectivity

**Solutions:**
- Fix underlying issue causing failures
- Manually replay webhooks once fixed:
  ```bash
  curl -X POST https://api.example.com/api/billing/webhooks/{id}/replay
  ```

### Problem: Webhooks Stuck in IN_PROGRESS

**Symptoms:** Webhooks show IN_PROGRESS status for > 1 minute

**Cause:** Processing was interrupted (crash, timeout)

**Solution:**
Create a cleanup job to reset stuck webhooks:

```java
@Scheduled(fixedRate = 60000)
public void cleanupStuckWebhooks() {
    Instant oneMinuteAgo = Instant.now().minusSeconds(60);
    webhookRepository.findByStatusAndUpdatedAtBefore(
        WebhookStatus.IN_PROGRESS, 
        oneMinuteAgo
    ).forEach(webhook -> {
        webhook.setStatus(WebhookStatus.PENDING);
        webhook.setFailureReason("Stuck in IN_PROGRESS, resetting");
        webhookRepository.save(webhook);
    });
}
```

### Problem: MaxRetries Not Respected

**Symptoms:** Webhooks retried more than max times

**Cause:** Multiple instances running or race condition

**Solution:**
Add distributed lock before updating status:

```java
@Transactional
@Scheduled(fixedRate = 30000)
public void replayFailedWebhooks() {
    try (LockProvider.Lock lock = lockProvider.lock(
            "webhook-replay",
            Duration.ofSeconds(25),
            Duration.ofMinutes(1))) {
        // Process webhooks
    }
}
```

## Best Practices

### 1. Idempotent Webhook Processing

Always process webhooks idempotently. Multiple attempts should produce the same result:

```java
// BAD - Not idempotent
webhook.forEach(event -> {
    account.balance += event.amount;  // Will add twice if retried
    account.save();
});

// GOOD - Idempotent
if (!isEventProcessed(event.id)) {
    account.balance += event.amount;
    markEventProcessed(event.id);
    account.save();
}
```

### 2. Fast Failure Detection

Fail fast when you can't process a webhook:

```java
// Check resources before processing
if (!isDatabaseAvailable()) {
    throw new TemporaryException("Database unavailable");
}

if (!hasEnoughMemory()) {
    throw new TemporaryException("Insufficient memory");
}

// Then process
processWebhook(payload);
```

### 3. Detailed Failure Reasons

Store specific failure reasons for debugging:

```java
// BAD
storeFailedWebhook(..., "Error");

// GOOD
storeFailedWebhook(..., 
    "Database connection pool exhausted: " + 
    "active=20, max=20: " +
    e.getClass().getSimpleName());
```

### 4. Regular Cleanup

Periodically clean up old succeeded webhooks to save storage:

```java
@Scheduled(cron = "0 0 2 * * *")  // 2 AM daily
public void cleanupOldSuccessful() {
    Instant ninetyDaysAgo = Instant.now().minus(Duration.ofDays(90));
    webhookRepository.deleteByStatusAndSucceededAtBefore(
        WebhookStatus.SUCCEEDED,
        ninetyDaysAgo
    );
}
```

### 5. Manual Replay Workflow

For permanently failed webhooks:

1. **Investigate** - Check failure reason
2. **Fix** - Address root cause
3. **Verify** - Confirm fix works
4. **Replay** - Manually trigger retry
5. **Verify** - Confirm successful processing
6. **Clean** - Delete webhook if unrecoverable

## Performance Tuning

### Batch Size Optimization

```yaml
webhook:
  replay:
    batch-size: 20  # Process up to 20 per cycle
    check-interval: 30000  # Check every 30 seconds
```

**Throughput:** 20 webhooks every 30 seconds = 40 webhooks/minute

**Tuning Guide:**
- Increase `batch-size` to process more per cycle
- Decrease `check-interval` to check more often
- Target: < 100 pending webhooks at any time

### Database Optimization

Ensure indexes are created:

```sql
CREATE INDEX idx_status ON failed_webhook_events(status);
CREATE INDEX idx_next_retry ON failed_webhook_events(next_retry_at);
CREATE INDEX idx_stripe_event_id ON failed_webhook_events(stripe_event_id);
```

### Query Optimization

Prefer specific queries over full scans:

```java
// GOOD - Uses index
findRetryableWebhooks(now, pageable);

// BAD - Full table scan
findAll();
```

## Security Considerations

### 1. Sensitive Data

Webhook payloads may contain sensitive information (credit card tokens, etc.). 

```java
// Log safely
log.info("Webhook processed: {}", webhookId);  // NOT: log.info(..., payload);

// Encrypt at rest
@Column(columnDefinition = "VARBINARY(4000)")
@Convert(converter = EncryptedStringConverter.class)
private String eventData;
```

### 2. Access Control

Protect replay endpoints with authentication:

```java
@PostMapping("/{webhookId}/replay")
@PreAuthorize("hasRole('ADMIN')")  // Require authentication
public ResponseEntity<FailedWebhookEvent> replayWebhook(
    @PathVariable String webhookId) {
    // ...
}
```

### 3. Audit Logging

Track who replayed webhooks manually:

```java
@Transactional
public FailedWebhookEvent manualReplay(String webhookId, String userId) {
    FailedWebhookEvent event = findById(webhookId);
    event.setManuallyReplayedBy(userId);
    event.setManuallyReplayedAt(Instant.now());
    return save(event);
}
```

## Summary

The Webhook Replay system provides:
- ✅ **Zero Loss**: Failed webhooks are stored and retried
- ✅ **Exponential Backoff**: Intelligent retry delays
- ✅ **Manual Control**: Endpoints for ops team
- ✅ **Monitoring**: Statistics and metrics
- ✅ **Scalability**: Batch processing and database indexes

By implementing this system, you ensure business continuity and data integrity even when facing temporary failures.
