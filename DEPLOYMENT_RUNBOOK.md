# Deployment Runbook for LeadFlow Backend

## Overview

This guide provides step-by-step instructions for deploying LeadFlow Backend to production environments (staging and production).

---

## Pre-Deployment Checklist

### Code Quality

- [ ] All tests passing (`mvn test`)
- [ ] Code review approved (2+ approvals)
- [ ] No critical security vulnerabilities
- [ ] Performance benchmarks acceptable
- [ ] All new dependencies approved

### Documentation

- [ ] README updated if API changed
- [ ] Migration guide written (if breaking changes)
- [ ] Changelog updated with new features
- [ ] Team notified of deployment time

### Environment Preparation

- [ ] All secrets configured in GitHub
- [ ] Database backups current
- [ ] Monitoring dashboards configured
- [ ] Health checks verified
- [ ] Rollback plan ready

---

## Deployment Methods

### Method 1: Automatic Deployment (Recommended)

**How it works:** Push to main/master → Automatic build, test, and deploy to staging

```bash
# 1. Code is ready
git switch main
git pull origin main

# 2. Create feature branch
git switch -b feature/new-feature

# 3. Make changes, commit, push
git push origin feature/new-feature

# 4. Create Pull Request
# → GitHub Actions will:
#    ├─ Build the code
#    ├─ Run tests
#    ├─ Run security scans
#    └─ Report status

# 5. Get approval and merge
# → GitHub Actions will:
#    ├─ Build again
#    ├─ Push to Docker registry
#    └─ Deploy to staging

# 6. Manual approval for production
# → Go to GitHub Actions → Deploy workflow
# → Click "Run workflow"
# → Select "production" environment
# → Confirm approval
# → Deploy to production
```

**Monitoring:**
```bash
# Watch deployment status
kubectl rollout status deployment/leadflow-backend -n staging
kubectl rollout status deployment/leadflow-backend -n production

# Check logs
kubectl logs -n production deployment/leadflow-backend -f

# Verify health
curl https://api.leadflow.io/api/actuator/health
```

---

### Method 2: Manual Deployment via CLI

**Use case:** Emergency hotfix, need more control

```bash
# 1. Build locally (optional, CI does this)
mvn clean package -DskipTests

# 2. Create Docker image
docker build -t docker.io/leadflow/backend:hotfix-1 .

# 3. Push to registry
docker push docker.io/leadflow/backend:hotfix-1

# 4. Deploy to staging
kubectl set image deployment/leadflow-backend \
  leadflow=docker.io/leadflow/backend:hotfix-1 \
  -n staging

# 5. Wait for rollout
kubectl rollout status deployment/leadflow-backend -n staging --timeout=5m

# 6. Verify health
curl https://api-staging.leadflow.io/api/actuator/health

# 7. If good, deploy to production
kubectl set image deployment/leadflow-backend \
  leadflow=docker.io/leadflow/backend:hotfix-1 \
  -n production

# 8. Monitor
kubectl rollout status deployment/leadflow-backend -n production --timeout=5m
```

---

### Method 3: Helm Upgrade

**Use case:** Deploying with configuration changes

```bash
# 1. Update Helm values
vim helm-charts/leadflow/values.yaml
# Change: image.tag, replicas, resources, etc.

# 2. Validate helm chart
helm lint helm-charts/leadflow

# 3. Dry run (see what will change)
helm upgrade leadflow helm-charts/leadflow \
  -n staging \
  --namespace staging \
  --dry-run \
  --values helm-charts/leadflow/values-staging.yaml

# 4. Upgrade staging
helm upgrade leadflow helm-charts/leadflow \
  -n staging \
  --namespace staging \
  --values helm-charts/leadflow/values-staging.yaml

# 5. Check deployment
helm status leadflow -n staging
kubectl get pods -n staging -l app=leadflow-backend

# 6. Upgrade production (after testing)
helm upgrade leadflow helm-charts/leadflow \
  -n production \
  --namespace production \
  --values helm-charts/leadflow/values-prod.yaml
```

---

## Staging Deployment Process

### 1. Pre-deployment

