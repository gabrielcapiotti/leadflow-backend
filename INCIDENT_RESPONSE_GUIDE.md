# Incident Response Playbook for LeadFlow Backend

## Table of Contents
1. [Overview](#overview)
2. [Severity Levels](#severity-levels)
3. [Incident Response Process](#incident-response-process)
4. [Common Incidents](#common-incidents)
5. [Escalation Contacts](#escalation-contacts)
6. [Post-Incident Review](#post-incident-review)
7. [Monitoring & Alerts](#monitoring--alerts)

---

## Overview

This playbook defines procedures for responding to production incidents affecting the LeadFlow Backend service. Incidents are classified by severity and trigger specific response procedures.

### Goals
- Minimize mean time to detection (MTTD)
- Minimize mean time to resolution (MTTR)
- Maintain data integrity during incidents
- Document all incidents for post-incident reviews
- Improve processes through lessons learned

### Service Level Objectives (SLO)
- **Availability:** 99.9% monthly
- **Mean Time to Detect (MTTD):** < 5 minutes
- **Mean Time to Resolution (MTTR):** < 30 minutes (P1)
- **Recovery Time Objective (RTO):** 15 minutes
- **Recovery Point Objective (RPO):** 5 minutes

---

## Severity Levels

### P1 - Critical (RED)
**Definition:** Service completely down or severely degraded, affecting all or most users

**Symptoms:**
- Application completely unavailable
- Database down or inaccessible
- All webhooks failing
- Authentication system down
- Payment processing stopped

**Response Time:** Immediate (< 2 minutes)
**MTTR Target:** 30 minutes
**Escalation:** All hands on deck

**Example:
```
Database connection pool exhausted
→ No one can login → Entire service down
```

---

### P2 - High (ORANGE)
**Definition:** Significant functionality degraded or affecting subset of users

**Symptoms:**
- Specific endpoints failing (e.g., only webhook reception)
- High error rates (> 10%)
- Performance degradation (> 5 sec response time)
- External API failures (Stripe, email service)
- Data sync issues

**Response Time:** < 15 minutes
**MTTR Target:** 2 hours
**Escalation:** Senior engineer + Slack notification

**Example:**
```
Stripe API timeout → All payment webhooks delayed
Some users affected but service partially functional
```

---

### P3 - Medium (YELLOW)
**Definition:** Minor functionality affected or intermittent issues

**Symptoms:**
- Non-critical feature down
- Intermittent errors (< 1% of requests)
- Minor performance issues (2-5 sec)
- Warnings in logs (not errors)
- Non-critical dependency slow

**Response Time:** < 1 hour
**MTTR Target:** 4 hours
**Escalation:** Team notification + planning for fix

**Example:**
```
Dashboard slow to load (5 sec vs 1 sec)
Not preventing users from completing tasks
```

---

### P4 - Low (BLUE)
**Definition:** Cosmetic issues or low-impact problems

**Symptoms:**
- UI display issues
- Non-critical logs warnings
- Documentation outdated
- Minor metrics inaccurate

**Response Time:** Next business day
**MTTR Target:** 1-2 weeks
**Escalation:** Log ticket for backlog

---

## Incident Response Process

### Phase 1: Detection & Alerting

#### Monitoring Systems Alert

```
Events trigger alerts in multiple systems:
├─ GitHub Actions: Build/test failure
├─ Kubernetes: Pod crash, resource exhaustion
├─ Prometheus: Service health threshold breach
├─ Application Logs: ERROR level events
├─ Health Checks: /api/actuator/health status DOWN
└─ User Reports: Slack #incidents channel
```

#### Initial Actions (First 2 minutes)

```bash
# 1. Receive alert on Slack
@oncall triggered for channel #incidents
Message includes:
- Alert name
- Service affected
- Severity level
- Link to dashboards

# 2. Open Grafana dashboard
https://grafana.leadflow.io/d/leadflow-backend

# 3. Check recent deployments
GitHub → Deployments → See what changed

# 4. Check application logs
kubectl logs deployment/leadflow-backend -n production --tail=100 -f

# 5. Open status page
https://status.leadflow.io → Check incident status
```

---

### Phase 2: Initial Incident Assessment

#### Triage (First 5 minutes)

```bash
#!/bin/bash
# Incident Triage Checklist

# 1. Verify the issue
echo "=== Service Status ==="
curl https://api.leadflow.io/api/actuator/health

# 2. Check health indicators
echo "=== Detailed Health ==="
curl https://api.leadflow.io/api/actuator/health/live

# 3. Monitor error rates
echo "=== Error Rate (Prometheus) ==="
# Query: rate(http_requests_total{status=~"5.."}[5m])

# 4. Check resource usage
echo "=== Kubernetes Resources ==="
kubectl top nodes
kubectl top pods -n production

# 5. Review recent commits
echo "=== Recent Changes ==="
git log --oneline -10

# 6. Determine severity
if application is completely down; then
  SEVERITY="P1"
elif error_rate > 10%; then
  SEVERITY="P2"
else
  SEVERITY="P3"
fi

echo "Severity Level: $SEVERITY"
```

#### Severity Decision Tree

```
Is the API completely unavailable?
├─ YES → P1 (Critical)
└─ NO
   ├─ Are all users affected?
   │  ├─ YES → P1 (Critical)
   │  └─ NO → Check error rate
   │
   └─ Is error rate > 10%?
      ├─ YES → Check user impact
      │  ├─ Payment processing down → P1
      │  ├─ Auth system down → P1
      │  ├─ Webhooks down → P2
      │  └─ Dashboard slow → P3
      └─ NO → Check symptoms
         ├─ Response time > 5s → P2
         ├─ Response time 2-5s → P3
         └─ Minor issues → P4
```

---

### Phase 3: Incident Command

#### Establish War Room

**For P1 incidents:**
```bash
# 1. Start Slack huddle
#    Slack → Incidents channel → Start huddle
#    Invite: @oncall, @senior-backend, @devops

# 2. Assign roles
   Incident Commander: Who is leading?
   Technical Lead: Who's investigating?
   Communication Lead: Who updates status?
   Operations: Who executes fixes?

# 3. Create incident tracking
   Slack: /incident create INCIDENT-123
   Pagerduty: Create incident
   Follow along in #incidents channel
```

#### Gather Information

```bash
# Timeline of recent changes
git log --oneline -20 | head

# Current deployment status
kubectl rollout status deployment/leadflow-backend -n production
kubectl get pods -n production

# Database status
kubectl exec -it postgres-0 -n production -- \
  psql -U postgres -c "SELECT version();"

# External services status
# Stripe API: https://status.stripe.com
# AWS: https://status.aws.amazon.com
# Email service: Provider dashboard
```

---

### Phase 4: Incident Resolution

#### Investigation Playbook

**Issue: Application Not Responding**

```bash
# Step 1: Check pod status
kubectl get pods -n production -o wide
# Look for: CrashLoopBackOff, ImagePullBackOff, Pending

# Step 2: If pod crashing:
kubectl logs deployment/leadflow-backend -n production --previous
# Check for: OutOfMemory, Exception, startup errors

# Step 3: Check events
kubectl describe deployment leadflow-backend -n production | tail -20

# Step 4: Resource exhaustion
kubectl top pod <pod-name> -n production
# Check if CPU/Memory at limits

# Resolution examples:
# Memory leak → Restart pods (TEMPORARY) → Deploy fix
# CPU throttled → Increase request limits → Trigger rollout
# Crash loop → Check configuration → Rollback
```

**Issue: High Error Rate**

```bash
# Step 1: Identify affected endpoint
# Prometheus query:
# rate(http_requests_total{status=~"5.."}[1m])

# Step 2: Check recent commits
git log --oneline -5
git show <commit-sha>

# Step 3: Review error logs
kubectl logs deployment/leadflow-backend -n production -f | grep ERROR

# Step 4: Check circuit breakers
# Metrics:
# resilience4j_circuitbreaker_state{name="stripe"} == 1 (OPEN = bad)

# Step 5: Decide on action
if recent_commit_is_cause; then
  kubectl rollout undo deployment/leadflow-backend -n production
elif external_service_down; then
  Enable circuit breaker fallback
  Update status page
elif database_issue; then
  Failover to replica
  Contact database team
fi
```

**Issue: Slow Response Times**

```bash
# Step 1: Identify slow queries
kubectl exec -it postgres-0 -n production -- \
  psql -U postgres -d leadflow \
  -c "SELECT query, mean_time FROM pg_stat_statements \
      ORDER BY mean_time DESC LIMIT 10;"

# Step 2: Check database performance
# Metrics:
# pg_queries_duration_seconds_bucket
# pg_stat_activity (long-running queries)

# Step 3: Check application metrics
# Rate of requests (increasing = normal)
# Request latency percentiles (p95, p99)

# Step 4: Mitigation
# Enable caching
# Add database index
# Optimize query
# Scale horizontally
```

---

### Phase 5: Recovery & Verification

#### Rollback Decision

```bash
Rollback Required?
├─ Caused by recent deployment → YES → ROLLBACK
├─ External service issue → NO → Wait/Failover
├─ Database problem → NO → Contact team
├─ Configuration issue → MAYBE → Check config
└─ Unknown cause → NO → Investigate further
```

#### Execute Rollback

```bash
# 1. Check available revisions
kubectl rollout history deployment/leadflow-backend -n production

# 2. Identify previous working version
# Check timestamps, deployment times
# Last successful deployment: 10:30 AM
# Current failed deployment: 10:45 AM

# 3. Execute rollback
kubectl rollout undo deployment/leadflow-backend -n production \
  --to-revision=5

# 4. Monitor after rollback
kubectl rollout status deployment/leadflow-backend -n production --timeout=5m

# 5. Verify functionality
curl https://api.leadflow.io/api/actuator/health
# Should respond: {"status":"UP"}

# 6. Check metrics returned to normal
# Error rate should drop within 1 minute
# Response latency should improve
```

#### Verification Checklist

```bash
# Post-recovery verification
✓ Health checks passing
  curl https://api.leadflow.io/api/actuator/health

✓ No error spikes
  Prometheus: rate(errors_total[5m]) == 0

✓ Response times normal
  P99 latency < 1 second

✓ Database responsive
  Select query response < 100ms

✓ External services OK
  Stripe, Email, other integrations working

✓ User transactions processing
  Check dashboard for recent transactions

✓ No cascading failures
  Check dependent services
```

---

## Common Incidents

### Incident Template

```
INCIDENT-XXX: [Brief Title]

Severity: P[1-4]
Start Time: 2024-01-15 14:23 UTC
End Time: 2024-01-15 14:38 UTC
Duration: 15 minutes

Timeline:
14:23 - Alert received: High error rate
14:24 - Incident commander assigned
14:26 - Root cause identified: OOM on pod
14:28 - Rollback executed
14:31 - Verification complete
14:38 - Incident closed

Root Cause: Memory leak in request handler

Action Taken: Rollback to previous version

Resolution: Code review of recent changes, memory profiling

Prevention: Add memory limits to CI/CD pipeline
```

---

### Scenario 1: Database Connection Pool Exhausted

**Symptoms:**
```
Error: Unable to acquire JDBC Connection
Error Rate: 100%
Response Time: Timeout (>30s)
```

**Diagnosis:**
```bash
# Check connection pool
SELECT count(*) FROM pg_stat_activity;

# Maximum: 100 connections (default HikariCP)
# If all in use: Connection leak somewhere
```

**Resolution:**
```bash
# Step 1: Identify hanging queries
SELECT pid, query, state_change FROM pg_stat_activity
  WHERE state != 'idle' AND query != '<IDLE>'
  ORDER BY state_change;

# Step 2: Terminate inactive connections
SELECT pg_terminate_backend(pid) FROM pg_stat_activity
  WHERE state = 'idle' AND query_start < now() - interval '10 minutes';

# Step 3: Restart application pool
kubectl rollout restart deployment/leadflow-backend -n production

# Step 4: Increase pool size if consistent issue
# application.yml:
# spring.datasource.hikari.maximum-pool-size: 150
# (then redeploy)
```

**Prevention:**
- Add monitoring: Alert if connections > 80%
- Code review for unclosed connections
- Add connection leak detection
- Set connection timeout to 10 minutes

---

### Scenario 2: Stripe API Timeout

**Symptoms:**
```
Error: com.stripe.exception.ApiConnectionException
Affected: All payment webhooks
Circuit Breaker State: OPEN
```

**Resolution:**
```bash
# Step 1: Verify Stripe status
curl https://status.stripe.com/api.json

# Step 2: If Stripe is down:
# → Our system will automatically fail over
# → Webhooks stored in FailedWebhookEvent table
# → Circuit breaker prevents cascade failures

# Step 3: Monitor Stripe recovery
# → Circuit breaker auto-closes after success

# Step 4: Manual retry (when Stripe recovers)
curl -X POST https://api.leadflow.io/api/billing/webhooks/retry \
  -H "Authorization: Bearer $API_KEY"

# Step 5: Verify recovery
SELECT COUNT(*) FROM failed_webhook_events WHERE status = 'SUCCEEDED';
```

**Prevention:**
- Circuit breaker configured (active)
- Failed webhook replay mechanism active
- Slack alert when circuit opens
- Status page updated automatically

---

### Scenario 3: Memory Leak (Gradual Degradation)

**Symptoms:**
```
Memory Usage: Gradually increasing
Pod Memory: 512MB → 900MB → 1900MB (limit)
Pod restarts: Every 2-4 hours
Error: OutOfMemoryError: Java heap space
```

**Diagnosis:**
```bash
# Step 1: Get memory usage trend
kubectl top pods deployment=leadflow-backend -n production --containers

# Step 2: Check if restarting helps
kubectl rollout restart deployment/leadflow-backend -n production

# Step 3: Analyze heap dump
# Enable in application:
# JAVA_OPTS="-XX:+HeapDumpOnOutOfMemoryError"
# Extract dump and analyze with jhat/JProfiler
```

**Resolution:**
```bash
# Step 1: Identify source (usually in code review)
# Common causes:
# - Caching without eviction
# - Collection growing unbounded
# - Event listeners not unregistering

# Step 2: Increase heap temporarily
# deployment:
#   spec.containers.resources:
#     limits.memory: 2Gi
#     requests.memory: 1Gi

# Step 3: Deploy fix immediately

# Step 4: Monitor memory after fix
kubectl top pods ... --watch
```

**Prevention:**
- Add memory profiling agent in production
- Set alerting on memory > 80% of limit
- Scheduled memory heap dumps
- Code review for memory-intensive operations

---

### Scenario 4: Cascading Failures

**Symptoms:**
```
Initial: Stripe timeout (P2 issue)
Cascades: Email service also times out
Then: Database connections exhausted (P1 issue)
Result: Entire system down
```

**Prevention - Already Implemented:**

```java
// 1. Circuit breakers prevent cascades
@CircuitBreaker(name = "stripe")
public void processStripeEvent(Event event) { ... }
// After 5 failures in 30s window:
// → Circuit opens
// → Requests fail immediately (not timeout)
// → Prevents connection pool exhaustion
// → Other systems unaffected

// 2. Retry policies with timeouts
@Retry(name = "stripe", maxAttempts = 3)
@Timeout(duration = "5s", unit = SECONDS)
// → Max 5 second attempts
// → Bounded retries

// 3. Fallback mechanisms
@Fallback(fallbackMethod = "emailFallback")
public void sendEmail(...) { ... }
// → When failed, use queue
// → Prevents cascade
```

**If Cascading Failure Occurs:**

```bash
# Step 1: Identify primary failure
Check logs for FIRST error:
  ERROR Stripe API timeout
  → This is the root cause

# Step 2: Isolate the failing service
kubectl set env deployment/leadflow-backend \
  -n production \
  STRIPE_ENABLED=false

# Temporary disables Stripe integration
# Application continues serving other functions

# Step 3: Fix root cause
Deploy fix for Stripe integration

# Step 4: Re-enable
kubectl set env deployment/leadflow-backend \
  -n production \
  STRIPE_ENABLED=true

# Step 5: Monitor
Verify error rate returns to 0
Verify other services stable
```

---

### Scenario 5: Data Corruption / Consistency Issue

**Symptoms:**
```
Error: Constraint violation or wrong data
Examples:
- User's subscription status = "ACTIVE" but no payment method
- Payment recorded twice in database
- Webhook processed twice
```

**Diagnosis:**
```sql
-- Example: Find duplicate transactions
SELECT payment_id, count(*)
FROM payments
GROUP BY payment_id
HAVING count(*) > 1;

-- Find inconsistent subscriptions
SELECT id, status, has_payment_method
FROM subscriptions
WHERE status = 'ACTIVE' AND has_payment_method = false;
```

**Resolution Process:**

```bash
# P1 Response: Immediately stop the source
# If duplicate webhook processing:
  kubectl scale deployment leadflow-backend --replicas=1 -n production
  # Stopped processing, prevent further corruption

# P1 Investigation: Identify exact cause
  git log --oneline -20 | Check for recent changes
  git bisect | Find which commit introduced it
  
# P2 Remediation:
  Write SQL script to fix inconsistencies
  SELECT pg_sleep(1); -- Test in transaction first
  BEGIN TRANSACTION;
    DELETE FROM payments WHERE id IN (SELECT ... DUPLICATE);
    UPDATE subscriptions SET status = 'GRACE_PERIOD' WHERE ...;
  ROLLBACK; -- First try
  COMMIT; -- If correct

# P3 Monitor after fix:
  Check constraint violations for 1 hour
  Monitor application for new errors
  
# P4 Root cause fix:
  Add unique constraint
  Add application-level deduplication
  Add better transaction isolation
  
# P5 Deploy fix:
  Scale back to normal replicas
  Deploy code fix
  Monitor for recurrence
```

**Prevention:**
- Database constraints (UNIQUE, FOREIGN KEY)
- Transaction isolation levels (READ_COMMITTED)
- Idempotent operations
- Change data capture auditing

---

## Escalation Contacts

### On-Call Schedule

```
Weekdays (Mon-Fri):
09:00 - 17:00: @senior-backend-oncall (primary)
17:00 - 09:00: @backend-overflow (backup)

Weekends & Holidays:
Always: @senior-backend-oncall (extended)

P1 Escalations:
├─ 0 min: Notify @oncall
├─ 5 min: Notify @senior-backend if oncall not responding
├─ 10 min: Notify @engineering-manager
└─ 15 min: Notify @cto
```

### Contact Bridge

**Slack Channels:**
- #incidents - All incident discussion
- #incidents-p1 - Critical issues (loud notifications)
- #engineering - Team updates

**Phone Escalation:**
```
1. Call oncall number (in Pagerduty)
2. Last resort: SMS to team leads (only P1)
3. Emergency: Call office number, ask for duty manager
```

**Pagerduty:**
```
Create incident: /incident create
Escalate: Click service → Escalate manually
Notify: Add responders midway through incident
```

---

### Team Contacts

**Backend Team:**
- Lead: @person1 - Kafka, queues, event processing
- Senior: @person2 - Database, transactions, consistency
- Rotation: @person3 @person4 @person5 - On-call weekly

**DevOps Team:**
- Lead: @devops-lead - Infrastructure decisions
- Kubernetes: @k8s-expert - Cluster issues
- Database: @db-admin - PostgreSQL issues

**External Escalations:**
- Stripe Support: [stripe.com/support](https://stripe.com/support)
- AWS Support: [console.aws.amazon.com](https://console.aws.amazon.com) (24/7)
- Email Service: [contact their support](https://sendgrid.com/en-us/contact)

---

## Post-Incident Review

### 5-Whys Analysis

**Example: Database Connection Pool Exhausted**

```
Incident: Database connection pool exhausted

Why 1: Connection pool grew to 100 (max limit)
Why 2: Requests were holding connections longer than expected
Why 3: A new HTTP timeout was set to 30 seconds
Why 4: Recent commit added external API call with high timeout
Why 5: We didn't test with production-like connection counts

Root Cause: Insufficient load testing before deployment
```

### Action Items

**From Example Incident:**

```
Action Item 1: (Severity: HIGH)
  Title: Add connection pool monitoring
  Owner: @person1
  Due: 1 week
  Details: Alert if connections > 80 of 100
  
Action Item 2: (Severity: HIGH)
  Title: Load test new features
  Owner: @person2
  Due: 2 weeks
  Details: Add to deployment checklist
  
Action Item 3: (Severity: MEDIUM)
  Title: Document connection tuning
  Owner: @person3
  Due: 1 week
  Details: Add to runbook
  
Action Item 4: (Severity: LOW)
  Title: Review timeout configurations
  Owner: @person4
  Due: 1 month
  Details: Audit all timeouts for appropriateness
```

### Blameless Culture

✅ **DO:**
- Focus on systems, not individuals
- Ask "What conditions allowed this to happen?"
- Appreciate the person who found the issue
- Share learnings across team

❌ **DON'T:**
- Blame individual engineers
- Skip post-mortem to "move fast"
- Make decisions in anger during incident
- Punish people for bugs that slip through

### Communication Template

```
Subject: Post-Incident Review - INCIDENT-123

Timeline:
14:23 - Alert: High error rate (100%)
14:38 - Incident resolved (rollback executed)
Duration: 15 minutes

Impact:
- 100% of users affected
- ~500 failed API requests
- ~2 failed webhook deliveries

Root Cause:
Memory leak in request handler (recent commit)

Immediate Actions:
1. Rollback deployed
2. Code reviewed and memory profiler attached

Prevention (Action Items):
1. [DONE] Deploy memory profiler in test environment
2. [TODO] Add heap dump on OOM for analysis
3. [TODO] Increase resource limits in production
4. [TODO] Add memory testing to CI pipeline

Lessons Learned:
- Memory profiling should happen in CI, not production
- Need load testing before deploying large changes
- Rollback process worked well, great execution
```

---

## Monitoring & Alerts

### Alert Configurations

**Health Check Alerts (Green = All Good):**

```yaml
# Prometheus AlertManager
groups:
  - name: "LeadFlow Backend"
    rules:
      # P1: Application Down
      - alert: ApplicationDown
        expr: up{job="leadflow"} == 0
        for: 2m
        severity: critical
        action: Page oncall immediately
      
      # P1: High Error Rate
      - alert: HighErrorRate
        expr: rate(errors_total[5m]) > 0.1
        for: 5m
        severity: critical
        action: Page oncall
      
      # P2: High Response Latency
      - alert: HighLatency
        expr: histogram_quantile(0.95, http_request_duration) > 5
        for: 10m
        severity: warning
        action: Notify team
      
      # P2: Database Connections Low
      - alert: DatabaseConnectionPoolLow
        expr: db_connections_available < 10
        for: 5m
        severity: warning
        action: Notify oncall
```

**Health Indicator Status:**

```
GET /api/actuator/health

Response (when healthy):
{
  "status": "UP",
  "components": {
    "stripe": {"status": "UP"},
    "database": {"status": "UP", "details": {"responseTime": "42ms"}},
    "email": {"status": "UP", "details": {"responseTime": "187ms"}},
    "webhookQueue": {"status": "UP", "details": {"pending": 5}}
  }
}

Alert Trigger: Any component status != "UP"
→ Investigated within 15 minutes

Response: Check specific component's integration
```

---

### Dashboards

**Main Dashboard - `https://grafana.leadflow.io/d/main`**

```
┌─────────────────────────────────────────────┐
│ LeadFlow Backend - Production Dashboard      │
├─────────────────────────────────────────────┤
│                                             │
│ ┌─────────┬──────────┬──────────┐           │
│ │ Status  │ Error %  │ Latency  │           │
│ │   UP    │  0.02%   │  234ms   │           │
│ └─────────┴──────────┴──────────┘           │
│                                             │
│ [Line Chart] Requests/sec (5m avg)          │
│ [Line Chart] Error Rate (5m avg)            │
│ [Line Chart] P95 Latency (5m avg)           │
│ [Line Chart] Database Connections           │
│ [Table] Recent Errors (top 10)              │
│ [Table] Slowest Endpoints                   │
│                                             │
└─────────────────────────────────────────────┘
```

**Incident Dashboard - When Alert Fires**

```
Automatically opens showing:
1. Timeline of metric changes
2. Recent deployments
3. Related logs
4. Similar past incidents
5. Runbook for incident type
```

---

### Automated Responses

**Circuit Breaker Auto-Recovery:**

```
Stripe API Error → Circuit Opens
└─ Status: OPEN (fails immediately, not timeout)
└─ Effect: Webhooks queued in FailedWebhookEvent
└─ Alert: #incidents "Stripe circuit breaker opened"

Stripe API Recovers → Automatic Test
└─ After 60s: Send test request
└─ If Success: Circuit closes (HALF_OPEN → CLOSED)
└─ If Fail: Keep OPEN, retry in another 60s

Automatic Retry
└─ Pending webhooks: Retry with exponential backoff
└─ After recovery: Process all pending webhooks
└─ Alert: #incidents "Stripe circuit breaker recovered"
```

---

### Chaos Engineering (Optional)

**Testing Incident Responses:**

```bash
# Safely test procedures in staging environment

# Test 1: Application restart
kubectl delete pods -n staging -l app=leadflow-backend

# Test 2: Database connection failure
# Temporarily disconnect database (in test cluster)

# Verify:
- Alerts trigger correctly
- Team gets notified
- Health checks go DOWN
- Incident procedures work
```

---

## Appendix: Quick Reference

### Emergency Stop Procedure

```bash
# If in doubt, stop processing to prevent damage
kubectl scale deployment leadflow-backend --replicas=0 -n production
sleep 2

# Assess damage
# Fix issue
# Restart
kubectl scale deployment leadflow-backend --replicas=3 -n production

# Verify
kubectl rollout status deployment/leadflow-backend -n production
```

### Manual Database Failover

```bash
# If primary PostgreSQL down
kubectl get statefulsets -n production
kubectl get pods -n production | grep postgres

# Switch to replica
# Contact database team immediately
# Do not attempt if unsure
```

### View Recent Changes

```bash
# Recent commits to main
git log --oneline -20 --graph

# Recent deployments
kubectl rollout history deployment/leadflow-backend -n production

# Recent configuration changes
kubectl get configmap leadflow-config -n production -o yaml > backup.yml
```

---

**Last Updated:** 2024
**Maintained By:** Engineering Team
**Version:** 1.0
**Review Frequency:** Quarterly
