# Troubleshooting Guide for LeadFlow Backend

## Quick Diagnosis

### Step 1: Is the API responding?

```bash
curl -v https://api.leadflow.io/api/actuator/health
```

**Expected Response:**
```json
{"status":"UP"}
```

**If no response:** → Section "Application Not Responding"
**If 5xx error:** → Section "Server Errors"
**If 4xx error:** → Section "Authentication Issues"
**If slow:** → Section "Performance Degradation"

---

## Common Issues & Solutions

### Application Not Responding

#### Check 1: Is the pod running?

```bash
kubectl get pods -n production -l app=leadflow-backend
```

**Output should have:**
- STATUS: Running (not CrashLoopBackOff, Pending, ImagePullBackOff)
- READY: 1/1 (not 0/1 or stuck)
- RESTARTS: < 5 (low number is good)

**If not running:**

```bash
# Check what's wrong
kubectl describe pod <pod-name> -n production

# Look for:
# - ImagePullBackOff: Wrong image, registry auth, or image doesn't exist
# - CrashLoopBackOff: Application won't start
# - Pending: Waiting for resources
# - FailedScheduling: Not enough cluster resources
```

**Fix:**
```bash
# For image issues
kubectl set image deployment/leadflow-backend \
  leadflow=docker.io/leadflow/backend:fix-tag \
  -n production

# For resource issues
# Edit deployment resources in values.yaml
helm upgrade leadflow ./helm-charts/leadflow \
  --set resources.limits.memory=2Gi

# For startup errors
kubectl logs <pod-name> -n production --previous
# Look for: ClassNotFound, Port already in use, Config missing
```

#### Check 2: Is the service reachable?

```bash
# From within cluster
kubectl exec -it <pod-name> -n production -- \
  curl localhost:8080/api/actuator/health

# From outside
curl https://api.leadflow.io/api/actuator/health

# Through load balancer
kubectl get svc -n production
kubectl describe svc leadflow-service -n production
```

**Common issues:**
- Service port mismatch (8080 vs 8443)
- Ingress not configured
- Network policy blocking traffic
- DNS not resolving

---

### "Connection refused" Error

**Symptoms:**
```
curl: (7) Failed to connect to api.leadflow.io port 443: Connection refused
or
java.sql.DriverManager.getConnection() → Connection refused
```

**Case 1: Can't connect to API**

```bash
# Check if service is running
kubectl get svc -n production
# Should show loadbalancer with EXTERNAL-IP

# Check ingress
kubectl get ingress -n production
# Should have rules pointing to leadflow-service:8080

# Check DNS (for DNS name)
nslookup api.leadflow.io
# Should resolve to the loadbalancer IP

# Manual test
curl http://<loadbalancer-ip>/api/actuator/health
# Should work with direct IP
```

**Solution:**
```bash
# Recreate service
kubectl delete svc leadflow-service -n production
kubectl apply -f deployment/service.yaml

# Or reconfigure ingress
kubectl apply -f deployment/ingress.yaml

# Test after fix
kubectl get svc -n production
curl https://api.leadflow.io/api/actuator/health
```

**Case 2: Can't connect to Database**

```bash
# Check database pod
kubectl get pods -n production | grep postgres
# Should show 1/1 Running

# Test database connection
kubectl exec -it leadflow-backend-xxx -n production -- \
  curl localhost:5432
# (Will show connection reset, which means port is reachable)

# Better test: From Java application
kubectl logs <pod-name> -n production | grep -i "database"
# Look for: org.postgresql.util.PSQLException

# Check from kubectl
kubectl port-forward -n production svc/postgres 5432:5432 &
psql -h localhost -U postgres -d leadflow
# If this works, DB is fine
```

**Solution:**
```bash
# Verify credentials in secrets
kubectl get secret leadflow-secrets -n production -o yaml
# Check: DATABASE_USER, DATABASE_PASSWORD, DATABASE_URL

# Verify network connectivity
kubectl exec -it <postgres-pod> -n production -- \
  pg_isready -h postgres -p 5432

# Check connectivity from app pod
kubectl run -it --image=postgres:15 debug \
  --restart=Never -n production -- \
  psql -h postgres -U postgres
```

---

### High Error Rate (> 10%)

**Diagnosis:**

