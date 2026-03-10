# 📊 JMH Performance Benchmarks - LeadFlow

Java Microbenchmark Harness (JMH) benchmarks for critical paths.

## 📋 Quick Start

### Run All Benchmarks
```bash
mvn clean test -Dtest=*Benchmark -DargLine="-verbose:gc"
```

### Run Specific Benchmark
```bash
# HMAC validation benchmarks
mvn clean test -Dtest=StripeWebhookHmacBenchmark

# Webhook processing benchmarks
mvn clean test -Dtest=WebhookProcessingBenchmark

# Email rendering benchmarks
mvn clean test -Dtest=EmailRenderingBenchmark
```

### Export Results to CSV
```bash
mvn test -Dtest=StripeWebhookHmacBenchmark \
  -Djmh.results.file=target/benchmark-results.csv
```

---

## 🔐 1. Stripe Webhook HMAC Benchmark

### Purpose
Measure performance of HMAC-SHA256 signature validation for Stripe webhooks.

### Benchmarks Included

| Benchmark | Purpose | Expected Result |
|-----------|---------|-----------------|
| `benchmarkComputeHmac` | Raw HMAC computation | ~10-15 µs |
| `benchmarkVerifySignature` | Signature verification with constant-time comparison | ~20-30 µs |
| `benchmarkNaiveComparison` | String.equals() (VULNERABLE) | ~5 µs (but unsafe!) |
| `benchmarkPayloadExtraction` | Extracting data from payload | ~1-2 µs |
| `benchmarkFullValidation` | Complete validation (compute + verify + timestamp check) | ~25-40 µs |
| `benchmarkPayloadSizeImpact` | Tests with different payload sizes (100, 500, 1000, 5000 bytes) | Size-dependent |

### Running HMAC Benchmark
```bash
mvn clean test -Dtest=StripeWebhookHmacBenchmark

# Verbose output
mvn clean test -Dtest=StripeWebhookHmacBenchmark -X
```

### Interpreting Results

**Example Output:**
```
Benchmark                                    Mode  Cnt    Score    Error  Units
StripeWebhookHmacBenchmark.benchmarkComputeHmac  avgt   10   12.345 ±  0.456  us/op
StripeWebhookHmacBenchmark.benchmarkVerifySignature  avgt   10   28.567 ±  1.234  us/op
StripeWebhookHmacBenchmark.benchmarkFullValidation  avgt   10   35.678 ±  1.567  us/op
```

**Score Breakdown:**
- `Score`: Average time in microseconds per operation
- `Error`: Standard deviation (±)
- `Units`: us/op = microseconds per operation

**Performance Analysis:**
- ✅ HMAC computation < 20 µs → Excellent
- ✅ Full validation < 50 µs → Acceptable
- ⚠️ Constant-time comparison slightly slower than naive, but REQUIRED for security

### Performance Targets

| Operation | Target | Why Important |
|-----------|--------|---------------|
| Compute HMAC | < 20 µs | Minimal overhead |
| Verify Signature | < 30 µs | Must be fast for high volume |
| Full Validation | < 50 µs | At 1000 webhooks/sec = 50ms total |
| Payload Processing | < 100 µs | Including all validation |

### Expected Payload Size Impact
```
Payload Size:     100 bytes  →  ~10 µs
Payload Size:     500 bytes  →  ~12 µs
Payload Size:    1000 bytes  →  ~14 µs
Payload Size:    5000 bytes  →  ~15 µs

(HMAC is O(n), but data copying dominates)
```

---

## 🔄 2. Webhook Processing Benchmark

### Purpose
Measure performance of webhook event processing (parsing, routing, persistence).

### Benchmarks Included

| Benchmark | Purpose | Expected Result |
|-----------|---------|-----------------|
| `benchmarkJsonParsing` | Jackson JSON parsing | ~20-30 µs |
| `benchmarkEventRouting` | Pattern matching event type | ~2-5 µs |
| `benchmarkDataExtraction` | Extracting fields from JSON | ~5-10 µs |
| `benchmarkProcessSubscriptionCreated` | Full subscription processing | ~50-100 µs |
| `benchmarkProcessPaymentFailed` | Payment failure processing | ~40-80 µs |
| `benchmarkFullWebhookProcessing` | Complete pipeline | ~100-150 µs |
| `benchmarkMultiEventProcessing` | Processing 3 events sequentially | ~60-90 µs |
| `benchmarkDatabaseInsert` | Simulated DB insert | ~5-10 µs |

