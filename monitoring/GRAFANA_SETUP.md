# 📊 Grafana Monitoring Dashboard - LeadFlow

Complete guide to setting up Grafana for LeadFlow Billing API monitoring.

## 🚀 Quick Start

### Option 1: Docker Compose (Easiest)
```bash
cd monitoring
docker-compose up -d
# Grafana: http://localhost:3000
# Prometheus: http://localhost:9090
```

### Option 2: Manual Installation

#### A. Install Prometheus
```bash
# Download
wget https://github.com/prometheus/prometheus/releases/download/v2.45.0/prometheus-2.45.0.linux-amd64.tar.gz
tar xvfz prometheus-2.45.0.linux-amd64.tar.gz
cd prometheus-2.45.0.linux-amd64

# Configure (see below)
cat > prometheus.yml << 'EOF'
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'leadflow-backend'
    metrics_path: '/api/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
EOF

# Start
./prometheus --config.file=prometheus.yml
```

#### B. Install Grafana
```bash
# Ubuntu/Debian
sudo apt-get install -y software-properties-common
sudo add-apt-repository "deb https://packages.grafana.com/oss/deb stable main"
sudo apt-get update
sudo apt-get install grafana-server

# Start service
sudo systemctl start grafana-server
sudo systemctl enable grafana-server

# Access: http://localhost:3000 (admin/admin)
```

#### C. macOS (Homebrew)
```bash
# Install
brew install prometheus grafana

# Start
brew services start prometheus
brew services start grafana
```

---

## ⚙️ Prometheus Configuration

### prometheus.yml
```yaml
global:
  scrape_interval: 15s            # Default scrape interval
  evaluation_interval: 15s        # How often to evaluate rules
  external_labels:
    cluster: 'production'
    environment: 'prod'

# Alertmanager configuration
alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - localhost:9093

# Load rules
rule_files:
  - "alert_rules.yml"
  - "recording_rules.yml"

scrape_configs:
  # LeadFlow Backend Metrics
  - job_name: 'leadflow-backend'
    metrics_path: '/api/actuator/prometheus'
    scrape_interval: 5s
    scrape_timeout: 3s
    static_configs:
      - targets: ['localhost:8080']
        labels:
          service: 'billing-api'

  # Prometheus self-monitoring
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
```

### alert_rules.yml
```yaml
groups:
  - name: webhook_alerts
    interval: 1m
    rules:
      - alert: HighWebhookErrorRate
        expr: rate(webhook_processing_failure_total[5m]) > 0.05
        for: 5m
        annotations:
          summary: "High webhook error rate (>5%)"
          description: "Webhook error rate: {{ $value }}"

      - alert: WebhookLatencyHigh
        expr: histogram_quantile(0.95, webhook_processing_duration_seconds) > 0.5
        for: 5m
        annotations:
          summary: "Webhook P95 latency > 500ms"
          description: "P95 latency: {{ $value | humanizeDuration }}"

      - alert: SignatureValidationFailures
        expr: increase(webhook_signature_validation_total{status='failure'}[5m]) > 10
        for: 5m
        annotations:
          summary: "Multiple signature validation failures detected"

      - alert: NoWebhooksProcessed
        expr: increase(webhook_processing_total[5m]) == 0
        for: 10m
        annotations:
          summary: "No webhooks processed in last 5 minutes"
```

---

## 🔧 Grafana Configuration

### Step 1: Initial Login
1. Open http://localhost:3000
2. Login: `admin` / `admin`
3. Change password (recommended)

### Step 2: Add Prometheus Data Source
1. **Configuration** → **Data Sources**
2. Click **Add data source**
3. Select **Prometheus**
4. Configure:
   ```
   Name: Prometheus
   URL: http://localhost:9090
   Scrape interval: 15s
   ```
5. Click **Save & Test**

Expected message: `Data source is working`

### Step 3: Import Dashboard

#### Option A: Import from JSON
1. **Dashboards** → **Import**
2. Upload `monitoring/grafana-dashboard-billing.json`
3. Select Prometheus data source
4. Click **Import**

#### Option B: Create Manually
1. **Dashboards** → **New**
2. Add panels (see dashboard details below)
3. Save dashboard

### Step 4: Configure Dashboard Variables

In dashboard settings, add variables:

```
Variable: interval
Type: Custom
Values: 1m,5m,15m,30m,1h
Default: 5m
```

---

## 📊 Dashboard Panels

### Panel 1: Webhook Success Rate
**Type:** Time Series Graph
**Query:**
```promql
sum(rate(webhook_processing_success_total[5m])) / sum(rate(webhook_processing_total[5m])) * 100
```
**Legend:** Success Rate %
**Y-Axis:** Percent (0-100)
**Alert Threshold:** < 95% = yellow, < 90% = red

### Panel 2: Error Rate by Endpoint
**Type:** Time Series Graph
**Query A:**
```promql
rate(webhook_processing_failure_total[5m]) / rate(webhook_processing_total[5m]) * 100
```
**Query B:**
```promql
rate(checkout_errors[5m]) / rate(http_requests_total{endpoint='/checkout'}[5m]) * 100
```
**Legend Format:** {{ job }}
**Y-Axis:** Percent (0-10)

### Panel 3: Webhook Latency Percentiles
**Type:** Table
**Query A:** (P50)
```promql
histogram_quantile(0.50, webhook_processing_duration_seconds) * 1000
```
**Query B:** (P95)
```promql
histogram_quantile(0.95, webhook_processing_duration_seconds) * 1000
```
**Query C:** (P99)
```promql
histogram_quantile(0.99, webhook_processing_duration_seconds) * 1000
```
**Unit:** Milliseconds
**Thresholds:** 300ms (yellow), 500ms (red)