```bash
# Get error count from Prometheus
curl 'http://prometheus:9090/api/v1/query?query=rate(http_requests_total{status=~"5.."}[5m])'

# Or from charts
# Grafana → Dashboards → LeadFlow Backend
# Look at "Error Rate % (5m)" panel

# See which endpoint is failing
curl 'http://prometheus:9090/api/v1/query?query=rate(http_requests_total{status="500"}[5m]) by (path)'
```

**Check logs:**

```bash
# Stream ERROR logs
kubectl logs -n production deployment/leadflow-backend | grep ERROR

# Example error:
# ERROR com.leadflow.backend.controller.WebhookController - Error processing webhook
# caused by: com.stripe.exception.ApiConnectionException: Unexpected error...

# Get request that failed
kubectl logs <pod-name> -n production | grep -A 10 "ERROR"
# Look for: stack trace with method names

# Find all 500 errors in last hour
kubectl logs -n production \
  --since=1h \
  deployment/leadflow-backend | grep "500"
```

**Common error patterns:**

```bash
# Pattern 1: Stripe API Timeout
ERROR StripeWebhookProcessor - com.stripe.exception.ApiConnectionException
→ Check Stripe API status: https://status.stripe.com
→ Check circuit breaker: /api/actuator/health/stripe
→ Enable fallback mode (automatic)

# Pattern 2: Database errors
ERROR LeadRepository - org.postgresql.util.PSQLException
→ Check database connection pool
→ Check database resource usage
→ See "High Database Load" section

# Pattern 3: Out of Memory
ERROR - java.lang.OutOfMemoryError: Java heap space
→ See "Memory Exhaustion" section

# Pattern 4: External API timeout
ERROR HttpClientService - SocketTimeoutException
→ Check which API timing out
→ Review timeout configuration
→ Consider increasing timeout or parallelizing calls
```

---

### Memory Exhaustion

**Symptoms:**

```
Pod repeatedly restarting every few minutes
Container memory usage: 1800Mi / 1900Mi limit
Error: java.lang.OutOfMemoryError: Java heap space
```

**Diagnosis:**

```bash
# Current memory usage
kubectl top pods -n production -l app=leadflow-backend
# NAME       CPU    MEMORY
# leadflow-0 250m   1850Mi  <- Getting close to 1900Mi limit

# Memory trend (last hour in Grafana)
# Graph should show: Gradually increasing, then restart, repeat

# Check what's consuming memory
# Option 1: Check GC logs
kubectl logs <pod-name> -n production | grep "GC overhead"
# GC taking > 98% of time = running out of memory

# Option 2: Connect heap dump analyzer
# (See advanced section below)
```

**Quick fixes:**

```bash
# Temporary: Increase memory limit
kubectl set resources deployment leadflow-backend \
  --limits=memory=2Gi \
  -n production

# Verify
kubectl get deployment leadflow-backend -n production -o yaml | grep memory

# Restart pods to apply
kubectl rollout restart deployment/leadflow-backend -n production
```

**Root cause investigation:**

```bash
# Enable heap dump on OOM
kubectl set env deployment/leadflow-backend \
  -n production \
  JAVA_OPTS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heap.bin"

# Restart and wait for OOM
kubectl rollout restart deployment/leadflow-backend -n production

# When OOM happens:
kubectl cp <pod-name>:/tmp/heap.bin ./heap.bin -n production

# Analyze with:
# jhat -Xmx512m heap.bin
# → Open http://localhost:7000
# → Find "Histogram" of object types
# → Look for unexpectedly large collections
```

**Common memory leaks:**

```bash
# In application logs, look for:
# 1. Size of caches: "Cache size: 50000 entries"
#    → Should be stable, not growing
#    → Check if cache eviction working

# 2. Threads growing: "Active threads: 500"
#    → Should be < 100
#    → Possible thread leak

# 3. Pending tasks: "Queue size: 10000"
#    → Should be < 1000
#    → Async tasks not processing

# Fix: Deploy patch and monitor
git log --oneline | grep -i memory
git show <commit-sha>
# Review the changes that added memory consumption
```

**Prevention:**

```bash
# Set memory limits to be proportional to actual needs
# Current: Limit 1.9Gi, Request 1Gi
# Not enough headroom for GC

# Better:
kubectl set resources deployment/leadflow-backend \
  --limits=memory=2Gi \
  --requests=memory=1.5Gi \
  -n production

# Monitor memory consistently:
# Alert when memory > 1.5Gi (80% of 1.9Gi limit)
```

---

### Database Issues

#### Can't Connect

