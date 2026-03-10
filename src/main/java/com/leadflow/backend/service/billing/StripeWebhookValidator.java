package com.leadflow.backend.service.billing;

import com.leadflow.backend.config.StripeProperties;
import com.leadflow.backend.exception.StripeSignatureVerificationException;
import com.leadflow.backend.exception.StripeTimestampExpiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Validates Stripe webhook requests with:
 * - HMAC-SHA256 signature verification
 * - Timestamp validation (prevent replay attacks)
 * 
 * Reference: https://stripe.com/docs/webhooks/securing
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookValidator {
    
    private final StripeProperties stripeProperties;
    
    /**
     * Validates the HMAC-SHA256 signature of a webhook request.
     * 
     * @param payload The webhook payload (raw JSON string)
     * @param signatureHash The signature hash from "Stripe-Signature" header
     * @param timestamp The timestamp from "Stripe-Signature" header
     * @throws StripeSignatureVerificationException If signature is invalid
     */
    public void validateSignature(String payload, String signatureHash, String timestamp) 
            throws StripeSignatureVerificationException {
        
        try {
            // Construct signed content: "{timestamp}.{payload}"
            String signedContent = timestamp + "." + payload;
            
            // Compute HMAC-SHA256 hash
            String computedSignature = computeHmacSha256(signedContent, stripeProperties.getWebhook().getSecret());
            
            log.debug("Signature Validation: Expected={}, Received={}", 
                computedSignature.substring(0, 16) + "...", 
                signatureHash.substring(0, Math.min(16, signatureHash.length())) + "...");
            
            // Compare in constant time to prevent timing attacks
            if (!constantTimeEquals(computedSignature, signatureHash)) {
                log.warn("❌ Invalid webhook signature detected. Payload may be tampered.");
                throw new StripeSignatureVerificationException("Webhook signature verification failed");
            }
            
            log.debug("✅ Webhook signature verification successful");
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to compute HMAC-SHA256", e);
            throw new StripeSignatureVerificationException("Signature computation failed", e);
        }
    }
    
    /**
     * Validates the webhook timestamp to prevent replay attacks.
     * Rejects webhooks older than the configured tolerance (default: 5 minutes).
     * 
     * @param timestamp The timestamp from "Stripe-Signature" header (Unix timestamp in seconds)
     * @throws StripeTimestampExpiredException If timestamp is too old
     */
    public void validateTimestamp(String timestamp) throws StripeTimestampExpiredException {
        try {
            long webhookTimestamp = Long.parseLong(timestamp);
            long currentTimeSeconds = System.currentTimeMillis() / 1000;
            long timeDifferenceSeconds = Math.abs(currentTimeSeconds - webhookTimestamp);
            long toleranceSeconds = stripeProperties.getWebhook().getTimestampToleranceSeconds();
            
            log.debug("Timestamp Validation: Webhook={}, Current={}, Difference={}s, Tolerance={}s",
                webhookTimestamp, currentTimeSeconds, timeDifferenceSeconds, toleranceSeconds);
            
            if (timeDifferenceSeconds > toleranceSeconds) {
                log.warn("❌ Webhook timestamp expired. Age: {}s, Tolerance: {}s",
                    timeDifferenceSeconds, toleranceSeconds);
                throw new StripeTimestampExpiredException(
                    String.format("Webhook timestamp is too old. Age: %d seconds, Tolerance: %d seconds", 
                        timeDifferenceSeconds, toleranceSeconds)
                );
            }
            
            log.debug("✅ Webhook timestamp validation successful (Age: {}s)", timeDifferenceSeconds);
            
        } catch (NumberFormatException e) {
            log.error("Invalid timestamp format: {}", timestamp);
            throw new StripeTimestampExpiredException("Invalid timestamp format: " + timestamp, e);
        }
    }
    
    /**
     * Computes HMAC-SHA256 hash of data using the webhook secret key.
     * 
     * @param data The data to hash
     * @param secret The secret key (webhook secret)
     * @return Hex-encoded HMAC-SHA256 hash
     * @throws NoSuchAlgorithmException If HmacSHA256 algorithm is not available
     * @throws InvalidKeyException If the key is invalid
     */
    private String computeHmacSha256(String data, String secret) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(), 0, secret.getBytes().length, "HmacSHA256");
        mac.init(keySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes());
        
        return bytesToHex(rawHmac);
    }
    
    /**
     * Converts byte array to hexadecimal string representation.
     * 
     * @param bytes The bytes to convert
     * @return Hex string representation
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * Compares two strings in constant time to prevent timing attacks.
     * Uses MessageDigest for comparison to avoid early exit on mismatch.
     * 
     * @param a First string
     * @param b Second string
     * @return true if strings are equal, false otherwise
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        
        byte[] aBytes = a.getBytes();
        byte[] bBytes = b.getBytes();
        
        if (aBytes.length != bBytes.length) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        
        return result == 0;
    }
}
