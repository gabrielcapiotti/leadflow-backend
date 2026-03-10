package com.leadflow.backend.entities;

/**
 * Status de assinatura do Stripe.
 */
public enum SubscriptionStatus {
    /**
     * Assinatura ativa e paga
     */
    ACTIVE,
    
    /**
     * Pagamento em atraso
     */
    PAST_DUE,
    
    /**
     * Assinatura cancelada
     */
    CANCELLED
}
