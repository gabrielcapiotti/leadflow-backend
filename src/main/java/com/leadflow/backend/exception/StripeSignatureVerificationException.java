package com.leadflow.backend.exception;

/**
 * Thrown when Stripe webhook signature verification fails.
 * This indicates an invalid or tampered webhook request.
 */
public class StripeSignatureVerificationException extends RuntimeException {
    
    public StripeSignatureVerificationException(String message) {
        super(message);
    }
    
    public StripeSignatureVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
