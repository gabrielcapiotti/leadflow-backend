# Health Check Endpoints Guide

## Overview

The **Health Check System** provides real-time visibility into the status of all critical system components. Spring Boot Actuator endpoints expose detailed health information for monitoring, alerting, and debugging.

This guide covers:
- Health indicator architecture
- Available health endpoints
- Integration with monitoring systems
- Custom health checks
- Best practices for health monitoring

## Health Endpoints

### Basic Health Check

```http
GET /api/actuator/health
```

**Response (Compact):**
```json
{
  "status": "UP"
}
```

### Detailed Health Check

```http
GET /api/actuator/health?include=all
```

**Response (Detailed):**
```json
{
  "status": "UP",
  "components": {
    "database": {
      "status": "UP",
      "details": {
        "service": "Database",
        "response_time_ms": 45,
        "pool_type": "HikariCP"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 1099511627776,
        "free": 750000000000,
        "threshold": 10485760,
        "exists": true
      }
    },
    "email": {
      "status": "UP",
      "details": {
        "service": "Email",
        "response_time_ms": 120,
        "smtp_host": "smtp.gmail.com"
      }
    },
    "livenessState": {
      "status": "UP"
    },
    "readinessState": {
      "status": "UP"
    },
    "stripe": {
      "status": "UP",
      "details": {
        "service": "Stripe",
        "api_version": "2023-06-15"
      }
    },
    "webhook": {
      "status": "UP",
      "details": {
        "service": "Webhook",
        "pending_count": 5,
        "succeeded_count": 156,
        "failed_count": 1
      }
    }
  }
}
```

## Custom Health Indicators

### 1. StripeHealthIndicator

**Purpose:** Validates Stripe API connectivity

**Check:** Performs lightweight API call to Stripe

**Status:**
- **UP** - API responding normally
- **DOWN** - API unreachable or auth failed

**Details:**
- `api_version` - Current Stripe API version
- `error` - Error message if failing
- `code` - Stripe error code if applicable

**Configuration:**
```yaml
management:
  endpoint:
    health:
      stripe:
        enabled: true
```

### 2. DatabaseHealthIndicator

**Purpose:** Validates database connectivity and performance

**Check:** Executes test query and measures response time

**Status:**
- **UP** - Connection working, response < 500ms
- **DEGRADED** - Connection working, response > 500ms
- **DOWN** - Connection failed

**Details:**
- `response_time_ms` - Query execution time
- `pool_type` - Connection pool type (HikariCP, Tomcat, DBCP)
- `error` - Error message if failing

**Configuration:**
```yaml
management:
  endpoint:
    health:
      db:
        enabled: true
```

### 3. EmailHealthIndicator

**Purpose:** Validates SMTP server accessibility

**Check:** Tests SMTP transport availability

**Status:**
- **UP** - SMTP server available, response < 2000ms
- **DEGRADED** - SMTP slow, response > 2000ms
- **DOWN** - SMTP server unreachable

**Details:**
- `response_time_ms` - Connection test time
- `smtp_host` - SMTP server hostname
- `error` - Error message if failing

**Configuration:**
```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
```

### 4. WebhookHealthIndicator

**Purpose:** Monitors webhook retry queue health

**Check:** Queries pending/failed webhook counts

**Status:**
- **UP** - Queue healthy, pending < threshold
- **DEGRADED** - Queue warning level, pending > threshold
- **DOWN** - Cannot query queue

**Details:**
- `pending_count` - Webhooks waiting for retry
- `succeeded_count` - Successfully processed webhooks
- `failed_count` - Permanently failed webhooks
- `threshold` - Warning threshold for pending

**Configuration:**
```yaml
webhook:
  health:
    max-pending: 100        # Alert if > 100 pending
    max-age-hours: 24       # Alert if oldest > 24 hours
```

## Configuration

### Enable Detailed Health

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus

  endpoint:
    health:
      show-details: always              # Show all details
      probes:
        enabled: true                   # Enable liveness/readiness probes
      components:
        enabled: true                   # Show all components

  health:
    defaults:
      enabled: true
    livenessState:
      enabled: true                     # /health/live
    readinessState:
      enabled: true                     # /health/ready
