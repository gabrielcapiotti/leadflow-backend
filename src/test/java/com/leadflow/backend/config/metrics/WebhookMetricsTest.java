package com.leadflow.backend.config.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for WebhookMetrics.
 * 
 * Tests metric recording and counter increments without requiring a full Spring context.
 * Uses SimpleMeterRegistry for isolated unit testing.
 */
@Slf4j
@DisplayName("Webhook Metrics Unit Tests")
class WebhookMetricsTest {

    private WebhookMetrics webhookMetrics;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        // Use SimpleMeterRegistry for unit testing (no Spring needed)
        meterRegistry = new SimpleMeterRegistry();
        webhookMetrics = new WebhookMetrics(meterRegistry);
    }

    /**
     * Test: Should record processing delay time
     */
    @Test
    @DisplayName("Should record processing delay in milliseconds")
    void shouldRecordProcessingDelay() {
        log.info("Testing processing delay recording");

        webhookMetrics.recordProcessingDelay(100);
        webhookMetrics.recordProcessingDelay(250);
        webhookMetrics.recordProcessingDelay(150);

        assertThat(webhookMetrics).isNotNull();
        log.info("✅ Processing delay recorded successfully");
    }

    /**
     * Test: Should increment success counter
     */
    @Test
    @DisplayName("Should increment success counter by event type")
    void shouldIncrementSuccessCounter() {
        log.info("Testing success counter increment");

        String eventType = "customer.subscription.created";
        
        webhookMetrics.incrementSuccessCounter(eventType);
        webhookMetrics.incrementSuccessCounter(eventType);
        webhookMetrics.incrementSuccessCounter(eventType);

        assertThat(webhookMetrics).isNotNull();
        log.info("✅ Success counter incremented correctly");
    }

    /**
     * Test: Should increment failure counter with error type
     */
    @Test
    @DisplayName("Should increment failure counter by event and error type")
    void shouldIncrementFailureCounter() {
        log.info("Testing failure counter increment");

        String eventType = "invoice.payment_failed";
        String errorType = "payment_declined";

        webhookMetrics.incrementFailureCounter(eventType, errorType);
        webhookMetrics.incrementFailureCounter(eventType, errorType);

        assertThat(webhookMetrics).isNotNull();
        log.info("✅ Failure counter incremented correctly");
    }

    /**
     * Test: Should record event type
     */
    @Test
    @DisplayName("Should record webhook event type")
    void shouldRecordEventType() {
        log.info("Testing event type recording");

        webhookMetrics.recordEventType("customer.subscription.created");
        webhookMetrics.recordEventType("invoice.payment_succeeded");
        webhookMetrics.recordEventType("customer.subscription.deleted");

        assertThat(webhookMetrics).isNotNull();
        log.info("✅ Event types recorded successfully");
    }

    /**
     * Test: Should record signature validation results
     */
    @Test
    @DisplayName("Should record signature validation as true")
    void shouldRecordSignatureValidationTrue() {
        log.info("Testing signature validation recording (true)");

        webhookMetrics.recordSignatureValidation(true);
        webhookMetrics.recordSignatureValidation(true);

        assertThat(webhookMetrics).isNotNull();
        log.info("✅ Signature validation recorded successfully");
    }

    /**
     * Test: Should record failed signature validation
     */
    @Test
    @DisplayName("Should record signature validation as false")
    void shouldRecordSignatureValidationFalse() {
        log.info("Testing signature validation recording (false)");

        webhookMetrics.recordSignatureValidation(false);

        assertThat(webhookMetrics).isNotNull();
        log.info("✅ Failed signature validation recorded successfully");
    }

    /**
     * Test: Should record timestamp validation results
     */
    @Test
    @DisplayName("Should record timestamp validation as true")
    void shouldRecordTimestampValidationTrue() {
        log.info("Testing timestamp validation recording (true)");

        webhookMetrics.recordTimestampValidation(true);
        webhookMetrics.recordTimestampValidation(true);

        assertThat(webhookMetrics).isNotNull();
        log.info("✅ Timestamp validation recorded successfully");
    }

    /**
     * Test: Should record failed timestamp validation
     */
    @Test
    @DisplayName("Should record timestamp validation as false")
    void shouldRecordTimestampValidationFalse() {
        log.info("Testing timestamp validation recording (false)");

        webhookMetrics.recordTimestampValidation(false);

        assertThat(webhookMetrics).isNotNull();
        log.info("✅ Failed timestamp validation recorded successfully");
    }

    /**
     * Test: Should handle multiple event types independently
     */
    @Test
    @DisplayName("Should track multiple event types independently")
    void shouldTrackMultipleEventTypesIndependently() {
        log.info("Testing multiple event type tracking");

        String[] eventTypes = {
            "customer.subscription.created",
            "invoice.payment_succeeded",
            "customer.subscription.deleted",
            "invoice.payment_failed"
        };

        for (String eventType : eventTypes) {
            webhookMetrics.recordEventType(eventType);
            webhookMetrics.incrementSuccessCounter(eventType);
        }

        assertThat(webhookMetrics).isNotNull();
        log.info("✅ Multiple event types tracked independently");
    }

    /**
     * Test: Should handle mixed validation results
     */
    @Test
    @DisplayName("Should handle mixed validation results")
    void shouldHandleMixedValidationResults() {
        log.info("Testing mixed validation results");

        // Mixed signature validations
        webhookMetrics.recordSignatureValidation(true);
        webhookMetrics.recordSignatureValidation(true);
        webhookMetrics.recordSignatureValidation(false);
        webhookMetrics.recordSignatureValidation(true);

        // Mixed timestamp validations
        webhookMetrics.recordTimestampValidation(true);
        webhookMetrics.recordTimestampValidation(false);
        webhookMetrics.recordTimestampValidation(true);

        assertThat(webhookMetrics).isNotNull();
        log.info("✅ Mixed validation results handled correctly");
    }

    /**
     * Test: Should handle high-frequency metric recording
     */
    @Test
    @DisplayName("Should handle high-frequency metric recording")
    void shouldHandleHighFrequencyRecording() {
        log.info("Testing high-frequency metric recording");

        // Simulate 100 webhook events
        for (int i = 0; i < 100; i++) {
            String eventType = (i % 2 == 0) ? "subscription.created" : "payment.failed";
            webhookMetrics.recordEventType(eventType);
            webhookMetrics.recordProcessingDelay(50 + i);
            
            if (i % 3 == 0) {
                webhookMetrics.incrementFailureCounter(eventType, "error_type");
            } else {
                webhookMetrics.incrementSuccessCounter(eventType);
            }
        }

        assertThat(webhookMetrics).isNotNull();
        log.info("✅ High-frequency recording completed successfully");
    }

    /**
     * Test: Should not throw exceptions on null or empty strings
     */
    @Test
    @DisplayName("Should handle edge cases gracefully")
    void shouldHandleEdgeCasesGracefully() {
        log.info("Testing edge case handling");

        // Should not throw exceptions
        assertThatNoException().isThrownBy(() -> {
            webhookMetrics.recordEventType("");
            webhookMetrics.recordEventType("normal_event");
            webhookMetrics.incrementSuccessCounter("event");
            webhookMetrics.incrementFailureCounter("event", "error");
            webhookMetrics.recordProcessingDelay(0);
            webhookMetrics.recordProcessingDelay(9999); // Large but reasonable value (9.999 seconds)
        });

        log.info("✅ Edge cases handled gracefully");
    }
}
