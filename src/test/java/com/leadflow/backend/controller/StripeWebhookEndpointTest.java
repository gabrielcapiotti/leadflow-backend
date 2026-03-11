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

    private String validTimestamp;
    private String validSignature;

    @BeforeEach
    void setUp() {
        validTimestamp = String.valueOf(System.currentTimeMillis() / 1000);
    }

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

    private String createSignatureHeader(String timestamp, String signature) {
        return String.format("t=%s,v1=%s", timestamp, signature);
    }

    @Test
    @DisplayName("Should process valid subscription.created webhook")
    void shouldProcessValidSubscriptionCreatedWebhook() throws Exception {

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

        doNothing().when(webhookValidator).validateTimestamp(validTimestamp);
        doNothing().when(webhookValidator).validateSignature(payload, validSignature, validTimestamp);
        when(stripeService.processWebhookEvent(payload)).thenReturn("customer.subscription.created");

        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", signatureHeader)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed"));

        verify(webhookValidator).validateTimestamp(validTimestamp);
        verify(webhookValidator).validateSignature(payload, validSignature, validTimestamp);
        verify(stripeService).processWebhookEvent(payload);

        verify(webhookMetrics).recordTimestampValidation(true);
        verify(webhookMetrics).recordSignatureValidation(true);
        verify(webhookMetrics).recordEventType("customer.subscription.created");
        verify(webhookMetrics).incrementSuccessCounter("customer.subscription.created");
    }

    @Test
    @DisplayName("Should handle processing errors gracefully")
    void shouldHandleProcessingErrors() throws Exception {

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
    }

    @Test
    @DisplayName("Should process multiple webhooks independently")
    void shouldProcessMultipleWebhooksIndependently() throws Exception {

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
    }
}