### Running Webhook Processing Benchmark
```bash
mvn clean test -Dtest=WebhookProcessingBenchmark
```

### Interpreting Results

**Example Output:**
```
Benchmark                                    Mode  Cnt    Score     Error  Units
WebhookProcessingBenchmark.benchmarkJsonParsing  avgt   10   22.456 ±  1.234  us/op
WebhookProcessingBenchmark.benchmarkEventRouting  avgt   10    3.567 ±  0.234  us/op
WebhookProcessingBenchmark.benchmarkDataExtraction  avgt   10    7.890 ±  0.456  us/op
WebhookProcessingBenchmark.benchmarkFullWebhookProcessing  avgt   10  125.678 ±  5.234  us/op
```

### Performance Targets

| Operation | Target | Reasoning |
|-----------|--------|-----------|
| JSON Parsing | < 50 µs | Largest component of processing |
| Event Routing | < 10 µs | Negligible, just pattern matching |
| Data Extraction | < 20 µs | Simple field access |
| Full Processing | < 200 µs | 1000 webhooks/sec = 200ms total (acceptable) |
| Database Insert | < 50 µs | In-memory operation (actual DB slower) |

### Throughput Calculation
```
If full webhook processing = 125 µs per event
Throughput = 1,000,000 µs/s ÷ 125 µs = 8,000 webhooks/sec

This is EXCELLENT. In practice:
- DB queries: +5-50 ms
- Email sending: +500-2000 ms (async)
- Real throughput: ~100-1000 webhooks/sec (still very good)
```

### Scaling Analysis
```
Single Thread:        8,000 events/sec
With 4 threads:      32,000 events/sec
With 8 threads:      64,000 events/sec

(accounting for GC pauses and context switching)
```

---

## 📧 3. Email Rendering Benchmark

### Purpose
Measure performance of Thymeleaf email template rendering.

### Benchmarks Included

| Benchmark | Purpose | Expected Result |
|-----------|---------|-----------------|
| `benchmarkRenderConfirmationEmail` | Render subscription confirmation | ~200-400 µs |
| `benchmarkRenderFailureEmail` | Render payment failure email | ~180-350 µs |
| `benchmarkRenderInvoiceEmail` | Render invoice email | ~220-420 µs |
| `benchmarkContextCreation` | Create variable context | ~5-10 µs |
| `benchmarkSimpleStringTemplate` | Simple string substitution | ~1-2 µs |
| `benchmarkHtmlGeneration` | StringBuilder HTML building | ~5-10 µs |
| `benchmarkBatchEmailRendering` | Render 10 emails in sequence | ~2-4 ms |
| `benchmarkComplexEmailRendering` | Render with nested objects | ~300-500 µs |

### Running Email Rendering Benchmark
```bash
mvn clean test -Dtest=EmailRenderingBenchmark
```

### Interpreting Results

**Example Output:**
```
Benchmark                                    Mode   Cnt    Score     Error  Units
EmailRenderingBenchmark.benchmarkRenderConfirmationEmail  avgt   10  312.456 ±  15.234  us/op
EmailRenderingBenchmark.benchmarkRenderFailureEmail  avgt   10  287.890 ±  12.567  us/op
EmailRenderingBenchmark.benchmarkContextCreation  avgt   10    7.234 ±  0.456  us/op
EmailRenderingBenchmark.benchmarkBatchEmailRendering  avgt   10 3245.678 ±  85.234  us/op
```

### Performance Targets

| Operation | Target | Reasoning |
|-----------|--------|-----------|
| Single Email Render | < 500 µs | OK for async processing |
| Context Creation | < 20 µs | Negligible overhead |
| Batch (10 emails) | < 5 ms | ~500 µs/email average |
| Total Email Send (with SMTP) | < 2 seconds | Async, background task |

### Email Processing Pipeline
```
1. Webhook received: 125 µs
2. Parse & validate: 50 µs
3. Create context: 10 µs
4. Render template: 300 µs
5. Send via SMTP: 500-2000 ms (async, doesn't block webhook)
   Total synchronous time: < 500 µs
```

### Caching Impact
Expected results assume:
- ✅ Template caching enabled (first load: ~500 µs, cached: ~200 µs)
- ✅ Thymeleaf on warmed JVM
- ❌ Cold startup time would be 2-3x longer

---

## 📈 Comparison: Before vs After Optimization

