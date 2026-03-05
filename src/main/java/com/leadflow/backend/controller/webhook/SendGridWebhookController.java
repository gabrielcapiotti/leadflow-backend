package com.leadflow.backend.controller.webhook;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.entities.vendor.EmailEvent;
import com.leadflow.backend.repository.EmailEventRepository;
import com.leadflow.backend.repository.VendorRepository;
import com.leadflow.backend.service.notification.SendGridWebhookVerifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/webhooks/sendgrid")
public class SendGridWebhookController {

    private static final String SIGNATURE_HEADER = "X-Twilio-Email-Event-Webhook-Signature";
    private static final String TIMESTAMP_HEADER = "X-Twilio-Email-Event-Webhook-Timestamp";

    private final EmailEventRepository repository;
    private final VendorRepository vendorRepository;
    private final SendGridWebhookVerifier verifier;
    private final ObjectMapper objectMapper;

    public SendGridWebhookController(EmailEventRepository repository,
                                     VendorRepository vendorRepository,
                                     SendGridWebhookVerifier verifier,
                                     ObjectMapper objectMapper) {
        this.repository = repository;
        this.vendorRepository = vendorRepository;
        this.verifier = verifier;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<Void> handle(
            @RequestBody String payload,
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature,
            @RequestHeader(value = TIMESTAMP_HEADER, required = false) String timestamp
    ) {

        if (verifier.isVerificationEnabled()
            && !verifier.verify(signature, timestamp, payload)) {
            return ResponseEntity.status(401).build();
        }

        List<Map<String, Object>> events;
        try {
            events = objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }

        for (Map<String, Object> event : events) {
            String email = stringValue(event.get("email"));
            String eventType = stringValue(event.get("event"));

            if (email == null || eventType == null) {
                continue;
            }

            EmailEvent emailEvent = new EmailEvent();
            emailEvent.setEmail(email);
            emailEvent.setEventType(eventType);
            emailEvent.setOccurredAt(resolveOccurredAt(event));
            emailEvent.setReason(stringValue(event.get("reason")));

            repository.save(emailEvent);

            if (shouldInvalidateEmail(eventType, event)) {
                markEmailAsInvalid(email);
            }
        }

        return ResponseEntity.ok().build();
    }

    private void markEmailAsInvalid(String email) {
        vendorRepository.findByUserEmail(email)
                .forEach(vendor -> {
                    vendor.setEmailInvalid(true);
                    vendorRepository.save(vendor);
                });
    }

    private boolean shouldInvalidateEmail(String eventType, Map<String, Object> event) {
        if ("spamreport".equalsIgnoreCase(eventType)) {
            return true;
        }

        if (!"bounce".equalsIgnoreCase(eventType)) {
            return false;
        }

        String bounceType = stringValue(event.get("type"));
        String reason = stringValue(event.get("reason"));

        if (bounceType != null && "blocked".equalsIgnoreCase(bounceType)) {
            return false;
        }

        if (reason == null) {
            return true;
        }

        String normalizedReason = reason.toLowerCase();
        return !normalizedReason.contains("mailbox full")
                && !normalizedReason.contains("temporarily")
                && !normalizedReason.contains("try again later");
    }

    private Instant resolveOccurredAt(Map<String, Object> event) {
        Object timestamp = event.get("timestamp");

        if (timestamp instanceof Number number) {
            return Instant.ofEpochSecond(number.longValue());
        }

        return Instant.now();
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }

        String result = String.valueOf(value).trim();
        return result.isEmpty() ? null : result;
    }
}
