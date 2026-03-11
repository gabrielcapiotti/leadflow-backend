package com.leadflow.backend.controller;

import com.leadflow.backend.entities.StripeEventLog;
import com.leadflow.backend.exception.StripeSignatureVerificationException;
import com.leadflow.backend.exception.StripeTimestampExpiredException;
import com.leadflow.backend.repository.StripeEventLogRepository;
import com.leadflow.backend.service.billing.StripeService;
import com.leadflow.backend.service.billing.StripeWebhookProcessor;
import com.leadflow.backend.service.billing.StripeWebhookValidator;
import com.leadflow.backend.service.billing.StripeWebhookProcessingService;
import com.stripe.model.Event;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Stripe Webhook Controller
 * 
 * Handles incoming webhooks from Stripe with security validation:
 * 1. HMAC-SHA256 signature verification
 * 2. Timestamp validation (prevent replay attacks)
 * 3. Event processing with handlers
 * 4. Event persistence for auditoria and replay
 */
@RestController
@RequestMapping("/stripe")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final StripeService stripeService;
    private final StripeWebhookProcessingService webhookProcessingService;
    
    // New secure webhook handling
    private final StripeWebhookValidator webhookValidator;
    private final StripeWebhookProcessor webhookProcessor;
    private final StripeEventLogRepository eventLogRepository;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(HttpServletRequest request) throws IOException {

        String payload = request.getReader()
                .lines()
                .collect(Collectors.joining());

        String signatureHeader = request.getHeader("Stripe-Signature");

        try {
            log.info("Received Stripe webhook");
            
            // Parse Stripe-Signature header: format is "t={timestamp},v1={signature}"
            String timestamp = parseTimestamp(signatureHeader);
            String signatureHash = parseSignatureHash(signatureHeader, "v1");
            
            // Step 1: Validate HMAC-SHA256 signature
            webhookValidator.validateSignature(payload, signatureHash, timestamp);
            log.debug("✅ Webhook signature validated");
            
            // Step 2: Validate timestamp (prevent replay attacks)
            webhookValidator.validateTimestamp(timestamp);
            log.debug("✅ Webhook timestamp validated");
            
            // Step 3: Construct and deserialize event (using Stripe SDK)
            Event event = stripeService.constructWebhookEvent(payload, signatureHeader);
            log.info("✅ Stripe event deserialized: type={}, id={}", event.getType(), event.getId());
            
            // Step 4: Check idempotency - is this event already processed?
            if (isEventAlreadyProcessed(event.getId())) {
                log.warn("⚠️  Duplicate webhook event received (idempotency): {}", event.getId());
                recordWebhookEvent(event, true, "DUPLICATE_EVENT");
                return ResponseEntity.ok("received"); // Return 200 even for duplicates
            }
            
            // Step 5: Process the event through handler registry
            try {
                webhookProcessor.process(event);
                log.info("✅ Webhook event processed successfully: id={}", event.getId());
                recordWebhookEvent(event, true, null);
            } catch (Exception e) {
                log.warn("⚠️  Event processing completed with exception (will retry), id={}", event.getId(), e);
                recordWebhookEvent(event, false, e.getMessage());
                // Return 200 anyway so Stripe doesn't retry immediately
                return ResponseEntity.ok("received");
            }
            
            // Step 6: Also call legacy processing service for backward compatibility
            webhookProcessingService.processAndLogEvent(event);
            
            return ResponseEntity.ok("received");
            
        } catch (StripeSignatureVerificationException e) {
            log.error("❌ Webhook signature verification failed: {}", e.getMessage(), e);
            recordWebhookEvent(null, false, "SIGNATURE_VERIFICATION_FAILED: " + e.getMessage());
            // Return 401 for invalid signature
            return ResponseEntity.status(401).body("Invalid signature");
            
        } catch (StripeTimestampExpiredException e) {
            log.error("❌ Webhook timestamp validation failed: {}", e.getMessage(), e);
            recordWebhookEvent(null, false, "TIMESTAMP_VERIFICATION_FAILED: " + e.getMessage());
            // Return 401 for expired timestamp
            return ResponseEntity.status(401).body("Timestamp expired");
            
        } catch (Exception e) {
            log.error("❌ Failed to process webhook", e);
            recordWebhookEvent(null, false, "PROCESSING_EXCEPTION: " + e.getMessage());
            // Return 200 anyway so Stripe doesn't keep retrying
            return ResponseEntity.ok("received");
        }
    }

    /**
     * Parse timestamp from Stripe-Signature header.
     * Format: "t=1234567890,v1=..."
     */
    private String parseTimestamp(String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new StripeSignatureVerificationException("Missing Stripe-Signature header");
        }
        
        for (String pair : signatureHeader.split(",")) {
            if (pair.startsWith("t=")) {
                return pair.substring(2);
            }
        }
        throw new StripeSignatureVerificationException("Missing timestamp (t=) in Stripe-Signature header");
    }

    /**
     * Parse signature hash from Stripe-Signature header.
     * Format: "t=1234567890,v1=..."
     */
    private String parseSignatureHash(String signatureHeader, String version) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new StripeSignatureVerificationException("Missing Stripe-Signature header");
        }
        
        for (String pair : signatureHeader.split(",")) {
            if (pair.startsWith(version + "=")) {
                return pair.substring(version.length() + 1);
            }
        }
        throw new StripeSignatureVerificationException("Missing signature hash (" + version + "=) in Stripe-Signature header");
    }

    /**
     * Check if this event was already processed (idempotency).
     * Prevents duplicate processing if Stripe retries the webhook.
     */
    private boolean isEventAlreadyProcessed(String eventId) {
        return eventLogRepository.findByEventId(eventId)
                .map(log -> log.getStatus() == StripeEventLog.EventProcessingStatus.SUCCESS)
                .orElse(false);
    }

    /**
     * Record webhook event in database for auditoria and replay capability.
     */
    private void recordWebhookEvent(Event event, boolean success, String errorMessage) {
        try {
            StripeEventLog eventLog = StripeEventLog.builder()
                    .eventId(event != null ? event.getId() : "unknown")
                    .eventType(event != null ? event.getType() : "unknown")
                    .payload(event != null ? event.toString() : "{}")
                    .status(success ? StripeEventLog.EventProcessingStatus.SUCCESS : StripeEventLog.EventProcessingStatus.FAILED)
                    .retryCount(0)
                    .maxRetries(3)
                    .lastError(errorMessage)
                    .processedAt(LocalDateTime.now())
                    .build();
            
            eventLogRepository.save(Objects.requireNonNull(eventLog));
            log.debug("Recorded webhook event: id={}, status={}", eventLog.getEventId(), eventLog.getStatus());
        } catch (Exception e) {
            log.error("Failed to record webhook event in database", e);
            // Don't fail the webhook handling if we can't save the log
        }
    }
}