### Panel 4: Webhook Throughput (Gauge)
**Type:** Gauge
**Query:**
```promql
sum(rate(webhook_processing_total[1m])) * 60
```
**Unit:** ops (operations/minute)
**Thresholds:** Green (>100), Yellow (>50), Red (<50)

### Panel 5: Signature Validation
**Type:** Time Series Graph
**Query:**
```promql
sum(webhook_signature_validation_total{status='success'}) / sum(webhook_signature_validation_total) * 100
```
**Legend:** Valid Signatures %
**Y-Axis:** Percent (90-100)
**Alert:** < 99% = red

### Panel 6: Email Delivery Status
**Type:** Bar Chart
**Query A:**
```promql
increase(email_sent_total[5m])
```
**Query B:**
```promql
increase(email_failed_total[5m])
```
**Query C:**
```promql
increase(email_bounced_total[5m])
```
**Legend:** Sent, Failed, Bounced
**Stack series:** Yes

---

## 🔔 Alerting Setup

### Install Alertmanager
```bash
# Download
wget https://github.com/prometheus/alertmanager/releases/download/v0.26.0/alertmanager-0.26.0.linux-amd64.tar.gz
tar xvfz alertmanager-0.26.0.linux-amd64.tar.gz
cd alertmanager-0.26.0.linux-amd64

# Configure (see below)
# Start
./alertmanager --config.file=alertmanager.yml
```

### alertmanager.yml Configuration
```yaml
global:
  resolve_timeout: 5m
  slack_api_url: 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'

templates:
  - '/etc/alertmanager/templates/*.tmpl'

route:
  receiver: 'default'
  group_by: ['alertname']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 1h

receivers:
  - name: 'default'
    slack_configs:
      - channel: '#alerts'
        title: 'Alert: {{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'
        send_resolved: true
```

---

## 📈 Dashboard Usage

### Viewing Real-time Metrics
1. Open dashboard
2. Change time range (top right):
   - Last 1 hour
   - Last 6 hours
   - Last 24 hours
   - Last 7 days
3. Adjust refresh rate:
   - 5s (development)
   - 30s (production)
   - 5m (historical analysis)

### Creating Custom Alerts
1. Open Panel → Edit
2. Go to **Alert** tab
3. Set condition:
   ```
   When: average()
   Of: query_result
   Is above: 0.05 (5% error rate)
   For: 5m
   ```
4. Add notification channel (Slack, Email, etc)
5. Save

### Exporting Panel Data
1. Click panel → **Export** → **Data**
2. Choose format: CSV, JSON
3. Download and analyze

---

## 🛠️ Troubleshooting

### Dashboard shows "No Data"
```
❌ Problem: All graphs empty
❌ Possible causes:
  1. Prometheus not running
  2. Metrics not collected
  3. Query incorrect

✅ Solution:
  1. Check Prometheus: http://localhost:9090
  2. Verify Targets: Status → Targets (v~all green)
  3. Try simpler query: up{job="leadflow-backend"}
  4. Check query syntax in Prometheus UI first
```

### Metrics not appearing in Prometheus
```
❌ Problem: Targets show "DOWN"
❅ Possible causes:
  1. Application not running
  2. Metrics endpoint blocked
  3. Port wrong

✅ Solution:
  curl http://localhost:8080/api/actuator/prometheus
  # Should return Prometheus metrics
```

### Slow dashboard performance
```
❌ Problem: Graphs lag, takes minutes to load
✅ Solution:
  1. Reduce query time range
  2. Increase scrape interval in prometheus.yml
  3. Use recording rules for common queries
  4. Reduce number of panels
```

### Alert not firing
```
❌ Problem: Alert condition met but no notification
✅ Solution:
  1. Verify Prometheus scrapes data: up == 1
  2. Test alert query manually in Prometheus UI
  3. Check Alertmanager: http://localhost:9093
  4. Verify notification channel configured
```

---

## 📜 Prometheus Query Examples

### Webhook Metrics
```promql
# Success rate last 5 minutes
sum(rate(webhook_processing_success_total[5m])) / sum(rate(webhook_processing_total[5m]))

# Errors per minute
rate(webhook_processing_failure_total[1m]) * 60

# P95 latency
histogram_quantile(0.95, webhook_processing_duration_seconds)

# Signature validation failures
increase(webhook_signature_validation_total{status='failure'}[5m])
```

### Email Metrics
```promql
# Email send rate
rate(email_sent_total[1m]) * 60

# Email failure rate
rate(email_failed_total[1m]) / rate(email_sent_total[1m])

# Total emails sent today
increase(email_sent_total[1d])
```

### System Metrics
```promql
# JVM memory usage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# GC time per minute
rate(jvm_gc_pause_seconds_sum[1m])

# Request latency
histogram_quantile(0.95, http_request_duration_seconds_bucket)
```

---

## 📚 Resources

- [Prometheus Query Language](https://prometheus.io/docs/prometheus/latest/querying/)
- [Grafana Documentation](https://grafana.com/docs/grafana/latest/)
- [PromQL Best Practices](https://prometheus.io/docs/practices/queries/)
- [Metrics Summary](../docs/API_BILLING_SPEC.md#metrics)

---

_Last updated: March 2026_