```bash
# Create change log
echo "## Version 1.5.0 - $(date +%Y-%m-%d)" >> CHANGELOG.md
echo "- Added webhook replay functionality" >> CHANGELOG.md
echo "- Improved error handling" >> CHANGELOG.md

# Update version
sed -i 's/<version>1.4.9<\/version>/<version>1.5.0<\/version>/' pom.xml

# Commit
git add CHANGELOG.md pom.xml
git commit -m "chore: bump version to 1.5.0"
git push origin develop
```

### 2. Build & Test

```bash
# In GitHub Actions (automatic):
# 1. Compile
# 2. Run unit tests
# 3. Run integration tests
# 4. Security scans
# 5. Build Docker image
# 6. Push to registry
```

### 3. Deploy to Staging

```bash
# Automatic on master/main merge

# Or manual:
kubectl set image deployment/leadflow-backend \
  leadflow=$DOCKER_REGISTRY/leadflow-backend:$COMMIT_SHA \
  -n staging

# Monitor
kubectl rollout status deployment/leadflow-backend -n staging
```

### 4. Smoke Tests

```bash
# Health check
curl https://api-staging.leadflow.io/api/actuator/health
# Expected: {"status":"UP"}

# Basic API test
curl https://api-staging.leadflow.io/api/subscriptions \
  -H "Authorization: Bearer $TEST_TOKEN"
# Expected: List of subscriptions

# Test new feature
# (specific to what's being deployed)

# Monitor for errors
kubectl logs -n staging deployment/leadflow-backend --tail=50
# Should show: "Application started"
# Should NOT show: ERROR messages
```

### 5. Load Testing (Optional)

```bash
# Use Apache Bench or k6 for load test
# k6 is recommended

# Install k6
# Run test
k6 run loadtest.js --vus=10 --duration=5m

# Expected results:
# - No errors
# - P95 latency < 2s
# - Error rate 0%

# If issues, investigate before production deployment
```

### 6. Soak Testing (24+ hours)

```bash
# Run staging for 24 hours with production traffic copy
# Monitor for:
# - Memory leaks
# - Connection pool exhaustion
# - Database issues
# - Cascading failures

# If all good, proceed to production
# If issues, debug and re-deploy to staging
```

---

## Production Deployment Process

### 1. Pre-deployment Meeting

```
Attendees: Engineering, DevOps, Product, Support

Agenda:
1. What's being deployed?
2. Why are we deploying?
3. What's the risk level?
4. What's the rollback plan?
5. Who's on-call if issues?
6. When is the deployment?

Output:
- Approval to proceed
- Assigned incident commander
- Staging environment confirmed stable
```

### 2. Notification to Users

```bash
# 5 minutes before
# Post in Slack #general:
"Deploying LeadFlow Backend v1.5.0 to production in 5 minutes.
Expected downtime: 2-3 minutes during rollout.
Contact @oncall if issues."

# Or update status page
https://status.leadflow.io → New incident
"Planned maintenance - LeadFlow API deployment"
```

### 3. Production Deployment

```bash
# Option A: Via GitHub Actions
# 1. Go to Actions → Deploy workflow
# 2. Click "Run workflow"
# 3. Select "production"
# 4. Review approval request
# 5. Confirm deployment

# Option B: Via kubectl (manual)
kubectl set image deployment/leadflow-backend \
  leadflow=$DOCKER_REGISTRY/leadflow-backend:$VERSION \
  -n production

# Monitor rollout
kubectl rollout status deployment/leadflow-backend \
  -n production \
  --timeout=10m

# Expected output:
# Waiting for deployment "leadflow-backend" rollout to finish...
# Waiting for 3 new pods...
# Waiting for 3 pods to be ready...
# deployment "leadflow-backend" successfully rolled out
```

### 4. Post-deployment Verification

