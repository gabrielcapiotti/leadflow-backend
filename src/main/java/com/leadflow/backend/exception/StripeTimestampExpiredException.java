package com.leadflow.backend.exception;

/**
 * Thrown when Stripe webhook timestamp validation fails.
 * This indicates a webhook that is too old or a potential replay attack.
 */
public class StripeTimestampExpiredException extends RuntimeException {
    
    public StripeTimestampExpiredException(String message) {
        super(message);
    }
    
    public StripeTimestampExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
