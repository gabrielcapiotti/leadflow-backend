package com.leadflow.backend.service.billing;

import com.leadflow.backend.service.vendor.SubscriptionService;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles invoice.payment_succeeded webhook events from Stripe.
 * This event is triggered when an invoice payment succeeds.
 * 
 * Reference: https://stripe.com/docs/api/events/types#event_types-invoice.payment_succeeded
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InvoicePaymentSucceededHandler implements StripeEventHandler {
    
    private final SubscriptionService subscriptionService;
    
    @Override
    public String getEventType() {
        return "invoice.payment_succeeded";
    }
    
    @Override
    public void handle(Event event) throws Exception {
        try {
            // Deserialize the event data to get the Invoice object
            Invoice invoice = (Invoice) event.getDataObjectDeserializer()
                    .getObject()
                    .orElseThrow(() -> new IllegalStateException("Invoice not found in event data"));
            
            log.info("Processing invoice payment succeeded: invoiceId={}, amount={}, customerId={}", 
                invoice.getId(), invoice.getAmountPaid(), invoice.getCustomer());
            
            // Extract subscription ID from invoice metadata if available
            String stripeSubscriptionId = invoice.getSubscription();
            String stripeCustomerId = invoice.getCustomer();
            
            if (stripeSubscriptionId != null && !stripeSubscriptionId.isBlank()) {
                // Mark payment as successful in the database
                subscriptionService.markPaymentSuccessful(stripeSubscriptionId, invoice.getId());
                log.info("✅ Marked payment successful: subscriptionId={}, invoiceId={}, amount={}", 
                    stripeSubscriptionId, invoice.getId(), invoice.getAmountPaid());
            } else {
                log.warn("⚠️  Invoice has no associated subscription: {}", invoice.getId());
            }
            
        } catch (Exception e) {
            log.error("Error handling invoice payment succeeded event", e);
            throw e;
        }
    }
}
