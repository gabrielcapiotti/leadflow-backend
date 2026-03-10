# ✅ Production Deployment Checklist - LeadFlow

Complete checklist before deploying to production.

## 📋 Pre-Deployment (Before Code Merge)

### Code Quality
- [ ] All tests passing (`mvn clean test -DskipITs`)
- [ ] No compile errors (`mvn clean compile -DskipTests`)
- [ ] Code coverage > 80% (run `mvn jacoco:report`)
- [ ] No XXX/TODO comments in critical code
- [ ] No hardcoded passwords/API keys in code
- [ ] No debug logging in production code
- [ ] No System.out.println (use logger)
- [ ] SonarQube quality gate passed

### Security Review
- [ ] No SQL injection vulnerabilities (use parameterized queries)
- [ ] No XSS vulnerabilities (sanitize output)
- [ ] No CSRF vulnerabilities (use tokens)
- [ ] All secrets in environment variables (.env not in git)
- [ ] JWT tokens have expiration (< 24h)
- [ ] Webhook signature validation enabled
- [ ] HTTPS enforced (no HTTP)
- [ ] CORS properly configured (not * for production)
- [ ] Authentication required on all endpoints
- [ ] API rate limiting configured

### Documentation
- [ ] API documentation updated (Swagger/OpenAPI)
- [ ] README.md reflects current state
- [ ] CHANGELOG.md contains new features
- [ ] Database migration scripts tested
- [ ] Runbooks created for common issues
- [ ] Architecture diagram updated

### Testing
- [ ] Unit tests: 100% passing
- [ ] Integration tests: 100% passing
- [ ] Contract tests (if applicable): passing
- [ ] Load test results reviewed (throughput acceptable)
- [ ] Security tests passed (OWASP top 10)
- [ ] Database rollback plan tested

---

## 🔧 Environment Setup (On Deployment Day)

### Infrastructure
- [ ] Database server running and accessible
- [ ] Redis cache configured (if used)
- [ ] Message queue configured (if used)
- [ ] Monitoring stack ready (Prometheus + Grafana)
- [ ] Logging aggregation running (ELK/CloudWatch)
- [ ] Load balancer configured
- [ ] SSL certificates installed
- [ ] Firewall rules opened for required ports
- [ ] Database backup configured (daily)
- [ ] Disaster recovery plan in place

### Configuration Files

#### .env Production
- [ ] `SERVER_PORT` set correctly
- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] `SPRING_DATASOURCE_URL` points to production DB
- [ ] Database username/password strong (20+ chars)
- [ ] `JWT_SECRET` strong (32+ chars, random)
- [ ] `STRIPE_API_KEY` is live key (sk_live_*)
- [ ] `STRIPE_WEBHOOK_SECRET` is live secret (whsec_live_*)
- [ ] Email configuration correct (SMTP host/port/auth)
- [ ] Email FROM address is company domain
- [ ] Logging level set to INFO (not DEBUG)
- [ ] All required variables defined

#### application-prod.yml
```yaml
✓ Database connection pool properly sized
✓ JPA caching enabled for read-heavy queries
✓ Thymeleaf cache enabled
✓ Spring MVC cache configured
✓ Error handling pages configured
✓ Static resource caching headers set
```

### Secrets Management
- [ ] All secrets stored in AWS Secrets Manager / Vault
- [ ] No secrets in git history (git-secrets installed)
- [ ] Access to secrets restricted (IAM roles)
- [ ] Secrets rotation scheduled
- [ ] Secret audit logging enabled

---

## 🗄️ Database Preparation

### Schema
- [ ] Latest Flyway migrations applied
- [ ] Database user created with minimal permissions
- [ ] Character encoding set to UTF-8
- [ ] Timezone set to UTC
- [ ] Indexes created for frequently queried columns
- [ ] Foreign keys properly defined
- [ ] Constraints validated

### Data
- [ ] Backups taken before deployment
- [ ] Data migration scripts tested
- [ ] Rollback scripts tested
- [ ] Database statistics updated
- [ ] Slow query log enabled for monitoring

### Validation
```sql
-- Run these checks before deployment
SELECT COUNT(*) FROM users;  -- Verify user table exists
SELECT COUNT(*) FROM leads;  -- Verify leads table exists
SELECT * FROM flyway_schema_history;  -- Check migrations
```

---

## 🚀 Deployment Process

### Pre-Deployment Validation
- [ ] Current application backed up (snapshot)
- [ ] Database backed up (full backup)
- [ ] Rollback plan documented and tested
- [ ] Team notified of deployment window
- [ ] On-call engineer assigned
- [ ] Communication channel open (Slack/Teams)