```

### Component-Specific Configuration

```yaml
management:
  health:
    # Individual component configuration
    db:
      enabled: true
    diskSpace:
      enabled: true
    livenessState:
      enabled: true
    readinessState:
      enabled: true
```

## Kubernetes Integration

### Liveness Probe

Checks if application is running (restart if failing):

```yaml
livenessProbe:
  httpGet:
    path: /api/actuator/health/live
    port: 8081
  initialDelaySeconds: 30
  periodSeconds: 10
  failureThreshold: 3
```

### Readiness Probe

Checks if application is ready for traffic:

```yaml
readinessProbe:
  httpGet:
    path: /api/actuator/health/ready
    port: 8081
  initialDelaySeconds: 10
  periodSeconds: 5
  failureThreshold: 3
```

## Monitoring & Alerting

### Prometheus Integration

Health status is exposed at `/api/actuator/prometheus`:

```promql
# Application is UP
application_up{app="leadflow-backend"} == 1

# Application is DOWN
application_up{app="leadflow-backend"} == 0
```

### Alert Rules

```yaml
groups:
  - name: health
    rules:
      # Application down
      - alert: ApplicationDown
        expr: application_up == 0
        for: 1m
        annotations:
          summary: "LeadFlow Backend is down"
          severity: critical

      # Database connection slow
      - alert: DatabaseSlow
        expr: db_health_response_time_ms > 500
        for: 5m
        annotations:
          summary: "Database response time > 500ms"
          severity: warning

      # Webhook queue backed up
      - alert: WebhookQueueBackup
        expr: webhook_pending_count > 100
        for: 10m
        annotations:
          summary: "Webhook queue has {{ $value }} pending"
          severity: warning

      # Too many permanent failures
      - alert: HighWebhookFailures
        expr: webhook_failed_count > 10
        for: 5m
        annotations:
          summary: "{{ $value }} permanently failed webhooks"
          severity: warning

      # Stripe API down
      - alert: StripeDown
        expr: stripe_health_status == 0
        for: 2m
        annotations:
          summary: "Stripe API unavailable"
          severity: critical
```

### Grafana Dashboard

Create dashboard panels:

```json
{
  "panels": [
    {
      "title": "Application Status",
      "targets": [{
        "expr": "application_up"
      }]
    },
    {
      "title": "Database Response Time",
      "targets": [{
        "expr": "db_health_response_time_ms"
      }]
    },
    {
      "title": "Webhook Queue Size",
      "targets": [{
        "expr": "webhook_pending_count"
      }]
    },
    {
      "title": "Component Status",
      "targets": [
        {"expr": "stripe_health_status"},
        {"expr": "db_health_status"},
        {"expr": "email_health_status"},
        {"expr": "webhook_health_status"}
      ]
    }
  ]
}
```

## Status Codes

### HTTP Status Mapping

| Health Status | HTTP Code | Meaning |
|---|---|---|
| UP | 200 | All systems operational |
| DEGRADED | 200 | Reduced functionality |
| DOWN | 503 | Service unavailable |
| UNKNOWN | 200 | Status unknown |
| OUT_OF_SERVICE | 503 | Component disabled |

### Status Details

```json
{
  "status": "DEGRADED",
  "components": {
    "database": {
      "status": "DEGRADED",
      "details": {
        "message": "Database responding slowly",
        "response_time_ms": 1200
      }
    }
  }
}
```

## Custom Health Indicator Example

Create a custom health check:

```java
@Component
public class CustomServiceHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // Check service status
            boolean isServiceHealthy = checkService();
            
            if (isServiceHealthy) {
                return Health.up()
                    .withDetail("service", "CustomService")
                    .withDetail("version", "1.0.0")
                    .build();
            } else {
                return Health.down()
                    .withDetail("service", "CustomService")
                    .withDetail("error", "Service degraded")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("service", "CustomService")
                .withException(e)
                .build();
        }
    }
}
```

## Troubleshooting

### Problem: Health Check Returns DEGRADED

**Symptoms:** `/health` shows DEGRADED status

**Cause:** One or more components responding slowly

**Solution:**
1. Identify slow component in `components` section
2. Check that component's logs
3. Address performance issue

Example:
```json
{
  "status": "DEGRADED",
  "components": {
    "database": {
      "status": "DEGRADED",
      "details": {
        "response_time_ms": 2500  // > 500ms threshold
      }
    }
  }
}
```

### Problem: Stripe Health Check Failing

**Error:** `code: "authentication_error"`

**Cause:** Invalid or missing Stripe API key

**Solution:**
```bash
# Verify STRIPE_SECRET_KEY is set
echo $STRIPE_SECRET_KEY

