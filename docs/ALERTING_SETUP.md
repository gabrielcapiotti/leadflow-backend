# 🔔 Alerting Setup Guide - LeadFlow

Complete guide to configuring Slack and Email alerting for the LeadFlow Billing API.

## 📋 Quick Start

### Enable Slack Alerting (2 min)
1. Create Slack Webhook: https://api.slack.com/messaging/webhooks
2. Add to `.env`:
   ```bash
   SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
   ```
3. Restart application
4. Alerts now sent to Slack!

### Enable Email Alerting (1 min)
1. Configure SMTP (see [SMTP_SETUP_GUIDE.md](./SMTP_SETUP_GUIDE.md))
2. Add to `.env`:
   ```bash
   ALERT_EMAIL_TO=your-email@example.com
   ```
3. Restart application
4. Alerts now sent via email!

---

## 🎛️ Configuration

### Slack Configuration

#### Create Slack Webhook
1. Go to https://api.slack.com/apps
2. Create New App → From scratch
3. Choose workspace
4. Navigate to Incoming Webhooks
5. Activate Incoming Webhooks
6. Add New Webhook to Workspace
7. Select target channel (recommend #alerts)
8. Copy Webhook URL

#### .env Configuration
```bash
# Slack
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/T123/B456/XXX

# Alert thresholds (optional, defaults shown)
ALERT_WEBHOOK_ERROR_RATE_THRESHOLD=0.05        # 5% error rate
ALERT_WEBHOOK_LATENCY_THRESHOLD_MS=500         # 500ms P95 latency
ALERT_WEBHOOK_NO_EVENT_THRESHOLD_MINS=10       # 10 minutes no webhooks
ALERT_DATABASE_POOL_SATURATION_THRESHOLD=0.85  # 85% connection pool
ALERT_MEMORY_USAGE_THRESHOLD=0.90              # 90% memory
ALERT_MONITOR_INTERVAL_MS=30000                # Check every 30 seconds
```

#### Example Slack Message
```
┌─────────────────────────────────────────┐
│ LEADFLOW-BACKEND - PRODUCTION           │
│                                         │
│ 🚨 High Webhook Error Rate               │
│ 7.23% (145 errors)                      │
│ 2026-03-10T12:30:45Z                    │
│                                         │
│ Error Rate: 7.23%                       │
│ Total Errors: 145                       │
│ Time: 2026-03-10T12:30:45.123Z          │
└─────────────────────────────────────────┘
```

### Email Configuration

#### .env Configuration
```bash
# SMTP (see SMTP_SETUP_GUIDE.md for detailed setup)
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=SG.YOUR_SENDGRID_API_KEY

# Alert Email
ALERT_EMAIL_TO=ops-team@leadflow.com
SPRING_MAIL_FROM=alerts@leadflow.com
```

#### Example Alert Email
```
Subject: [ALERT] High Webhook Error Rate: 7.23% - leadflow-backend

To: ops-team@leadflow.com

ALERT: High Webhook Error Rate
Severity: HIGH
Error Rate: 7.23%
Total Errors: 145
Time: 2026-03-10 12:30:45 UTC
Environment: PRODUCTION

Details:
Check webhook processing logs for details

Action:
1. Review webhook error logs
2. Check Stripe webhook connectivity
3. Verify database query performance
```

---

## 🔧 Alert Types & Thresholds

### Webhook Error Rate
- **Threshold:** 5% (configurable)
- **Trigger:** When error rate exceeds threshold for 1 minute
- **Alert Frequency:** Maximum every 5 minutes
- **Action Items:**
  - Check application logs
  - Verify Stripe connection
  - Check database performance

### Webhook Processing Latency
- **Threshold:** 500ms P95 (configurable)
- **Trigger:** When P95 latency exceeds threshold
- **Alert Frequency:** Maximum every 5 minutes
- **Action Items:**
  - Check database queries
  - Monitor GC pauses
  - Check network latency

### No Webhooks Processed
- **Threshold:** 10 minutes (configurable)
- **Trigger:** When no webhooks received for threshold duration
- **Alert Frequency:** Maximum every 15 minutes (critical alert)
- **Action Items:**
  - Verify Stripe webhook is configured
  - Check network connectivity
  - Verify application is running
  - Restart webhook listener if needed

### Signature Validation Failures
- **Threshold:** 10+ failures in a window
- **Trigger:** Multiple signature validation failures
- **Alert Frequency:** Immediate
- **Action Items:**
  - Verify Stripe webhook secret
  - Check for webhook spoofing attempts
  - Review security logs

### Email Delivery Failures
- **Threshold:** > 5 failures
- **Trigger:** Multiple email send failures
- **Alert Frequency:** Per occurrence
- **Action Items:**
  - Check SMTP configuration
  - Verify email server is up
  - Check email provider status
  - Review bounce list

### Memory Usage
- **Threshold:** 90% (configurable)
- **Trigger:** When JVM memory usage exceeds threshold
- **Alert Frequency:** Per minute while exceeding
- **Action Items:**
  - Increase JVM heap size
  - Check for memory leaks
  - Review object allocations
  - Restart application if necessary

---

## 🔐 Securing Alert Endpoints

### Slack Webhook Security
```bash
# DO:
✅ Store webhook URL in environment variable
✅ Rotate webhook URL quarterly
✅ Use separate channel for critical alerts
✅ Monitor webhook access logs

# DON'T:
❌ Hardcode webhook URL in code
❌ Share webhook URL via email/chat
❌ Use same webhook for multiple environments
❌ Log webhook URL in application logs
```

### Email Alert Security
```bash
# DO:
✅ Use TLS/SSL for SMTP connection
✅ Store credentials in environment variables
✅ Use dedicated alert email account
✅ Monitor email delivery logs

# DON'T:
❌ Send alerts to public/shared email
❌ Hardcode credentials in code
❌ Use unencrypted SMTP connection
❌ Log recipient email addresses
```

---

## 📝 Alert Template Customization

### Create Custom Template
1. Create file: `src/main/resources/templates/alert/custom-alert.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; }
        .alert { background: #f8d7da; border: 1px solid #f5c6cb; padding: 15px; }
        .title { font-size: 18px; font-weight: bold; color: #721c24; }
        .details { margin-top: 10px; }
    </style>
</head>
<body>
    <div class="alert">
        <div class="title" th:text="${alertTitle}"></div>
        <div class="details">
            <p>Severity: <strong th:text="${severity}"></strong></p>
            <p th:text="${details}"></p>
            <p>Time: <em th:text="${timestamp}"></em></p>
        </div>
    </div>
</body>
</html>
```

2. Use in code:
```java
emailAlertService.sendCustomAlert("alert/custom-alert", variables);
```

---

## 🧪 Testing Alerts

### Test Slack Alert Manually
```bash
curl -X POST -H 'Content-type: application/json' \
  --data '{"text":"Test alert from LeadFlow"}' \
  https://hooks.slack.com/services/YOUR/WEBHOOK/URL
```

**Expected Response:**
```
ok
```

### Test Email Alert Manually
```java
// In your controller or test
@Autowired
private EmailAlertService emailAlertService;

@PostMapping("/test-alert")
public void testAlert() {
    emailAlertService.alertHighWebhookErrorRate(
        0.07,  // 7% error rate
        145,   // 145 errors
        "Test alert triggered"
    );
}
```

### Monitor Alert Delivery
```bash
# Check Slack message delivery
# View #alerts channel in Slack

# Check email delivery
# Check inbox for alert emails
# Check spam folder if not received
```

---

## 🔍 Troubleshooting

### "Alert not sending to Slack"
```
❌ Problem: Slack alerts not appearing
❌ Possible causes:
  1. Webhook URL not configured
  2. Webhook URL expired
  3. Channel no longer exists
  4. Network connectivity issue

✅ Solution:
  1. Check SLACK_WEBHOOK_URL in .env
  2. Test webhook with curl (see above)
  3. Verify channel still exists in Slack
  4. Check application logs for errors
  5. Regenerate webhook if expired
```

### "Alert not sending to Email"
```
❌ Problem: Email alerts not arriving
❌ Possible causes:
  1. SMTP not configured
  2. Email address invalid
  3. Email being marked as spam
  4. Email provider rejecting

✅ Solution:
  1. Verify MAIL_HOST, MAIL_PORT, credentials
  2. Check ALERT_EMAIL_TO is correct
  3. Check spam folder
  4. Verify SMTP credentials work
  5. Test with curl if using SendGrid:
     curl -X POST https://api.sendgrid.com/v3/mail/send \
       -H 'Authorization: Bearer SG.YOUR_KEY' ...
```

### "Too many alerts / Alert fatigue"
```
❌ Problem: Getting too many alerts, can't keep up
✅ Solution:
  1. Increase alert thresholds:
     - ALERT_WEBHOOK_ERROR_RATE_THRESHOLD=0.10 (10% instead of 5%)
     - ALERT_WEBHOOK_LATENCY_THRESHOLD_MS=1000 (1s instead of 500ms)
  2. Reduce alert frequency:
     - ALERT_MONITOR_INTERVAL_MS=60000 (60s instead of 30s)
  3. Filter alerts:
     - Send only CRITICAL to email
     - Send all to Slack
  4. Create alert rules:
     - Only alert during business hours
     - Adjust thresholds by time of day
```

### "Alert template not rendering"
```
❌ Error: "Unable to process template"
✅ Solution:
  1. Check template file exists: src/main/resources/templates/alert/
  2. Verify Thymeleaf namespace: xmlns:th="http://www.thymeleaf.org"
  3. Check variable names match template expressions
  4. Verify template syntax (use [[${var}]] for expressions)
```

---

## 📊 Recommended Alert Rules

### Development Environment
```bash
ALERT_WEBHOOK_ERROR_RATE_THRESHOLD=0.20        # 20%
ALERT_WEBHOOK_LATENCY_THRESHOLD_MS=2000        # 2000ms
ALERT_WEBHOOK_NO_EVENT_THRESHOLD_MINS=30       # 30 minutes
ALERT_MONITOR_INTERVAL_MS=60000                # 60 seconds
# Send to: Slack only (development channel)
```

### Production Environment
```bash
ALERT_WEBHOOK_ERROR_RATE_THRESHOLD=0.05        # 5%
ALERT_WEBHOOK_LATENCY_THRESHOLD_MS=500         # 500ms
ALERT_WEBHOOK_NO_EVENT_THRESHOLD_MINS=10       # 10 minutes
ALERT_DATABASE_POOL_SATURATION_THRESHOLD=0.85  # 85%
ALERT_MEMORY_USAGE_THRESHOLD=0.90              # 90%
ALERT_MONITOR_INTERVAL_MS=30000                # 30 seconds
# Send to: Both Slack (#incidents) and Email (ops-team)
```

---

## 📞 Escalation Procedures

### By Alert Severity

#### 🟢 INFO (no action required)
- Logged only
- Example: "Webhook processed successfully"

#### 🟡 WARNING (monitor)
- Slack: #alerts channel
- Email: ops-team (next business day OK)
- Action: Investigate within 1 hour

#### 🔴 ERROR (urgent)
- Slack: @oncall tag in #incidents
- Email: ops-team immediately
- Action: Investigate within 15 minutes

#### 🟣 CRITICAL (page engineer)
- Slack: @channel in #incidents  
- Email: ops-team + security@
- SMS: On-call engineer
- Action: Immediate investigation

---

## 📈 Monitoring Alert Health

### Check Alert Service Status
```bash
# Via Prometheus
# Query: up{job="leadflow-backend"}

# Via Health Endpoint
curl http://localhost:8080/api/actuator/health/alerts

# Expected Response
{
  "status": "UP",
  "slack": {
    "status": "UP",
    "configured": true
  },
  "email": {
    "status": "UP",
    "configured": true
  }
}
```

### Alert Metrics
```bash
# Slack alerts sent
rate(alerts_slack_sent_total[5m])

# Email alerts sent
rate(alerts_email_sent_total[5m])

# Alert failures
rate(alerts_failed_total[5m])
```

---

## 📚 Resources

- [Slack API Documentation](https://api.slack.com)
- [Slack Incoming Webhooks](https://api.slack.com/messaging/webhooks)
- [SendGrid Documentation](https://docs.sendgrid.com)
- [SMTP Setup Guide](./SMTP_SETUP_GUIDE.md)

---

_Last updated: March 2026_