### Build & Package
- [ ] Clean build: `mvn clean package -Pprod -DskipTests`
- [ ] JAR size reasonable (< 200MB in most cases)
- [ ] No warnings/errors in build output
- [ ] Docker image built and pushed (if using Docker)
- [ ] Image security scan passed (no critical CVEs)
- [ ] JAR signed (if required by organization)

### Application Startup
- [ ] Java version correct (`java -version` matches pom.xml)
- [ ] Heap size set appropriately (`-Xmx2G` for production)
- [ ] GC settings optimized (G1GC recommended)
- [ ] Application starts without errors
- [ ] Health check endpoint responds (GET /actuator/health)
- [ ] Metrics endpoint accessible (GET /actuator/prometheus)
- [ ] No exceptions in logs on startup

### Integration Testing (Post-Deployment)
- [ ] Postman collection runs successfully
- [ ] All webhook endpoints respond with 200
- [ ] Email sending works (test email sent)
- [ ] Database queries working (data retrievable)
- [ ] Stripe integration working (test checkout)
- [ ] Authentication working (JWT tokens valid)
- [ ] All metrics visible in Prometheus

---

## 📊 Monitoring & Validation

### Application Health
- [ ] Health check: `GET /actuator/health` returns `UP`
- [ ] Metrics endpoint: `GET /actuator/prometheus` returns data
- [ ] Response times normal (< 500ms p95)
- [ ] Memory usage stable (no memory leaks)
- [ ] CPU usage < 70%
- [ ] Disk usage < 80%
- [ ] Database connections healthy
- [ ] Thread count normal (no deadlocks)

### Business Metrics
- [ ] Webhook processing: Success rate > 99%
- [ ] Email delivery: No bounces
- [ ] Checkout sessions: Response time < 3s
- [ ] Payment processing: No failures
- [ ] User registration: No errors
- [ ] API latency: P95 < 500ms

### Logging & Alerting
- [ ] Logs flowing to aggregation system
- [ ] Error rate dashboard visible
- [ ] Alert thresholds configured
- [ ] On-call notifications enabled
- [ ] Error tracking (Sentry) connected
- [ ] Performance APM enabled (if applicable)

### Historical Comparison
- [ ] Compare metrics to pre-deployment baseline
- [ ] Error rate not increased
- [ ] Response time not degraded
- [ ] Database query performance stable
- [ ] Memory footprint acceptable

---

## 🔒 Security Validation

### SSL/TLS
- [ ] HTTPS enforced (HTTP redirects to HTTPS)
- [ ] SSL certificate valid (not expired)
- [ ] Certificate chain complete
- [ ] TLS 1.2+ only (no SSL 3.0/TLS 1.0/1.1)
- [ ] Strong cipher suites configured
- [ ] HSTS header enabled

### API Security
- [ ] Authentication required (all endpoints protected)
- [ ] JWT tokens have short expiration
- [ ] Webhook signature validation working
- [ ] CORS headers correct (not *)
- [ ] Rate limiting active
- [ ] Input validation enforced
- [ ] Output encoding applied

### Data Protection
- [ ] Sensitive data encrypted at rest
- [ ] Sensitive data encrypted in transit
- [ ] Database credentials not in logs
- [ ] API keys not exposed in responses
- [ ] Customer data PII protected
- [ ] Webhook payloads not logged

### Compliance
- [ ] GDPR compliance checked (if EU users)
- [ ] CCPA compliance checked (if CA users)
- [ ] PCI-DSS compliance checked (if handling cards)
- [ ] SOC 2 requirements met
- [ ] Data retention policies enforced
- [ ] Audit logs enabled

---

## 📞 Post-Deployment (24-48 Hours)

### Monitoring
- [ ] Monitor error rate for 24h (should be < 0.1%)
- [ ] Monitor response times (should be baseline)
- [ ] Monitor resource usage (stable)
- [ ] Monitor webhook failures (should be 0)
- [ ] Monitor user feedback (no complaints)
- [ ] Check log aggregation (all logs present)

### Testing
- [ ] Smoke test all critical paths
- [ ] Test with actual production data
- [ ] Test user workflows end-to-end
- [ ] Test payment flow with test card
- [ ] Test webhook with actual Stripe events
- [ ] Test email notifications
- [ ] Test multi-tenant isolation

### Performance Review
- [ ] Database query performance acceptable?
- [ ] Cache hit rates good (> 80%)?
- [ ] Memory usage stable (no leaks)?
- [ ] CPU usage normal (< 50% baseline)?
- [ ] Network latency acceptable?
- [ ] Disk I/O normal?

### Documentation Update
- [ ] Update deployment runbook with actual times
- [ ] Document any issues encountered
- [ ] Update architecture diagram if changed
- [ ] Update API documentation if changed
- [ ] Document configuration changes
- [ ] Update team handbook

