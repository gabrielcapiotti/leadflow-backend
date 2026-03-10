package com.leadflow.backend.service.billing;

import com.stripe.model.Event;

/**
 * Interface for handling Stripe webhook events.
 * Different handlers implement this interface for specific event types.
 * 
 * Reference: https://stripe.com/docs/api/event_types
 */
public interface StripeEventHandler {
    
    /**
     * Handle a Stripe webhook event.
     * 
     * @param event The Stripe event to process
     * @throws Exception If processing fails
     */
    void handle(Event event) throws Exception;
    
    /**
     * Get the event type this handler is responsible for.
     * Examples: "invoice.payment_succeeded", "customer.subscription.deleted"
     * 
     * @return The Stripe event type string
     */
    String getEventType();
}