```bash
# Check if database pod exists
kubectl get pods -n production | grep postgres
# Status should be Running 1/1

# Check credentials
kubectl get secret leadflow-db-secret -n production -o yaml | grep DATABASE
```

#### Slow Queries

**Symptoms:**
```
Response time: Normal API queries taking 5+ seconds
Database CPU: High (visible in monitoring)
```

**Diagnosis:**

```bash
# Connect to database directly
kubectl port-forward postgres-0 5432:5432 -n production &
psql -h localhost -U postgres

# Check active queries
SELECT pid, query, query_start FROM pg_stat_activity
  WHERE query NOT LIKE '%idle%'
  ORDER BY query_start DESC;

# Check slow query log (if enabled)
SELECT query, mean_time, calls FROM pg_stat_statements
  ORDER BY mean_time DESC LIMIT 10;

# Explain slow query
EXPLAIN ANALYZE SELECT ... FROM subscriptions WHERE user_id = 123;
# Look for: Sequential Scans instead of Index Scans
```

**Solutions:**

```bash
# 1. Add index if sequential scan
CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);

# 2. Analyze query plan
ANALYZE;
EXPLAIN ANALYZE SELECT ...;

# 3. Increase shared_buffers if many queries
# In PostgreSQL config: shared_buffers = 256MB (default)

# 4. Check locks
SELECT * FROM pg_locks WHERE NOT granted;
# If results, there's lock contention

# 5. Check connection count
SELECT count(*) FROM pg_stat_activity;
# Should be < 80 of 100 max

# Apply migration
kubectl exec -it postgres-0 -n production -- \
  psql -U postgres leadflow < index_migration.sql

# Verify improvement
# Run same query again, check query_start time
```

#### Out of Disk Space

**Symptoms:**
```
Error: Could not write block... disk full
Pod: postgres in pending/failing state
```

**Check:**

```bash
# Disk usage
kubectl exec -it postgres-0 -n production -- \
  df -h /var/lib/postgresql

# Database size
kubectl exec -it postgres-0 -n production -- \
  du -sh /var/lib/postgresql/data

# Large tables
kubectl exec -it postgres-0 -n production -- \
  psql -U postgres -c \
  "SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename))
   FROM pg_tables ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC LIMIT 10;"
```

**Solutions:**

```bash
# 1. Clean up old data (if safe)
DELETE FROM audit_logs WHERE created_at < now() - interval '90 days';
VACUUM ANALYZE;

# 2. Expand storage
# Edit PVC size (StatefulSet)
kubectl patch pvc postgres-data -n production \
  -p "{\"spec\":{\"resources\":{\"requests\":{\"storage\":\"250Gi\"}}}}"

# Wait for volume expansion
kubectl get pvc postgres-data -n production --watch

# Verify
kubectl exec -it postgres-0 -n production -- df -h /var/lib/postgresql

# 3. Monitor going forward
# Alert: Disk usage > 80%
# Run cleanup jobs weekly
```

---

### Webhook Issues

#### Webhooks Not Being Received

**Check:**

```bash
# Check if webhook handler is running
kubectl logs -n production deployment/leadflow-backend | grep "Webhook handler"

# Check failed webhook table
kubectl exec -it postgres-0 -n production -- \
  psql -U postgres leadflow -c \
  "SELECT COUNT(*), status FROM failed_webhook_events GROUP BY status;"

# Example output:
# count | status
# ------|--------
#   150 | PENDING
#    10 | SUCCEEDED
#    20 | FAILED_PERMANENT

# This is expected: Pending webhooks will retry
```

**If many FAILED_PERMANENT:**

```bash
# Check what the errors are
SELECT event_id, event_data, error_message FROM failed_webhook_events
  WHERE status = 'FAILED_PERMANENT'
  ORDER BY created_at DESC
  LIMIT 5;

# Common errors:
# 1. "Invalid JSON format" → Webhook data is malformed
# 2. "Signature verification failed" → Stripe signing key wrong
# 3. "Database constraint violation" → Data conflict in system

# Fix: Depending on error type
# If signature verification: Check STRIPE_WEBHOOK_SECRET in secrets
# If data conflict: Manually resolve in database
# If JSON parsing: Contact Stripe support
```

#### Manual Webhook Replay

**When needed:**
- After fixing webhook handler
- After recovering from service outage
- After fixing data inconsistency

**Steps:**