---

## 🚨 Rollback Plan

### When to Rollback
- [ ] Application startup fails
- [ ] Database migration fails
- [ ] API latency > 2 seconds
- [ ] Error rate > 1%
- [ ] Data corruption detected
- [ ] Security vulnerability discovered
- [ ] Major business feature broken

### Rollback Procedure
```bash
# 1. Stop current application
systemctl stop leadflow-backend

# 2. Restore previous version
docker pull leadflow:previous-version
docker run -d --restart=always ... leadflow:previous-version

# 3. Database rollback (if migrations caused issue)
# Restore from snapshot taken before deployment
# Or run Flyway undo (if available)

# 4. Validate rollback
curl http://localhost:8080/actuator/health

# 5. Notify team
# Post in #incidents Slack channel

# 6. Investigate root cause
# Review logs, check what went wrong
```

### Time to Recover
- Target RTO (Recovery Time Objective): 5 minutes
- Target RPO (Recovery Point Objective): < 5 minutes of data loss

---

## 📋 Sign-Off Checklist

### Development Team
- [ ] Code review approved by 2+ engineers
- [ ] All tests passing on CI/CD
- [ ] Feature flag available (for quick disable)
- [ ] Deployment runbook created
- [ ] Team trained on new features

### QA Team
- [ ] All test cases passed in staging
- [ ] Load testing completed
- [ ] Security testing completed
- [ ] User acceptance testing signed off
- [ ] No critical/major bugs open

### Operations Team
- [ ] Infrastructure prepared
- [ ] Monitoring configured
- [ ] Alerts tested
- [ ] Backup/recovery tested
- [ ] On-call rotation set up

### Product/Business
- [ ] Feature complete and tested
- [ ] Documentation ready
- [ ] Users/customers notified
- [ ] Support trained
- [ ] Go-live approval granted

### Final Approval
```
Deployment Manager: _________________________ Date: _______
Release Lead: _________________________ Date: _______
On-Call Engineer: _________________________ Date: _______
```

---

## 📱 Communication Template

### Pre-Deployment Notification
```
🚀 DEPLOYMENT NOTICE

We will be deploying LeadFlow Backend v1.2.0 to production.

⏰ DATE/TIME: [Date] [Time] UTC
⏱️ DURATION: ~15 minutes
🔧 EXPECTED IMPACT: None (backward compatible)

CHANGES:
- Implemented Stripe webhook validation
- Added email notifications
- Improved wallet performance

Please report any issues to #incidents on Slack.
```

### Post-Deployment Notification
```
✅ DEPLOYMENT COMPLETE

LeadFlow Backend v1.2.0 successfully deployed to production.

📊 STATUS: All systems operational
✔️ Health checks: PASSING
📈 Metrics: Normal
🔔 Monitoring: Active

Thanks for your patience during the deployment!
```

### Incident Notification (if needed)
```
🚨 INCIDENT ALERT

Application latency increased 10x after deployment.
Rolling back to previous version.

💬 Updates: #incidents channel
📞 On-call: [Name] [Phone]

ETA to resolution: 10 minutes
```

---

## 🎯 Success Criteria

Deployment is successful when:
- [ ] Application started without errors
- [ ] All health checks passing
- [ ] No increase in error rate
- [ ] No increase in response time
- [ ] All webhooks processed successfully
- [ ] All emails sent successfully
- [ ] No user complaints reported
- [ ] Monitoring showing baseline behavior

**Deployment considered stable when:**
- [ ] Above criteria met for 24 hours straight
- [ ] Load testing pass rates > 99%
- [ ] Zero critical issues reported

---

## 📞 Support Contacts

| Role | Name | Phone | Email |
|------|------|-------|-------|
| Deployment Manager | [Name] | [Phone] | [Email] |
| On-Call Engineer | [Name] | [Phone] | [Email] |
| Database Admin | [Name] | [Phone] | [Email] |
| Infrastructure | [Name] | [Phone] | [Email] |
| Product Lead | [Name] | [Phone] | [Email] |

---

## 🔗 Useful Links

- [Runbook](./RUNBOOK.md)
- [Architecture Diagram](./ARCHITECTURE.md)
- [API Documentation](/swagger-ui.html)
- [Database Schema](./database/schema.sql)
- [SMTP Setup Guide](./SMTP_SETUP_GUIDE.md)
- [Stripe Webhook Guide](./STRIPE_WEBHOOK_GUIDE.md)
- [Monitoring Dashboard](https://grafana.leadflow.com)
- [Error Tracking](https://sentry.leadflow.com)

---

_Version: 1.0 | Last updated: March 2026_
