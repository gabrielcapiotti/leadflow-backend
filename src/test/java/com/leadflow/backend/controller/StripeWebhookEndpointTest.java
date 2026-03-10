package com.leadflow.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.config.TestBillingConfig;
import com.leadflow.backend.config.metrics.WebhookMetrics;
import com.leadflow.backend.service.billing.StripeService;
import com.leadflow.backend.service.billing.StripeWebhookValidator;
import com.leadflow.backend.service.billing.StripeWebhookAlertService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Stripe webhook endpoint (/billing/webhook).
 * 
 * Tests webhook signature validation, timestamp validation, event processing,
 * and error handling with simulated Stripe webhook payloads.
 */
@Slf4j
@WebMvcTest(controllers = BillingController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import({TestBillingConfig.class})
@DisplayName("Stripe Webhook Endpoint Tests")
class StripeWebhookEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StripeService stripeService;

    @MockBean
    private StripeWebhookValidator webhookValidator;

    @MockBean
    private WebhookMetrics webhookMetrics;

    @MockBean
    private StripeWebhookAlertService webhookAlertService;

    private static final String WEBHOOK_ENDPOINT = "/billing/webhook";
    private static final String WEBHOOK_SECRET = "whsec_test_secret";
    private static final long TIMESTAMP_TOLERANCE = 300; // 5 minutes

    private String validTimestamp;
    private String validSignature;

    @BeforeEach
    void setUp() {
        // Generate fresh timestamp and signature for each test
        validTimestamp = String.valueOf(System.currentTimeMillis() / 1000);
    }

    /**
     * Helper: Generate Stripe HMAC-SHA256 signature
     */
    private String generateSignature(String timestamp, String payload) {
        try {
            String signedContent = timestamp + "." + payload;
            SecretKeySpec keySpec = new SecretKeySpec(
                WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);
            byte[] signature = mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper: Create Stripe-Signature header value
     */
    private String createSignatureHeader(String timestamp, String signature) {
        return String.format("t=%s,v1=%s", timestamp, signature);
    }

    /**
     * Test: Valid subscription.created event should be processed successfully
     */
    @Test
    @DisplayName("Should process valid subscription.created webhook")
    void shouldProcessValidSubscriptionCreatedWebhook() throws Exception {
        log.info("Testing valid subscription.created webhook");

        String payload = """
            {
              "id": "evt_1234567890",
              "type": "customer.subscription.created",
              "created": %d,
              "data": {
                "object": {
                  "id": "sub_1234567890",
                  "customer": "cus_1234567890",
                  "status": "active"
                }
              }
            }
            """.formatted(System.currentTimeMillis() / 1000);

        validSignature = generateSignature(validTimestamp, payload);
        String signatureHeader = createSignatureHeader(validTimestamp, validSignature);

        // Mock the validator to accept the request
        doNothing().when(webhookValidator).validateTimestamp(validTimestamp);
        doNothing().when(webhookValidator).validateSignature(payload, validSignature, validTimestamp);
        when(stripeService.processWebhookEvent(payload)).thenReturn("customer.subscription.created");

        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", signatureHeader)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed"));

        // Verify metrics were recorded
        verify(webhookValidator).validateTimestamp(validTimestamp);
        verify(webhookValidator).validateSignature(payload, validSignature, validTimestamp);
        verify(stripeService).processWebhookEvent(payload);
        verify(webhookMetrics).recordTimestampValidation(true);
        verify(webhookMetrics).recordSignatureValidation(true);
        verify(webhookMetrics).recordEventType("customer.subscription.created");
        verify(webhookMetrics).incrementSuccessCounter("customer.subscription.created");

        log.info("✅ Valid subscription.created webhook test passed");
    }

    /**
     * Test: Valid invoice.payment_failed event should be processed
     */
    @Test
    @DisplayName("Should process valid invoice.payment_failed webhook")
    void shouldProcessValidPaymentFailedWebhook() throws Exception {
        log.info("Testing valid payment_failed webhook");

        String payload = """
            {
              "id": "evt_payment_failed",
              "type": "invoice.payment_failed",
              "created": %d,
              "data": {
                "object": {
                  "id": "in_1234567890",
                  "subscription": "sub_1234567890",
                  "status": "open"
                }
              }
            }
            """.formatted(System.currentTimeMillis() / 1000);

        validSignature = generateSignature(validTimestamp, payload);
        String signatureHeader = createSignatureHeader(validTimestamp, validSignature);

        doNothing().when(webhookValidator).validateTimestamp(validTimestamp);
        doNothing().when(webhookValidator).validateSignature(payload, validSignature, validTimestamp);
        when(stripeService.processWebhookEvent(payload)).thenReturn("invoice.payment_failed");

        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", signatureHeader)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed"));

        verify(webhookMetrics).recordEventType("invoice.payment_failed");
        verify(webhookMetrics).incrementSuccessCounter("invoice.payment_failed");

        log.info("✅ Valid payment_failed webhook test passed");
    }

    /**
     * Test: Missing Stripe-Signature header should return 400
     */
    @Test
    @DisplayName("Should reject webhook with missing Stripe-Signature header")
    void shouldRejectWebhookWithoutSignatureHeader() throws Exception {
        log.info("Testing webhook without Stripe-Signature header");

        String payload = """
            {
              "id": "evt_test",
              "type": "customer.subscription.created"
            }
            """;

        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Missing Stripe-Signature header"));

        // Verify metrics record the failure
        verify(webhookMetrics).recordEventType("invalid");
        verify(webhookMetrics).incrementFailureCounter("invalid", "missing_signature");

        log.info("✅ Missing signature header test passed");
    }

    /**
     * Test: Malformed Stripe-Signature header should return 400
     */
    @Test
    @DisplayName("Should reject webhook with malformed Stripe-Signature")
    void shouldRejectWebhookWithMalformedSignature() throws Exception {
        log.info("Testing webhook with malformed Stripe-Signature");

        String payload = """
            {
              "id": "evt_test",
              "type": "customer.subscription.created"
            }
            """;

        String invalidSignatureHeader = "invalid_format_without_equals";

        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", invalidSignatureHeader)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid Stripe-Signature header format"));

        verify(webhookMetrics).recordEventType("invalid");
        verify(webhookMetrics).incrementFailureCounter("invalid", "malformed_signature");

        log.info("✅ Malformed signature header test passed");
    }

    /**
     * Test: Invalid signature should return 400
     */
    @Test
    @DisplayName("Should reject webhook with invalid signature")
    void shouldRejectWebhookWithInvalidSignature() throws Exception {
        log.info("Testing webhook with invalid signature");

        String payload = """
            {
              "id": "evt_test",
              "type": "customer.subscription.created"
            }
            """;

        String invalidSignature = "invalid_signature_not_matching";
        String signatureHeader = createSignatureHeader(validTimestamp, invalidSignature);

        doNothing().when(webhookValidator).validateTimestamp(validTimestamp);
        doThrow(new com.leadflow.backend.exception.StripeSignatureVerificationException("Invalid signature"))
            .when(webhookValidator).validateSignature(payload, invalidSignature, validTimestamp);

        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", signatureHeader)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid webhook signature"));

        verify(webhookMetrics).recordTimestampValidation(true);
        verify(webhookMetrics).recordSignatureValidation(false);
        verify(webhookMetrics).incrementFailureCounter("all", "invalid_signature");

        log.info("✅ Invalid signature test passed");
    }

    /**
     * Test: Expired timestamp should return 400
     */
    @Test
    @DisplayName("Should reject webhook with expired timestamp")
    void shouldRejectWebhookWithExpiredTimestamp() throws Exception {
        log.info("Testing webhook with expired timestamp");

        String payload = """
            {
              "id": "evt_test",
              "type": "customer.subscription.created"
            }
            """;

        String expiredTimestamp = String.valueOf((System.currentTimeMillis() / 1000) - 600); // 10 minutes ago
        String signature = generateSignature(expiredTimestamp, payload);
        String signatureHeader = createSignatureHeader(expiredTimestamp, signature);

        doThrow(new com.leadflow.backend.exception.StripeTimestampExpiredException("Timestamp too old"))
            .when(webhookValidator).validateTimestamp(expiredTimestamp);

        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", signatureHeader)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Webhook timestamp too old"));

        verify(webhookMetrics).recordTimestampValidation(false);
        verify(webhookMetrics).incrementFailureCounter("all", "expired_timestamp");

        log.info("✅ Expired timestamp test passed");
    }

    /**
     * Test: Processing error should return 400 with error message
     */
    @Test
    @DisplayName("Should handle processing errors gracefully")
    void shouldHandleProcessingErrors() throws Exception {
        log.info("Testing webhook processing error handling");

        String payload = """
            {
              "id": "evt_test",
              "type": "customer.subscription.created"
            }
            """;

        validSignature = generateSignature(validTimestamp, payload);
        String signatureHeader = createSignatureHeader(validTimestamp, validSignature);

        doNothing().when(webhookValidator).validateTimestamp(validTimestamp);
        doNothing().when(webhookValidator).validateSignature(payload, validSignature, validTimestamp);
        when(stripeService.processWebhookEvent(payload))
            .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", signatureHeader)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Failed to process webhook")));

        verify(webhookMetrics).recordTimestampValidation(true);
        verify(webhookMetrics).recordSignatureValidation(true);

        log.info("✅ Processing error handling test passed");
    }

    /**
     * Test: Multiple valid webhooks should each be processed independently
     */
    @Test
    @DisplayName("Should process multiple webhooks independently")
    void shouldProcessMultipleWebhooksIndependently() throws Exception {
        log.info("Testing multiple webhook requests");

        String[] eventTypes = {
            "customer.subscription.created",
            "invoice.payment_succeeded",
            "customer.subscription.deleted"
        };

        for (String eventType : eventTypes) {
            String payload = """
                {
                  "id": "evt_%s",
                  "type": "%s",
                  "created": %d
                }
                """.formatted(eventType, eventType, System.currentTimeMillis() / 1000);

            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String signature = generateSignature(timestamp, payload);
            String signatureHeader = createSignatureHeader(timestamp, signature);

            doNothing().when(webhookValidator).validateTimestamp(timestamp);
            doNothing().when(webhookValidator).validateSignature(payload, signature, timestamp);
            when(stripeService.processWebhookEvent(payload)).thenReturn(eventType);

            mockMvc.perform(post(WEBHOOK_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Stripe-Signature", signatureHeader)
                    .content(payload))
                    .andExpect(status().isOk());

            verify(webhookMetrics).recordEventType(eventType);
        }

        log.info("✅ Multiple webhooks test passed");
    }
}