```bash
# 1. Find failed webhooks
kubectl exec -it postgres-0 -n production -- \
  psql -U postgres -c \
  "SELECT id FROM failed_webhook_events WHERE status = 'PENDING' LIMIT 5;"

# 2. Replay one webhook (for testing)
curl -X POST https://api.leadflow.io/api/billing/webhooks/replay \
  -H "Authorization: Bearer $API_KEY" \
  -H "Content-Type: application/json" \
  -d "{\"webhookId\": \"<id-from-above>\"}"

# 3. Check result
SELECT id, status FROM failed_webhook_events WHERE id = '<id>';

# If SUCCESS: Continue with all webhooks
# If FAILED: Debug the specific webhook

# 4. Replay all pending webhooks
curl -X POST https://api.leadflow.io/api/billing/webhooks/retry-all \
  -H "Authorization: Bearer $API_KEY"

# 5. Monitor progress
# Check /api/billing/webhooks/stats for counts
```

---

### Performance Degradation

**Symptoms:**
```
Response times: Normal 200ms → 2000ms (10x slower)
P95 Latency: 300ms → 5000ms
Request queue building up
Users reporting slow load
```

**Diagnosis:**

```bash
# Check request latency percentiles
# Prometheus query:
# http_request_duration_seconds_bucket{quantile="0.95"}

# Check if recent change caused it
git log --oneline -10 | head
# Look for recent deployments around time of slowdown

# Check database query times
SELECT query, mean_time, calls FROM pg_stat_statements
  ORDER BY mean_time DESC LIMIT 5;
# Look for: mean_time increasing

# Check CPU usage
kubectl top nodes
kubectl top pods -n production -l app=leadflow-backend

# Check network I/O
# Look for: Network bandwidth saturated
```

**Common causes:**

```
1. Database issue
   → Check slow queries section above
   → May need indexing or query optimization

2. External API slow
   → Check Stripe/Email service status
   → Increase timeout if service is slow but reliable
   → Consider circuit breaker (already implemented)

3. Memory pressure/GC
   → Check: Java process GC time increasing
   → Solution: Increase memory or optimize object creation

4. Resource contention
   → Check: CPU 100%, memory 100%, disk I/O 100%
   → Solution: Scale up pods or optimize code

5. Bad deployment
   → Check: Which commit caused slowdown
   → Solution: Rollback and investigate
```

**Fix steps:**

```bash
# Step 1: Quick mitigation
# Scale to more replicas (temporary)
kubectl scale deployment leadflow-backend --replicas=5 -n production

# Step 2: Identify root cause (steps above)

# Step 3: Deploy permanent fix
# If database: Add index, optimize query
# If resource: Increase limits, optimize code
# If external API: Add caching, increase timeout

# Step 4: Rollback if can't quickly fix
kubectl rollout undo deployment/leadflow-backend -n production

# Step 5: Verify improvement
# Monitor latency graph return to normal
```

---

### Authentication Issues

#### "401 Unauthorized"

**Causes & Fixes:**

```bash
# 1. Missing Authorization header
Request: curl https://api.leadflow.io/api/billing
Fix: curl -H "Authorization: Bearer $TOKEN" https://api.leadflow.io/api/billing

# 2. Invalid/expired token
Check: Token expiration (in JWT claim)
Fix: Request new token from /api/auth/login

# 3. Token signed with wrong key
Check: JWT_SECRET or OAuth provider
Fix: Verify secret matches in application.yml

# 4. Wrong scope/permissions
Check: What scopes does token have
Fix: Request token with correct scope

# 5. OIDC/OAuth provider down
Check: https://identity-provider.example.com/health
Fix: Wait for provider recovery, or use backup token
```

#### "403 Forbidden"

**This means:**
- User authenticated but not authorized for this resource

**Check:**

```bash
# Get current user
curl -H "Authorization: Bearer $TOKEN" https://api.leadflow.io/api/users/me

# Check if user is admin (required for some endpoints)
# GET /api/users/me should show: "roles": ["ADMIN"]

# Check if accessing wrong tenant's resource
# URL: /api/tenants/12345/webhooks/view
# User's tenant: 67890
# Result: 403 (trying to access different tenant)

# Solution:
# 1. Use correct tenant ID
# 2. Request admin access from account owner
# 3. Verify token scopes
```

---

### Networking Issues

#### DNS Resolution

```bash
# Test DNS from pod
kubectl exec -it <pod-name> -n production -- \
  nslookup api.leadflow.io

# Expected: Returns the service's IP

# If fails:
# Check CoreDNS pod
kubectl get pods -n kube-system | grep coredns

# DNS logs
kubectl logs -n kube-system -l k8s-app=kube-dns
```

