package com.leadflow.backend.service.billing;

import com.leadflow.backend.service.vendor.SubscriptionService;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles customer.subscription.deleted webhook events from Stripe.
 * This event is triggered when a subscription is deleted (either by the customer or after non-payment).
 * 
 * Reference: https://stripe.com/docs/api/events/types#event_types-customer.subscription.deleted
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionDeletedHandler implements StripeEventHandler {
    
    private final SubscriptionService subscriptionService;
    
    @Override
    public String getEventType() {
        return "customer.subscription.deleted";
    }
    
    @Override
    public void handle(Event event) throws Exception {
        try {
            // Deserialize the event data to get the Subscription object
            Subscription subscription = (Subscription) event.getDataObjectDeserializer()
                    .getObject()
                    .orElseThrow(() -> new IllegalStateException("Subscription not found in event data"));
            
            log.warn("Processing subscription deleted: subscriptionId={}, customerId={}, status={}", 
                subscription.getId(), subscription.getCustomer(), subscription.getStatus());
            
            // Mark subscription as deleted in the database
            subscriptionService.markAsDeletedFromStripe(subscription.getId());
            log.info("✅ Marked subscription as deleted from Stripe: subscriptionId={}, customerId={}", 
                subscription.getId(), subscription.getCustomer());
            
        } catch (Exception e) {
            log.error("Error handling subscription deleted event", e);
            throw e;
        }
    }
}
