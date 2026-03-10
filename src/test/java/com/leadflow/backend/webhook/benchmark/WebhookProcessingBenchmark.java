package com.leadflow.backend.webhook.benchmark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JMH Benchmark for webhook processing performance
 * 
 * Measures:
 * - JSON parsing of webhook payload
 * - Event type routing
 * - Subscriber message creation
 * - Database record creation (simulated)
 * - End-to-end webhook processing time
 * 
 * Run with: mvn test -Dtest=WebhookProcessingBenchmark
 */
@Fork(value = 2, jvmArgs = {"-XX:+UseG1GC", "-Xmx1G"})
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class WebhookProcessingBenchmark {

    private ObjectMapper objectMapper;
    private String subscriptionCreatedPayload;
    private String paymentFailedPayload;
    private String invoicePayload;

    @Setup(Level.Trial)
    public void setupTrial() throws Exception {
        objectMapper = new ObjectMapper();
        
        // Subscription Created Event
        subscriptionCreatedPayload = """
            {
              "id": "evt_1234567890",
              "object": "event",
              "type": "customer.subscription.created",
              "created": 1234567890,
              "data": {
                "object": {
                  "id": "sub_1234567890",
                  "customer": "cus_1234567890",
                  "status": "active",
                  "current_period_started": 1234567890,
                  "current_period_end": 1234567890,
                  "items": {
                    "data": [{
                      "id": "si_1234567890",
                      "price": {
                        "id": "price_1234567890",
                        "unit_amount": 2999,
                        "currency": "usd",
                        "recurring": {
                          "interval": "month",
                          "interval_count": 1
                        }
                      }
                    }]
                  }
                }
              }
            }
            """;
        
        // Payment Failed Event
        paymentFailedPayload = """
            {
              "id": "evt_9876543210",
              "object": "event",
              "type": "invoice.payment_failed",
              "created": 1234567890,
              "data": {
                "object": {
                  "id": "in_1234567890",
                  "customer": "cus_1234567890",
                  "subscription": "sub_1234567890",
                  "amount_due": 2999,
                  "currency": "usd",
                  "attempt_count": 1,
                  "next_payment_attempt": 1234567890
                }
              }
            }
            """;
        
        // Invoice Event
        invoicePayload = """
            {
              "id": "evt_5555555555",
              "object": "event",
              "type": "invoice.created",
              "created": 1234567890,
              "data": {
                "object": {
                  "id": "in_5555555555",
                  "customer": "cus_1234567890",
                  "subscription": "sub_1234567890",
                  "amount_due": 2999,
                  "currency": "usd",
                  "status": "open"
                }
              }
            }
            """;
    }

    /**
     * Benchmark: JSON parsing of webhook payload
     */
    @Benchmark
    public JsonNode benchmarkJsonParsing(Blackhole bh) throws Exception {
        return objectMapper.readTree(subscriptionCreatedPayload);
    }

    /**
     * Benchmark: Event type routing (pattern matching)
     */
    @Benchmark
    public String benchmarkEventRouting(Blackhole bh) throws Exception {
        JsonNode event = objectMapper.readTree(subscriptionCreatedPayload);
        String eventType = event.get("type").asText();
        
        return switch (eventType) {
            case "customer.subscription.created" -> "handleSubscriptionCreated";
            case "invoice.payment_failed" -> "handlePaymentFailed";
            case "invoice.created" -> "handleInvoiceCreated";
            case "charge.refunded" -> "handleChargeRefunded";
            default -> "unknown";
        };
    }

    /**
     * Benchmark: Extracting data from parsed event
     */
    @Benchmark
    public Map<String, String> benchmarkDataExtraction(Blackhole bh) throws Exception {
        JsonNode event = objectMapper.readTree(subscriptionCreatedPayload);
        JsonNode object = event.get("data").get("object");
        
        Map<String, String> data = new HashMap<>();
        data.put("id", object.get("id").asText());
        data.put("customer", object.get("customer").asText());
        data.put("status", object.get("status").asText());
        data.put("periodEnd", object.get("current_period_end").asText());
        
        return data;
    }

    /**
     * Benchmark: Processing subscription created event
     */
    @Benchmark
    public boolean benchmarkProcessSubscriptionCreated(Blackhole bh) throws Exception {
        JsonNode event = objectMapper.readTree(subscriptionCreatedPayload);
        JsonNode object = event.get("data").get("object");
        
        String subscriptionId = object.get("id").asText();
        String customerId = object.get("customer").asText();
        String status = object.get("status").asText();
        
        // Simulate database INSERT
        return !subscriptionId.isEmpty() && !customerId.isEmpty() && status.equals("active");
    }

    /**
     * Benchmark: Processing payment failed event
     */
    @Benchmark
    public boolean benchmarkProcessPaymentFailed(Blackhole bh) throws Exception {
        JsonNode event = objectMapper.readTree(paymentFailedPayload);
        JsonNode object = event.get("data").get("object");
        
        String invoiceId = object.get("id").asText();
        int attemptCount = object.get("attempt_count").asInt();
        
        // Simulate email preparation
        return attemptCount > 0 && !invoiceId.isEmpty();
    }

    /**
     * Benchmark: Full webhook processing pipeline
     * (parse + route + extract + process + persist)
     */
    @Benchmark
    public Map<String, Object> benchmarkFullWebhookProcessing(Blackhole bh) throws Exception {
        // 1. Parse JSON
        JsonNode event = objectMapper.readTree(subscriptionCreatedPayload);
        
        // 2. Route by type
        String eventType = event.get("type").asText();
        
        // 3. Extract data
        JsonNode object = event.get("data").get("object");
        Map<String, Object> result = new HashMap<>();
        result.put("eventId", event.get("id").asText());
        result.put("type", eventType);
        result.put("subscriptionId", object.get("id").asText());
        result.put("customerId", object.get("customer").asText());
        result.put("status", object.get("status").asText());
        
        // 4. Simulate DB write
        result.put("persisted", true);
        
        return result;
    }

    /**
     * Benchmark: Comparing different event types
     */
    @Benchmark
    public int benchmarkMultiEventProcessing(Blackhole bh) throws Exception {
        int processed = 0;
        
        for (String payload : new String[]{
            subscriptionCreatedPayload,
            paymentFailedPayload,
            invoicePayload
        }) {
            JsonNode event = objectMapper.readTree(payload);
            String type = event.get("type").asText();
            if (!type.isEmpty()) {
                processed++;
            }
        }
        
        return processed;
    }

    /**
     * Benchmark: Database insert simulation
     * (HashMap operations represent DB calls)
     */
    @Benchmark
    public Map<String, String> benchmarkDatabaseInsert(Blackhole bh) throws Exception {
        Map<String, String> record = new HashMap<>();
        record.put("event_id", "evt_1234567890");
        record.put("event_type", "customer.subscription.created");
        record.put("subscription_id", "sub_1234567890");
        record.put("customer_id", "cus_1234567890");
        record.put("processed_at", System.currentTimeMillis() + "");
        record.put("status", "succeeded");
        
        return record;
    }

    // ==================== Main Method ====================

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(WebhookProcessingBenchmark.class.getSimpleName())
            .forks(1)
            .warmupIterations(3)
            .measurementIterations(5)
            .build();

        new Runner(opt).run();
    }
}
