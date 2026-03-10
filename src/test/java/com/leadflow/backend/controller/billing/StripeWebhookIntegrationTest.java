package com.leadflow.backend.controller.billing;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Stripe webhook signature validation.
 * Tests HMAC-SHA256 signature generation and timestamp validation.
 * No Spring dependencies - pure unit tests.
 */
@Slf4j
class StripeWebhookIntegrationTest {

    private static final String WEBHOOK_SECRET = "whsec_test_secret_key_1234567890";
    private static final long TIMESTAMP_TOLERANCE = 300; // 5 minutes in seconds

    /**
     * Test: Valid signature should generate correctly
     */
    @Test
    @DisplayName("Should generate valid HMAC SHA256 signature")
    void shouldGenerateValidSignature() {
        log.info("Testing HMAC SHA256 signature generation");
        
        String timestamp = "1234567890";
        String payload = "{\"id\": \"evt_test\"}";
        
        String signature = generateSignature(timestamp, payload);
        
        assertNotNull(signature);
        assertEquals(64, signature.length());
        assertTrue(signature.matches("[a-f0-9]{64}"));
        
        log.info("✅ Signature generation test passed");
    }

    /**
     * Test: Same input should generate same signature
     */
    @Test
    @DisplayName("Should consistently generate same signature")
    void shouldGenerateConsistentSignature() {
        log.info("Testing signature consistency");
        
        String timestamp = "1234567890";
        String payload = "{\"id\": \"evt_test\"}";
        
        String signature1 = generateSignature(timestamp, payload);
        String signature2 = generateSignature(timestamp, payload);
        
        assertEquals(signature1, signature2);
        
        log.info("✅ Signature consistency test passed");
    }

    /**
     * Test: Different payload should generate different signature
     */
    @Test
    @DisplayName("Should generate different signature for different payload")
    void shouldGenerateDifferentSignatureForDifferentPayload() {
        log.info("Testing signature changes with payload");
        
        String timestamp = "1234567890";
        String payload1 = "{\"id\": \"evt_1\"}";
        String payload2 = "{\"id\": \"evt_2\"}";
        
        String signature1 = generateSignature(timestamp, payload1);
        String signature2 = generateSignature(timestamp, payload2);
        
        assertNotEquals(signature1, signature2);
        
        log.info("✅ Signature differentiation test passed");
    }

    /**
     * Test: Timestamp validation - recent timestamp should be valid
     */
    @Test
    @DisplayName("Should accept timestamp within tolerance window")
    void shouldAcceptRecentTimestamp() {
        log.info("Testing recent timestamp acceptance");
        
        long currentTime = System.currentTimeMillis() / 1000;
        long recentTime = currentTime - 240; // 4 minutes ago
        
        boolean isValid = validateTimestamp(recentTime, currentTime);
        
        assertTrue(isValid);
        
        log.info("✅ Recent timestamp acceptance test passed");
    }

    /**
     * Test: Timestamp validation - old timestamp should be rejected
     */
    @Test
    @DisplayName("Should reject expired timestamp")
    void shouldRejectExpiredTimestamp() {
        log.info("Testing expired timestamp rejection");
        
        long currentTime = System.currentTimeMillis() / 1000;
        long oldTime = currentTime - 600; // 10 minutes ago
        
        boolean isValid = validateTimestamp(oldTime, currentTime);
        
        assertFalse(isValid);
        
        log.info("✅ Expired timestamp rejection test passed");
    }

    /**
     * Test: Signature validation - correct signature should verify
     */
    @Test
    @DisplayName("Should verify correct signature")
    void shouldVerifyCorrectSignature() {
        log.info("Testing signature verification");
        
        String timestamp = "1234567890";
        String payload = "{\"id\": \"evt_test\"}";
        String signature = generateSignature(timestamp, payload);
        
        String signedContent = timestamp + "." + payload;
        boolean isValid = verifySignature(signature, signedContent);
        
        assertTrue(isValid);
        
        log.info("✅ Signature verification test passed");
    }

    /**
     * Test: Signature validation - wrong signature should fail
     */
    @Test
    @DisplayName("Should reject invalid signature")
    void shouldRejectInvalidSignature() {
        log.info("Testing invalid signature rejection");
        
        String timestamp = "1234567890";
        String payload = "{\"id\": \"evt_test\"}";
        String wrongSignature = "0000000000000000000000000000000000000000000000000000000000000000";
        
        String signedContent = timestamp + "." + payload;
        boolean isValid = verifySignature(wrongSignature, signedContent);
        
        assertFalse(isValid);
        
        log.info("✅ Invalid signature rejection test passed");
    }

    /**
     * Test: Empty signature should be rejected
     */
    @Test
    @DisplayName("Should handle empty signature gracefully")
    void shouldHandleEmptySignature() {
        log.info("Testing empty signature handling");
        
        String timestamp = "1234567890";
        String payload = "{\"id\": \"evt_test\"}";
        String emptySignature = "";
        
        String signedContent = timestamp + "." + payload;
        boolean isValid = verifySignature(emptySignature, signedContent);
        
        assertFalse(isValid);
        
        log.info("✅ Empty signature handling test passed");
    }

    private String generateSignature(String timestamp, String payload) {
        try {
            String signedContent = timestamp + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(WEBHOOK_SECRET.getBytes(), "HmacSHA256");
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(signedContent.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hmacBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Signature generation failed", e);
        }
    }

    private boolean validateTimestamp(long webhookTimestamp, long currentTime) {
        long timeDifference = Math.abs(currentTime - webhookTimestamp);
        return timeDifference <= TIMESTAMP_TOLERANCE;
    }

    private boolean verifySignature(String providedSignature, String signedContent) {
        try {
            if (providedSignature == null || providedSignature.isEmpty()) {
                return false;
            }
            
            String expectedSignature = generateSignatureForContent(signedContent);
            
            // Constant-time comparison to prevent timing attacks
            return constantTimeEqual(providedSignature, expectedSignature);
        } catch (Exception e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }

    private String generateSignatureForContent(String signedContent) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(WEBHOOK_SECRET.getBytes(), "HmacSHA256");
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(signedContent.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hmacBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Signature generation failed", e);
        }
    }

    private boolean constantTimeEqual(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
