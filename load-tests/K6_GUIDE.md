# 🚀 k6 Load Testing Guide - LeadFlow

Complete guide to running load tests against the LeadFlow Billing API.

## 📋 Quick Start

### Install k6
```bash
# Windows (Chocolatey)
choco install k6

# macOS (Homebrew)
brew install k6

# Or download from https://k6.io/docs/getting-started/installation/
```

### Run Basic Load Test
```bash
cd load-tests
k6 run webhook-load-test.js
```

### Run with Custom Duration
```bash
k6 run --duration 10m webhook-load-test.js
```

### Run with Custom Virtual Users
```bash
k6 run --vus 50 --duration 30s webhook-load-test.js
```

---

## 🔧 Configuration

### Environment Variables
```bash
# Set API endpoint
export API_BASE_URL=http://localhost:8080/api

# Set JWT token
export JWT_TOKEN=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# Set Stripe webhook secret
export WEBHOOK_SECRET=whsec_test_1234567890

# Set tenant ID
export TENANT_ID=550e8400-e29b-41d4-a716-446655440000

# Run test
k6 run webhook-load-test.js
```

### Or pass via command line
```bash
k6 run \
  --env API_BASE_URL=http://localhost:8080/api \
  --env JWT_TOKEN=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... \
  --env WEBHOOK_SECRET=whsec_test_1234567890 \
  webhook-load-test.js
```

---

## 📊 Test Scenarios

### Scenario 1: Baseline Performance
```bash
# Single user, local server, no concurrency
k6 run \
  --vus 1 \
  --iterations 10 \
  webhook-load-test.js
```

**Expected Results:**
- Response time: < 500ms
- Error rate: 0%
- Throughput: ~2 requests/second

### Scenario 2: Peak Hour Traffic
```bash
# Simulate peak hour (lunch time)
k6 run \
  --vus 50 \
  --duration 5m \
  webhook-load-test.js
```

**Expected Results:**
- P95 latency: < 500ms
- P99 latency: < 1000ms
- Error rate: < 1%
- Throughput: ~500-1000 requests/second

### Scenario 3: Stress Test (Find Breaking Point)
```bash
# Gradually increase load until system breaks
k6 run \
  --vus 100 \
  --duration 10m \
  webhook-load-test.js
```

**Expected Results:**
- Performance degrades gracefully
- No cascading failures
- Error rate increases slowly (not suddenly)
- Database connections stay manageable

### Scenario 4: Soak Test (Long Duration)
```bash
# Run at baseline load for extended period
k6 run \
  --vus 10 \
  --duration 1h \
  webhook-load-test.js
```

**Expected Results:**
- Memory stable (no leaks)
- GC pause times consistent
- Error rate constant (no degradation over time)
- Database connections don't grow unbounded

### Scenario 5: Spike Test
```bash
# Sudden traffic increase
k6 run \
  --vus 200 \
  --duration 30s \
  webhook-load-test.js
```

**Expected Results:**
- System recovers quickly
- No dropped connections
- Graceful degradation (slow, not broken)

---

## 📈 Test Execution Examples

### Run with Detailed Logs
```bash
k6 run \
  --vus 50 \
  --duration 60s \
  --summary-export=summary.json \
  --out csv=results.csv \
  webhook-load-test.js
```

**Outputs:**
- `summary.json`: JSON summary of results
- `results.csv`: Detailed metrics as CSV

### Run Multiple Times & Store Results
```bash
for i in {1..3}; do
  k6 run \
    --out csv=results-run-$i.csv \
    webhook-load-test.js
  sleep 60  # Wait 1 minute between runs
done
```

### Run with Threshold Validation
```bash
# Fails if thresholds not met
k6 run webhook-load-test.js
# Exit code will be non-zero if failed
```

---

## 📊 Understanding Results

