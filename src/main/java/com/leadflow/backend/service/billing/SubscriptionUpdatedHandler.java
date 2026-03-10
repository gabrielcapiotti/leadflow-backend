package com.leadflow.backend.service.billing;

import com.leadflow.backend.service.vendor.SubscriptionService;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles customer.subscription.updated webhook events from Stripe.
 * This event is triggered when a subscription is updated (status change, plan change, etc).
 * 
 * Reference: https://stripe.com/docs/api/events/types#event_types-customer.subscription.updated
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionUpdatedHandler implements StripeEventHandler {
    
    private final SubscriptionService subscriptionService;
    
    @Override
    public String getEventType() {
        return "customer.subscription.updated";
    }
    
    @Override
    public void handle(Event event) throws Exception {
        try {
            // Deserialize the event data to get the Subscription object
            Subscription subscription = (Subscription) event.getDataObjectDeserializer()
                    .getObject()
                    .orElseThrow(() -> new IllegalStateException("Subscription not found in event data"));
            
            log.info("Processing subscription updated: subscriptionId={}, customerId={}, status={}", 
                subscription.getId(), subscription.getCustomer(), subscription.getStatus());
            
            log.info("Processing subscription updated: subscriptionId={}, status={}, customerId={}", 
                subscription.getId(), subscription.getStatus(), subscription.getCustomer());
            
            // Sync subscription state with Stripe
            subscriptionService.syncWithStripe(subscription);
            log.info("✅ Synced subscription with Stripe: subscriptionId={}", subscription.getId());
            
            // Log subscription details
            if (subscription.getStatus() != null) {
                log.debug("Subscription status: {}", subscription.getStatus());
            }
            if (subscription.getCurrentPeriodEnd() != null) {
                log.debug("Current period ends at: {}", subscription.getCurrentPeriodEnd());
            }
            
        } catch (Exception e) {
            log.error("Error handling subscription updated event", e);
            throw e;
        }
    }
}