```bash
# 1. Health check (5 sec after rollout complete)
for i in {1..5}; do
  curl -s https://api.leadflow.io/api/actuator/health | jq .status
  sleep 1
done
# Expected: UP (all 5 times)

# 2. Error rate check (2 min after rollout)
# In Grafana, check "Error Rate %" panel
# Expected: 0% (or < 0.1%)

# 3. Response latency check
# Prometheus query: histogram_quantile(0.95, http_request_duration_seconds_bucket)
# Expected: < 1 second (same as before deployment)

# 4. Database connection pool
# Prometheus query: db_connections_available
# Expected: > 10 available (out of 100 max)

# 5. Sample API request
curl -s https://api.leadflow.io/api/subscriptions \
  -H "Authorization: Bearer $TOKEN" | jq . | head -20
# Expected: Valid JSON response with data

# 6. Check logs for errors
kubectl logs -n production deployment/leadflow-backend --tail=100 | grep ERROR
# Expected: No ERROR messages (or only expected warnings)
```

### 5. Declare Success

```bash
# Once all checks pass:

# Post in Slack
"✅ LeadFlow Backend v1.5.0 successfully deployed to production.
- Deployment time: 3 min 24 sec
- Health checks: PASS
- Error rate: 0%
- All systems operating normally"

# Update status page
https://status.leadflow.io → Incident resolved
"Deployment completed at 2024-01-15 14:30 UTC"

# Close incident in Pagerduty
Mark incident as "Resolved"

# Continue monitoring for 1 hour
# If issues arise, see Incident Response Guide
```

---

## Rollback Procedures

### When to Rollback

⚠️ **Immediate rollback if:**
- Service completely unavailable
- Database data corruption
- All endpoints returning errors
- Cascading failures to other services

📋 **Gradual investigation if:**
- Single endpoint failing
- Intermittent errors (< 1%)
- Slow response times
- Non-critical features down

### Quick Rollback

**Option 1: Kubernetes Rollout Undo (Fastest)**

```bash
# Rollback to previous deployment
kubectl rollout undo deployment/leadflow-backend -n production

# Monitor rollout
kubectl rollout status deployment/leadflow-backend -n production

# Verify
curl https://api.leadflow.io/api/actuator/health
```

**Option 2: Redeploy Previous Image**

```bash
# Get previous image
kubectl rollout history deployment/leadflow-backend -n production
# Shows all revisions with timestamps

# Redeploy specific revision
kubectl rollout undo deployment/leadflow-backend \
  --to-revision=5 \
  -n production

# Monitor
kubectl rollout status deployment/leadflow-backend -n production
```

**Option 3: Manual image change**

```bash
# Find previous working image
kubectl describe deployment leadflow-backend -n production | grep Image
# Image: docker.io/leadflow-backend:abc123

# Rollback to known good image
kubectl set image deployment/leadflow-backend \
  leadflow=docker.io/leadflow-backend:previous-known-good \
  -n production

# Monitor
kubectl rollout status deployment/leadflow-backend -n production
```

### Post-Rollback

```bash
# 1. Verify original version stable
kubectl logs -n production deployment/leadflow-backend | grep ERROR
# Expected: No new errors

# 2. Create incident report
# Title: "Rollback executed for v1.5.0"
# Root cause: [To be determined]
# Timeline: Deployment 14:25 → Rollback 14:31 (6 minutes)

# 3. Root cause analysis
# Use: git log, git diff, test results
# Run: Profiler, heap dump, stress test

# 4. Schedule re-deployment
# Once root cause fixed and verified

# 5. Post-mortem review
# See: Post-Incident Review (Incident Response Guide)
```

---

## Database Migrations

### Pre-deployment Migrations

**If schema changes are needed:**

```bash
# 1. Write migration
# File: db/migrations/V123__AddWebhookStatusField.sql
CREATE TABLE failed_webhook_events (
  id UUID PRIMARY KEY,
  status VARCHAR(20) NOT NULL,
  ...
);

# 2. Test locally
# Run migration against local database
flyway migrate

# 3. Include in Docker image
# Dockerfile includes: COPY db/ /migration/
# Startup runs: flyway migrate

# 4. Deploy normally
# Migration happens during pod startup
# Before application code starts
```

### Zero-Downtime Migrations