### Summary Output
```
    checks........................: 98.5% ✓ 3940    ✗ 60
    data_received..................: 2.4 MB 1.6 kB/s
    data_sent.......................: 982 kB  654 B/s
    http_req_blocked...............: avg=1.2ms    min=0s      med=0s       max=45ms   p(90)=1ms     p(95)=2ms
    http_req_connecting............: avg=0.5ms    min=0s      med=0s       max=30ms   p(90)=0s      p(95)=0s
    http_req_duration..............: avg=234ms    min=12ms    med=201ms    max=512ms  p(90)=401ms   p(95)=450ms
    http_req_failed................: 1.3%   ✓ 65    ✗ 5135
    http_req_receiving.............: avg=1.2ms    min=0s      med=0s       max=102ms  p(90)=2ms     p(95)=3ms
    http_req_sending...............: avg=1.5ms    min=0s      med=1ms      max=45ms   p(90)=2ms     p(95)=3ms
    http_req_tls_handshaking.......: avg=0s       min=0s      med=0s       max=0s     p(90)=0s      p(95)=0s
    http_req_waiting...............: avg=230ms    min=10ms    med=198ms    max=508ms  p(90)=398ms   p(95)=448ms
    http_reqs......................: 5000   3.33/s
    iteration_duration.............: avg=2.04s    min=1.20s   med=2.01s    max=3.15s  p(90)=2.41s   p(95)=2.58s
    iterations.....................: 2500   1.67/s
    vus............................: 50     min=50    max=50
    vus_max........................: 50     min=50    max=50
    webhook_errors.................: 2.1%   ✗ 105
    webhook_latency................: avg=156ms    min=8ms     med=145ms    max=412ms  p(90)=289ms   p(95)=356ms
    webhooks_processed.............: 2500
```

### Metrics Breakdown

| Metric | Meaning | Target |
|--------|---------|--------|
| `checks` | Assertions that passed/failed | > 95% pass |
| `http_req_duration` | Total response time | p(95) < 500ms |
| `http_req_failed` | Percentage of failed requests | < 1% |
| `http_reqs` | Total requests per second | Baseline dependent |
| `iteration_duration` | Time per test iteration | < 3s |
| `webhook_latency` | Webhook-specific latency | < 300ms |
| `webhook_errors` | Webhook-specific error rate | < 2% |
| `webhooks_processed` | Total webhooks processed | > 0 |

### Example: Interpreting P95/P99 Latency
```
http_req_duration: avg=234ms, p(90)=401ms, p(95)=450ms, p(99)=520ms

Meaning:
- Average response time: 234ms ✅
- 90% of requests: < 401ms ✅
- 95% of requests: < 450ms ✅
- 99% of requests: < 520ms ⚠️ (close to limit)
- 1% of requests: > 520ms (outliers)

Acceptable if P95 < 500ms, otherwise investigate slow requests.
```

---

## 🎯 Performance Targets

### API Response Time Targets
| Endpoint | Target P95 | Target P99 |
|----------|-----------|-----------|
| POST /checkout | < 400ms | < 700ms |
| POST /webhooks/stripe | < 300ms | < 500ms |
| GET /actuator/health | < 50ms | < 100ms |
| GET /actuator/prometheus | < 200ms | < 400ms |

### Overall Test Targets
| Metric | Target |
|--------|--------|
| Success rate | > 99% |
| P95 latency | < 500ms |
| P99 latency | < 1000ms |
| Throughput | > 1000 req/s |
| Error rate | < 1% |
| Webhook error rate | < 2% |

---

## 🔍 Analyzing Results

### Step 1: Check Overall Health
```bash
# Does test show PASS or FAIL?
# Check "checks" section - want > 95% success
```

### Step 2: Review Latency
```bash
# Check p(95) and p(99) against targets
# If exceeding targets, investigate:
#   - Database slow queries
#   - Network latency
#   - GC pauses
```

### Step 3: Identify Error Patterns
```bash
# Check which endpoints fail most
# Check error messages (look in logs)
# Determine if errors are:
#   - Connection timeouts (infrastructure issue)
#   - 500 errors (application bug)
#   - 429 errors (rate limiting hit)
```