#### Timeouts

```bash
# Connection timeout
Error: connection timeout after 5s
→ Probably network firewall or service down

# Solution:
# 1. Check if destination is running
kubectl get svc -n production

# 2. Check network policy
kubectl get networkpolicy -n production

# 3. Check firewall rules (cloud provider)
# AWS: Security Groups → Inbound rules
# Azure: Network Security Groups → Inbound rules
# GCP: Firewall rules → check egress

# 4. Test connectivity
kubectl run -it --image=alpine debug --restart=Never -- \
  wget -O- http://leadflow-service:8080/health
```

---

## Advanced Debugging

### Connect to Running Pod

```bash
# Get interactive shell
kubectl exec -it <pod-name> -n production -- /bin/bash

# Inside container:
ps aux          # See running processes
netstat -tlnp   # See listening ports
cat /etc/hosts  # See host file
env | grep JAVA # See Java environment

# Check application logs
tail -100 /var/log/application.log

# Monitor real-time
tail -f /var/log/application.log | grep ERROR
```

### Enable Debug Logging

```bash
# Temporarily enable DEBUG logs
kubectl set env deployment/leadflow-backend \
  -n production \
  LOGGING_LEVEL_COM_LEADFLOW=DEBUG

# or

kubectl patch deployment leadflow-backend -n production --type merge \
  -p '{"spec":{"template":{"spec":{"containers":[{"name":"leadflow","env":[{"name":"LOGGING_LEVEL_COM_LEADFLOW","value":"DEBUG"}]}]}}}}'

# Restart
kubectl rollout restart deployment/leadflow-backend -n production

# View logs
kubectl logs -n production deployment/leadflow-backend | grep "DEBUG"

# Disable when done
kubectl set env deployment/leadflow-backend \
  -n production \
  LOGGING_LEVEL_COM_LEADFLOW=INFO

kubectl rollout restart deployment/leadflow-backend -n production
```

### Network Tracing

```bash
# Capture traffic to/from pod
kubectl exec -it <pod-name> -n production -- \
  tcpdump -i eth0 port 5432 -w /tmp/db.pcap

# Copy file out
kubectl cp <pod-name>:/tmp/db.pcap ./db.pcap -n production

# Analyze with Wireshark
wireshark db.pcap
```

### Metrics Queries (Prometheus)

```bash
# Request rate (requests/sec)
rate(http_requests_total[1m])

# Error rate (%)
rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m]) * 100

# P95 latency (seconds)
histogram_quantile(0.95, http_request_duration_seconds_bucket)

# Database connection pool usage (%)
db_connections_available / db_connections_max * 100

# JVM heap memory used (bytes)
jvm_memory_used_bytes{area="heap"}

# Circuit breaker status (1=OPEN, 0=CLOSED)
resilience4j_circuitbreaker_state{name="stripe"}
```

---

## Escalation Decision Tree

```
Error reproduced?
├─ NO → Intermittent issue
│       └─ Enable debug logs
│       └─ Increase monitoring
│       └─ Create ticket for investigation
│
└─ YES → Consistent issue
    ├─ All users affected?
    │   ├─ YES → P1 (Critical)
    │   │       └─ See Incident Response Guide
    │   └─ NO → Continue below
    │
    ├─ Recent deployment?
    │   ├─ YES → Git bisect to find commit
    │   │       └─ Rollback if serious
    │   └─ NO → Continue below
    │
    ├─ External service issue?
    │   ├─ YES (Stripe down)
    │   │       └─ Wait for recovery + check status page
    │   ├─ YES (Database)
    │   │       └─ Contact database team
    │   └─ NO → Continue below
    │
    └─ Resource issue?
        ├─ YES → Scale up resources
        │       └─ Investigate root cause
        └─ NO → Requires code investigation
                └─ Run profiler/debug logs
                └─ Create ticket for fix
```

---

## Support & Resources

- **SLA Response Time:** P1: 2min, P2: 15min, P3: 1hour, P4: 1day
- **Oncall:** See Escalation Contacts in Incident Response Guide
- **Monitoring:** https://grafana.leadflow.io
- **Logs:** https://logs.leadflow.io (ELK stack)
- **Status Page:** https://status.leadflow.io
- **Docs:** https://docs.leadflow.io

---

**Last Updated:** 2024
**Version:** 1.0