```bash
# If migration requires downtime:

# 1. Deploy with feature flag
kubectl set env deployment/leadflow-backend \
  NEW_FEATURE_ENABLED=false

# 2. Run migration (with feature disabled)
# No users affected by incomplete schema

# 3. Verify migration success
SELECT COUNT(*) FROM failed_webhook_events;

# 4. Enable feature
kubectl set env deployment/leadflow-backend \
  NEW_FEATURE_ENABLED=true

# 5. Monitor
kubectl logs -n production deployment/leadflow-backend | grep ERROR
```

### Rollback Migrations

```bash
# If migration has issues:

# Option 1: Simple rollback (safe)
# Set feature flag to false
kubectl set env deployment/leadflow-backend \
  NEW_FEATURE_ENABLED=false

# Rollback Kubernetes
kubectl rollout undo deployment/leadflow-backend -n production

# Option 2: Database rollback (if needed)
# Run inverse migration
# File: db/migrations/V124__RemoveWebhookStatusField.sql
DROP TABLE failed_webhook_events;

# Verify
SELECT * FROM pg_tables WHERE tablename = 'failed_webhook_events';
# Should return no results
```

---

## Deployment Metrics

### Track Each Deployment

```bash
# Duration
Start: 14:25:00
End: 14:28:24
Duration: 3 min 24 sec

# Success Rate
Attempted: 1
Successful: 1
Success Rate: 100%

# Impact
Downtime: < 1 second (rolling update)
Errors during deployment: 0
Users affected: 0
```

### Monthly Deployment Report

```bash
Deployments this month: 24
Success rate: 100%
Average deployment time: 4 minutes
Mean time to rollback (if needed): 2 minutes
Critical incidents caused by deployments: 0
Minor incidents: 1 (slow response after dep, resolved with optimization)

Trend: Improving efficiency
Next goal: < 3 min average deployment time
```

---

## Emergency Deployment (Critical Hotfix)

**Use case:** Security vulnerability, data corruption, critical bug

### Steps

```bash
# 1. Fix code (FAST!)
# Usually isolate to one method/file
# Review by senior engineer only (no full code review process)

# 2. Build immediately
mvn clean package -DskipTests -q

# 3. Docker build (local)
docker build -t hotfix-1 .

# 4. Push to staging (bypass CI/CD)
docker tag hotfix-1 docker.io/leadflow/backend:hotfix-$(date +%s)
docker push docker.io/leadflow/backend:hotfix-$(date +%s)

# 5. Deploy to production immediately
kubectl set image deployment/leadflow-backend \
  leadflow=docker.io/leadflow/backend:hotfix-$(date +%s) \
  -n production

# 6. Monitor closely
kubectl logs -n production deployment/leadflow-backend -f

# 7. If issues: IMMEDIATE ROLLBACK
kubectl rollout undo deployment/leadflow-backend -n production
```

### Post-Hotfix

```bash
# 1. Update code review policy
# Add this change to normal code review

# 2. Add tests
# Write test that would catch this bug

# 3. Update deployment checklist
# Add step: "Verify hotfix applies to main branch"

# 4. Schedule for normal deployment
# Merge to main, run full CI/CD
```

---

## Deployment Checklist Summary

### Before Deployment
- [ ] All code merged to main
- [ ] Tests passing (100%)
- [ ] Security scans clean (no critical CVEs)
- [ ] Team notified
- [ ] Rollback plan documented
- [ ] Monitoring dashboards ready
- [ ] Health checks verified

### During Deployment
- [ ] Staging deployment successful
- [ ] Smoke tests passed
- [ ] Team monitoring
- [ ] Slack notifications sent
- [ ] Status page updated
- [ ] Incident commander assigned

### After Deployment
- [ ] Production health checks passing
- [ ] Error rate 0%
- [ ] Response latency normal
- [ ] Database healthy
- [ ] Users reporting no issues
- [ ] Monitoring active for 1 hour
- [ ] Success notification sent

---

## Support

- **Questions:** Ask in #engineering Slack
- **Issues:** Create GitHub issue or Pagerduty incident
- **Escalation:** See Incident Response Guide
- **Metrics:** https://grafana.leadflow.io
- **Status:** https://status.leadflow.io

---

**Last Updated:** 2024
**Version:** 1.0