# Test with curl
curl -H "Authorization: Bearer $STRIPE_SECRET_KEY" \
  https://api.stripe.com/v1/customers?limit=1
```

### Problem: Database Health Check Timing Out

**Symptoms:** Health check hangs or times out

**Cause:** Database connection pool exhausted or locked

**Solution:**
1. Check database connection pool:
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20
         connection-timeout: 10000
   ```
2. Kill long-running queries:
   ```sql
   SELECT * FROM pg_stat_statements ORDER BY total_time DESC LIMIT 5;
   ```

### Problem: Email Health Check Fails

**Error:** "SMTP host not found"

**Cause:** Misconfigured SMTP server or DNS issue

**Solution:**
```bash
# Test SMTP connectivity
telnet smtp.gmail.com 587

# Verify configuration
grep -r "MAIL_HOST" .env
```

## Performance Considerations

### Health Check Frequency

```yaml
management:
  health:
    # Don't check too frequently
    defaults:
      enabled: true
```

Load balancers typically probe every 10-30 seconds. Configure indicators with timeouts:

- Stripe API check: 5 seconds timeout
- Database check: 5 seconds timeout
- SMTP check: 5 seconds timeout
- Webhook queue: 1 second timeout

### Caching

Health checks are cached for performance (typically 0ms, instant):

```java
@Component
public class CachedHealthIndicator implements HealthIndicator {
    private volatile Health lastHealth;
    private volatile long lastCheck;
    
    @Override
    public Health health() {
        long now = System.currentTimeMillis();
        // Cache for 30 seconds
        if (lastHealth != null && (now - lastCheck) < 30000) {
            return lastHealth;
        }
        
        lastHealth = performCheck();
        lastCheck = now;
        return lastHealth;
    }
}
```

## Best Practices

### 1. Use Appropriate Status Thresholds

```java
if (responseTime > 500) {
    return Health.degraded();  // Warning, not critical
} else if (responseTime > 5000) {
    return Health.down();      // Critical failure
}
```

### 2. Include Useful Context

```java
return Health.up()
    .withDetail("response_time_ms", duration)
    .withDetail("pool_size", poolSize)
    .withDetail("version", apiVersion)
    .build();
```

### 3. Handle Exceptions Gracefully

```java
try {
    return performCheck();
} catch (TimeoutException e) {
    return Health.down()
        .withDetail("error", "Health check timeout")
        .build();
} catch (Exception e) {
    return Health.down()
        .withException(e)
        .build();
}
```

### 4. Separate Liveness and Readiness

- **Liveness** (`/health/live`): Is the app running?
- **Readiness** (`/health/ready`): Is it ready for traffic?

Different components affect each probe differently.

## Summary

The Health Check system provides:
- ✅ **Real-time Visibility** - Know system status instantly
- ✅ **Kubernetes Integration** - Works with probes
- ✅ **Alerting** - Prometheus/Grafana dashboards
- ✅ **Debugging** - Detailed component information
- ✅ **Monitoring** - Track performance metrics
- ✅ **Recovery** - Auto-restart unhealthy containers

Key endpoints:
- `/api/actuator/health` - Overall status
- `/api/actuator/health/live` - Kubernetes liveness
- `/api/actuator/health/ready` - Kubernetes readiness
- `/api/actuator/health/{component}` - Individual component

By implementing comprehensive health checks, you ensure visibility into all critical system dependencies and can quickly detect and resolve issues.
