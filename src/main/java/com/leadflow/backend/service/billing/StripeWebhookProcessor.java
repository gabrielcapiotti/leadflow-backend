package com.leadflow.backend.service.billing;

import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Routes Stripe webhook events to appropriate handlers.
 * Acts as a central processor for all Stripe events.
 * 
 * Handlers are registered in a map, allowing for easy extension
 * of event handling without modifying this class.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookProcessor {
    
    private final InvoicePaymentSucceededHandler invoicePaymentSucceededHandler;
    private final SubscriptionDeletedHandler subscriptionDeletedHandler;
    private final SubscriptionUpdatedHandler subscriptionUpdatedHandler;
    
    private Map<String, StripeEventHandler> handlers;
    
    /**
     * Initialize the handler registry.
     * This is called after dependency injection.
     */
    @SuppressWarnings("unused")
    private void initializeHandlers() {
        handlers = new HashMap<>();
        registerHandler(invoicePaymentSucceededHandler);
        registerHandler(subscriptionDeletedHandler);
        registerHandler(subscriptionUpdatedHandler);
        log.info("Stripe event handlers initialized: {}", handlers.keySet());
    }
    
    /**
     * Register an event handler.
     * 
     * @param handler The handler to register
     */
    public void registerHandler(StripeEventHandler handler) {
        if (handler != null) {
            handlers.put(handler.getEventType(), handler);
            log.debug("Registered handler for event type: {}", handler.getEventType());
        }
    }
    
    /**
     * Process a Stripe webhook event by routing it to the appropriate handler.
     * 
     * @param event The Stripe event to process
     * @throws Exception If processing fails
     */
    public void process(Event event) throws Exception {
        if (event == null) {
            log.warn("Received null event, skipping processing");
            return;
        }
        
        String eventType = event.getType();
        log.info("Processing Stripe event: type={}, id={}", eventType, event.getId());
        
        StripeEventHandler handler = getHandler(eventType);
        
        if (handler == null) {
            log.warn("No handler registered for event type: {}. Event will be skipped.", eventType);
            return;
        }
        
        try {
            handler.handle(event);
            log.info("✅ Event processed successfully: type={}, id={}", eventType, event.getId());
        } catch (Exception e) {
            log.error("❌ Error processing event: type={}, id={}", eventType, event.getId(), e);
            throw e;
        }
    }
    
    /**
     * Get a handler for a specific event type.
     * 
     * @param eventType The event type string
     * @return The handler, or null if not found
     */
    public StripeEventHandler getHandler(String eventType) {
        if (handlers == null) {
            initializeHandlers();
        }
        return handlers.get(eventType);
    }
    
    /**
     * Check if a handler exists for a given event type.
     * 
     * @param eventType The event type string
     * @return true if a handler is registered, false otherwise
     */
    public boolean hasHandler(String eventType) {
        if (handlers == null) {
            initializeHandlers();
        }
        return handlers.containsKey(eventType);
    }
}