### Step 4: Calculate Throughput
```bash
# Formula: http_reqs / duration
# https_reqs = 5000, duration = 25 min
# Throughput = 5000 / (25 * 60) = 3.33 req/s

# For 50 concurrent users (VUs):
# Per-user throughput = 3.33 / 50 = 0.067 req/s per user
```

---

## 🐛 Troubleshooting

### "Failed to connect to server"
```bash
❌ Error: dial tcp [::1]:8080: connect: connection refused
✅ Solution:
  1. Start the application: mvn spring-boot:run
  2. Wait for it to be ready (~10 seconds)
  3. Re-run test
  4. Check API_BASE_URL is correct
```

### "Connection reset by peer"
```bash
❌ Error: i/o error: Connection reset by peer
✅ Solutions:
  - Check server logs for crashes
  - Verify server has memory (GC issue?)
  - Reduce VU count (not enough database connections)
  - Increase connection pool size
```

### "Read timeout"
```bash
❌ Error: read tcp: i/o timeout
✅ Solutions:
  - Requests too slow, increase server resources
  - Database queries too slow, add indexes
  - Network latency, reduce test duration
  - Increase timeout in script
```

### "Invalid JWT token"
```bash
❌ Error: 401 Unauthorized
✅ Solution:
  1. Generate valid JWT token
  2. Set JWT_TOKEN env var
  3. Ensure token not expired
```

### "Results are inconsistent"
```bash
❌ Multiple runs show very different results (>20% variance)
✅ Solutions:
  - Warm up server first (first run discarded)
  - Run on idle machine (close other apps)
  - Increase test duration (at least 30 seconds)
  - Run multiple times and average results
```

---

## 📊 Exporting & Comparing Results

### Export to JSON
```bash
k6 run \
  --summary-export=summary.json \
  webhook-load-test.js
```

### Compare Two Runs
```bash
# Run 1 (baseline)
k6 run -o json=baseline.json webhook-load-test.js

# Run 2 (after optimization)
k6 run -o json=optimized.json webhook-load-test.js

# Compare with jq
jq '.metrics.http_req_duration.values.p95' baseline.json
jq '.metrics.http_req_duration.values.p95' optimized.json
```

### Acceptable Performance Variance
- Expected variance between runs: 5-10%
- Investigation needed if variance: > 20%
- Consistent degradation: Potential regression

---

## ⚡ Advanced: Custom Thresholds

### Modify script to add stricter thresholds
```javascript
export const options = {
  thresholds: {
    'http_req_duration': ['p(95)<400', 'p(99)<600'],  // Stricter
    'http_req_failed': ['rate<0.005'],                 // < 0.5% errors
    'checkout_errors': ['rate<0.02'],                  // < 2% errors
  },
};
```

### Run and fail if thresholds not met
```bash
k6 run webhook-load-test.js
echo "Exit code: $?"  # 0 = pass, 1 = fail
```

---

## 🎬 Continuous Performance Testing

### CI/CD Integration (GitHub Actions)
```yaml
name: Load Test

on: [push]

jobs:
  load-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: grafana/k6-action@v0.3.0
        with:
          filename: load-tests/webhook-load-test.js
          env: |
            API_BASE_URL=http://localhost:8080/api
            JWT_TOKEN=${{ secrets.JWT_TOKEN }}
```

### Nightly Soak Tests
```bash
#!/bin/bash
# Run 1-hour soak test nightly
k6 run \
  --vus 10 \
  --duration 1h \
  --summary-export=results-$(date +%Y%m%d).json \
  load-tests/webhook-load-test.js
```

---

## 📚 Resources

- [k6 Official Documentation](https://k6.io/docs/)
- [k6 API Reference](https://k6.io/docs/javascript-api/)
- [k6 Examples](https://k6.io/docs/examples/)
- [Best Practices](https://k6.io/docs/testing-guides/)

---

_Last updated: March 2026_
