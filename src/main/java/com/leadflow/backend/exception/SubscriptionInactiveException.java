package com.leadflow.backend.exception;

/**
 * Exception lançada quando uma operação requer assinatura ativa
 * mas a assinatura está inativa, cancelada ou expirada.
 * 
 * Deve retornar HTTP 402 PAYMENT_REQUIRED
 */
public class SubscriptionInactiveException extends RuntimeException {

    private String errorCode;

    public SubscriptionInactiveException(String message) {
        super(message);
        this.errorCode = "SUBSCRIPTION_INACTIVE";
    }

    public SubscriptionInactiveException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
