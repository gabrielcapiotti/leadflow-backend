package com.leadflow.backend.controller;

import com.leadflow.backend.config.metrics.WebhookMetrics;
import com.leadflow.backend.dto.billing.CheckoutRequest;
import com.leadflow.backend.dto.billing.CheckoutResponse;
import com.leadflow.backend.exception.StripeSignatureVerificationException;
import com.leadflow.backend.exception.StripeTimestampExpiredException;
import com.leadflow.backend.service.billing.StripeService;
import com.leadflow.backend.service.billing.StripeWebhookValidator;
import com.leadflow.backend.service.billing.StripeWebhookAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Billing and Payment API Controller.
 * 
 * Handles Stripe checkout sessions, webhook processing with HMAC-SHA256 validation,
 * timestamp validation, and comprehensive error handling.
 * 
 * All webhook operations are tracked with metrics (timer, counters, validation rates).
 */
@Slf4j
@RestController
@RequestMapping("/billing")
@RequiredArgsConstructor
@Tag(name = "Billing", description = "Payment processing and subscription management via Stripe")
public class BillingController {

    private final StripeService stripeService;
    private final StripeWebhookValidator webhookValidator;
    private final WebhookMetrics webhookMetrics;
    private final StripeWebhookAlertService webhookAlertService;

    /**
     * Creates a Stripe checkout session for subscription payment.
     * 
     * @param request validated checkout request containing customer email and optional tenantId
     * @return checkout response with URL, reference ID, and provider
     */
    @PostMapping("/checkout")
    @Operation(
        summary = "Create Stripe checkout session",
        description = "Initiates a new Stripe checkout session for subscription payment. Returns a checkout URL and session reference ID.",
        tags = {"Billing"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Checkout session created successfully",
            content = @Content(schema = @Schema(implementation = CheckoutResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid checkout request - missing or invalid email"),
        @ApiResponse(responseCode = "500", description = "Stripe API error or server error")
    })
    public ResponseEntity<CheckoutResponse> createCheckoutSession(
            @Valid @RequestBody CheckoutRequest request
    ) {
        CheckoutResponse response = stripeService.createCheckoutSession(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Handles incoming Stripe webhook events.
     * Validates webhook signature and routes to appropriate handler.
     * 
     * Stripe-Signature header format: "t=<timestamp>,v1=<signature>"
     * 
     * @param payload raw webhook payload JSON
     * @param stripeSignature Stripe-Signature header containing timestamp and signature
     * @return "Webhook processed" with HTTP 200 on success, error message with HTTP 400 on failure
     */
    @PostMapping("/webhook")
    @Operation(
        summary = "Handle Stripe webhook events",
        description = "Receives and processes Stripe webhook events (subscription, payment, invoice). " +
                      "Validates HMAC-SHA256 signature and timestamp (5 min tolerance). " +
                      "Supports: customer.subscription.*, invoice.payment_*",
        tags = {"Billing", "Webhooks"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhook processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid signature, expired timestamp, or missing header",
            content = @Content(schema = @Schema(example = "Invalid webhook signature"))),
        @ApiResponse(responseCode = "500", description = "Unexpected error during webhook processing")
    })
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @Parameter(description = "Stripe-Signature header with format: t=<timestamp>,v1=<signature>", required = false)
            @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature
    ) {
        long startTime = System.currentTimeMillis();
        String eventType = "unknown";
        
        try {
            // Parse Stripe-Signature header format: "t=<timestamp>,v1=<signature>"
            if (stripeSignature == null || stripeSignature.isBlank()) {
                log.warn("Missing Stripe-Signature header");
                webhookMetrics.recordEventType("invalid");
                webhookMetrics.incrementFailureCounter("invalid", "missing_signature");
                webhookAlertService.recordFailure("invalid", "Missing Stripe-Signature header", null);
                return ResponseEntity.badRequest().body("Missing Stripe-Signature header");
            }

            String timestamp = null;
            String signature = null;

            for (String part : stripeSignature.split(",")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2) {
                    if ("t".equals(kv[0].trim())) {
                        timestamp = kv[1].trim();
                    } else if ("v1".equals(kv[0].trim())) {
                        signature = kv[1].trim();
                    }
                }
            }

            if (timestamp == null || signature == null) {
                log.warn("Invalid Stripe-Signature header format: {}", stripeSignature);
                webhookMetrics.recordEventType("invalid");
                webhookMetrics.incrementFailureCounter("invalid", "malformed_signature");
                webhookAlertService.recordFailure("invalid", "Invalid Stripe-Signature header format", null);
                return ResponseEntity.badRequest().body("Invalid Stripe-Signature header format");
            }

            // Validate timestamp first (prevents replay attacks)
            try {
                webhookMetrics.recordTimestampValidation(true);
                webhookValidator.validateTimestamp(timestamp);
            } catch (StripeTimestampExpiredException e) {
                webhookMetrics.recordTimestampValidation(false);
                webhookMetrics.incrementFailureCounter("all", "expired_timestamp");
                webhookAlertService.recordFailure("all", "Webhook timestamp expired: " + e.getMessage(), e);
                log.warn("Webhook timestamp expired: {}", e.getMessage());
                return ResponseEntity.badRequest().body("Webhook timestamp too old");
            }

            // Validate signature
            try {
                webhookValidator.validateSignature(payload, signature, timestamp);
                webhookMetrics.recordSignatureValidation(true);
            } catch (StripeSignatureVerificationException e) {
                webhookMetrics.recordSignatureValidation(false);
                webhookMetrics.incrementFailureCounter("all", "invalid_signature");
                webhookAlertService.recordFailure("all", "Webhook signature verification failed: " + e.getMessage(), e);
                log.warn("Webhook signature verification failed: {}", e.getMessage());
                return ResponseEntity.badRequest().body("Invalid webhook signature");
            }

            // Process webhook event and route to appropriate handler
            eventType = stripeService.processWebhookEvent(payload);
            webhookMetrics.recordEventType(eventType);
            webhookMetrics.incrementSuccessCounter(eventType);
            webhookAlertService.recordSuccess(eventType);

            long duration = System.currentTimeMillis() - startTime;
            webhookMetrics.recordProcessingDelay(duration);

            log.info("Webhook processed successfully: event_type={}, duration={}ms", eventType, duration);
            return ResponseEntity.ok("Webhook processed");

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            webhookMetrics.recordProcessingDelay(duration);
            webhookMetrics.incrementFailureCounter(eventType, "processing_error");
            webhookAlertService.recordFailure(eventType, "Unexpected error: " + e.getMessage(), e);
            
            log.error("Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to process webhook: " + e.getMessage());
        }
    }
}