### Typical Improvements
```
HMAC Validation:
  Before: 50 µs (using naive string comparison)
  After:  28 µs (constant-time + optimized MAC)
  Gain:   44% faster ✅

Webhook Processing:
  Before: 200 µs (with logging at debug level)
  After:  125 µs (info level logging only)
  Gain:   37.5% faster ✅

Email Rendering:
  Before: 600 µs (no template caching)
  After:  250 µs (with caching)
  Gain:   58% faster ✅
```

---

## 🔍 Advanced JMH Options

### Run with GC Analysis
```bash
mvn test -Dtest=StripeWebhookHmacBenchmark \
  -Djmh.jvmArgs="-XX:+PrintGCDetails -XX:+PrintGCTimeStamps"
```

### Run with Custom Iterations
```bash
mvn test -Dtest=WebhookProcessingBenchmark \
  -Djmh.warmupIterations=10 \
  -Djmh.measurementIterations=20 \
  -Djmh.fork=4
```

### Run with Profiler
```bash
# Detect allocation pressure
mvn test -Dtest=EmailRenderingBenchmark \
  -Djmh.profilers=gc,jfr,stack

# Profile CPU usage
mvn test -Dtest=StripeWebhookHmacBenchmark \
  -Djmh.profilers=perfasm
```

### Save Results for Comparison
```bash
mvn test -Dtest=*Benchmark \
  -Djmh.results.file=target/results-v1.0.0.json \
  -Djmh.results.format=json
```

---

## 💾 Baseline & Regression Detection

### Store Baseline
```bash
# Run benchmarks and save results
mvn test -Dtest=*Benchmark \
  -Djmh.results.file=target/baseline-$(date +%Y%m%d).json \
  -Djmh.results.format=json
```

### Compare Against Baseline
```bash
# After code changes, check for regressions
mvn test -Dtest=StripeWebhookHmacBenchmark \
  -Djmh.results.file=target/current.json

# Use jmh-visualizer to compare
java -jar jmh-visualizer.jar target/baseline-*.json target/current.json
```

### Acceptable Regression Thresholds
- ⚠️ < 5%: Acceptable (could be measurement noise)
- 🚨 5-10%: Investigate, might be expected
- 🔴 > 10%: Must investigate and fix

---

## 🎯 Performance Testing Checklist

Before deployment, verify:

- [ ] All benchmarks run without errors
- [ ] No racepoint detection failures
- [ ] Memory usage stable (no leaks)
- [ ] Full validation < 50 µs (HMAC)
- [ ] Webhook processing < 200 µs
- [ ] Email rendering < 500 µs
- [ ] No regressions vs baseline (< 5%)
- [ ] Results documented in PR

---

## 📊 Interpreting GC Impact

### Good Signs
```
2026-03-10 12:30:45.123: [GC (G1 Evacuation Pause) ... 15ms]
```
- G1GC: Use default settings
- Pause time < 200ms: Acceptable
- Minor GC only during test

### Bad Signs
```
2026-03-10 12:30:45.123: [Full GC ... 500ms]
```
- Full GC during benchmark: Memory pressure
- Pauses > 500ms: Too aggressive
- Frequent pauses: Tune heap size

### Recommended JVM Settings
```bash
-XX:+UseG1GC \
-Xmx2G \
-XX:MaxGCPauseMillis=200 \
-XX:InitiatingHeapOccupancyPercent=35 \
-XX:+ParallelRefProcEnabled
```

---

## 📞 Troubleshooting

### Benchmark doesn't compile
```
❌ Cannot find symbol: class TemplateEngine
✅ Solution: mvn clean compile (ensures all dependencies)
```

### Results are inconsistent
```
❌ Percentages vary by > 20% between runs
✅ Solution:
  - Increase warmup iterations (-Djmh.warmupIterations=10)
  - Run on idle machine
  - Disable CPU frequency scaling
  - Close other applications
```

### Thread safety warnings
```
❌ Warning: Objects shared between threads
✅ Solution: Check @State(Scope.Thread) is used
```

### OutOfMemory during benchmark
```
❌ java.lang.OutOfMemoryError
✅ Solution: Increase heap (-Djmh.jvmArgs="-Xmx4G")
```

---

## 📎 Resources

- [JMH Documentation](https://github.com/openjdk/jmh)
- [JMH Samples](https://github.com/openjdk/jmh/tree/master/jmh-samples)
- [Microbenchmarking Guide](https://shipilev.net/blog/2014/nanotrusting-nanotime/)

---

_Last updated: March 2026_
