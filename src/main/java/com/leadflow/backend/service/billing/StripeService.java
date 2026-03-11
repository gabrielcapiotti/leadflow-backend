package com.leadflow.backend.service.billing;

import com.leadflow.backend.dto.billing.CheckoutRequest;
import com.leadflow.backend.dto.billing.CheckoutResponse;
import com.leadflow.backend.entities.billing.PaymentCheckoutRequest;
import com.leadflow.backend.repository.PaymentCheckoutRequestRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    private final PaymentCheckoutRequestRepository checkoutRepository;
    private final BillingTenantProvisioningService provisioningService;

    @Value("${stripe.secret-key:${stripe.secret.key:}}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret:${stripe.webhook.secret:}}")
    private String webhookSecret;

    @Value("${stripe.success-url:${app.frontend.success-url}}")
    private String successUrl;

    @Value("${stripe.cancel-url:${app.frontend.cancel-url}}")
    private String cancelUrl;

    @Value("${stripe.price-id:${stripe.price.default:}}")
    private String priceId;

    @Value("${stripe.price.pro:${stripe.price-id:}}")
    private String proPriceId;

    @PostConstruct
    public void init() {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            log.warn("Stripe secret key is not configured - Stripe integration will not be available");
            return;
        }

        Stripe.apiKey = stripeSecretKey;
        log.info("Stripe initialized");
    }

    public CheckoutResponse createCheckoutSession(CheckoutRequest request) {

        if (request == null || request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Email is required to create checkout session");
        }

        Session session = createCheckoutSession(request.email(), request.tenantId());
        String referenceId = session.getMetadata() != null ? session.getMetadata().get("referenceId") : null;
        return new CheckoutResponse(session.getUrl(), referenceId, "stripe");
    }

    public Session createCheckoutSession(String email, Long tenantId) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required to create checkout session");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String referenceId = UUID.randomUUID().toString();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("email", normalizedEmail);
        metadata.put("plan", "default");
        metadata.put("referenceId", referenceId);

        if (tenantId != null) {
            metadata.put("tenantId", String.valueOf(tenantId));
            log.debug("Checkout session created for existing tenant: {}", tenantId);
        } else {
            log.debug("Checkout session created for new tenant signup");
        }

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                        .setCustomerEmail(normalizedEmail)
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                    .setPrice(priceId)
                                        .setQuantity(1L)
                                        .build()
                        )
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl)
                        .putAllMetadata(metadata)
                        .build();

        try {
            Session session = Session.create(params);
            savePendingCheckout(normalizedEmail, "default", referenceId);
            return session;
        } catch (StripeException e) {
            log.error("Stripe checkout creation failed for email={}", normalizedEmail, e);
            throw new RuntimeException("Stripe checkout creation failed", e);
        }
    }

    public Session retrieveSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Stripe session id cannot be blank");
        }

        try {
            return Session.retrieve(sessionId);
        } catch (StripeException e) {
            log.error("Failed to retrieve stripe session id={}", sessionId, e);
            throw new RuntimeException("Stripe session retrieval failed", e);
        }
    }

    public Customer createCustomer(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Customer email cannot be blank");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("email", normalizedEmail);
            return Customer.create(params);
        } catch (StripeException e) {
            log.error("Stripe customer creation failed for email={}", normalizedEmail, e);
            throw new RuntimeException("Customer creation failed", e);
        }
    }

    public Event constructWebhookEvent(String payload, String signature) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Webhook payload cannot be blank");
        }

        if (signature == null || signature.isBlank()) {
            throw new IllegalArgumentException("Stripe signature cannot be blank");
        }

        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException("Stripe webhook secret is not configured");
        }

        try {
            return Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe webhook signature", e);
            throw new RuntimeException("Invalid webhook signature", e);
        }
    }

    public Optional<Session> extractCheckoutSession(Event event) {
        if (event == null) {
            return Optional.empty();
        }

        return event.getDataObjectDeserializer()
                .getObject()
                .filter(Session.class::isInstance)
                .map(Session.class::cast);
    }

    public void processCheckoutCompletedWebhook(String payload, String signature) {
        Event event = constructWebhookEvent(payload, signature);

        if (!"checkout.session.completed".equals(event.getType())) {
            return;
        }

        processCheckoutCompletedEvent(event, payload);
    }

    public void processCheckoutCompletedEvent(Event event, String payload) {
        Session session = extractCheckoutSession(event)
                .orElseThrow(() -> new RuntimeException("Invalid checkout session payload"));

        provisioningService.provisionFromCheckout(event.getId(), session, payload);
    }

    private void savePendingCheckout(String email, String plan, String referenceId) {
        PaymentCheckoutRequest checkoutRequest = new PaymentCheckoutRequest();
        checkoutRequest.setReferenceId(referenceId);
        checkoutRequest.setProvider("stripe");
        checkoutRequest.setEmail(email);
        checkoutRequest.setNomeVendedor(localPart(email));
        checkoutRequest.setWhatsappVendedor("0000000000");
        checkoutRequest.setNomeEmpresa(null);
        checkoutRequest.setSlug(localPart(email) + "-" + UUID.randomUUID().toString().substring(0, 6));
        checkoutRequest.setStatus("PENDING_" + plan.toUpperCase(Locale.ROOT));
        checkoutRepository.save(checkoutRequest);
    }

    private String resolvePriceId(String plan) {
        String resolved = "pro".equals(plan) ? proPriceId : priceId;
        if (resolved == null || resolved.isBlank()) {
            throw new IllegalStateException("Stripe price id is not configured");
        }
        return resolved;
    }

    private String localPart(String email) {
        if (!email.contains("@")) {
            return "vendor";
        }
        return email.substring(0, email.indexOf('@'));
    }

    /**
     * Processes Stripe webhook events with support for multiple event types.
     * 
     * Supported events:
     * - checkout.session.completed: Process successful checkout and provision tenant
     * - invoice.payment_succeeded: Handle successful subscription payment
     * - customer.subscription.deleted: Handle subscription cancellation
     * 
     * @param payload webhook event payload from Stripe
     * @param signature Stripe-Signature header value for verification
     * @throws RuntimeException if signature verification fails or event processing fails
     */
    public void processWebhook(String payload, String signature) {
        Event event = constructWebhookEvent(payload, signature);
        String eventType = event.getType();
        
        log.info("Processing Stripe webhook event: type={}, id={}", eventType, event.getId());
        
        switch (eventType) {
            case "checkout.session.completed":
                handleCheckoutSessionCompleted(event, payload);
                break;
            
            case "invoice.payment_succeeded":
                handleInvoicePaymentSucceeded(event);
                break;
            
            case "customer.subscription.deleted":
                handleSubscriptionDeleted(event);
                break;
            
            default:
                log.debug("Unhandled Stripe webhook event type: {}", eventType);
        }
    }
    
    /**
     * Handles checkout.session.completed event.
     * Provisions new tenant with schema, admin user, and usage limits.
     */
    private void handleCheckoutSessionCompleted(Event event, String payload) {
        Session session = extractCheckoutSession(event)
                .orElseThrow(() -> new RuntimeException("Invalid checkout session payload"));
        
        provisioningService.provisionFromCheckout(event.getId(), session, payload);
        log.info("Checkout session completed and tenant provisioned: eventId={}", event.getId());
    }
    
    /**
     * Handles invoice.payment_succeeded event.
     * Updates subscription status and extends billing period.
     */
    private void handleInvoicePaymentSucceeded(Event event) {
        // Invoice payment succeeded - subscription renewal
        // This can be enhanced to update subscription renewal dates
        log.info("Invoice payment succeeded: eventId={}", event.getId());
        
        // Future enhancement: update subscription.expires_at based on invoice period
        // For now, we log the event as the initial checkout already provisions the tenant
    }
    
    /**
     * Handles customer.subscription.deleted event.
     * Marks subscription as cancelled and potentially disables tenant access.
     */
    private void handleSubscriptionDeleted(Event event) {
        // Subscription cancelled - mark vendor as inactive
        log.info("Subscription deleted: eventId={}", event.getId());
        
        // Future enhancement: transition vendor status to CANCELLED
        // This requires extracting customer ID from event and finding associated vendor
    }

    /**
     * Routes Stripe webhook event to appropriate handler.
     * Called by StripeWebhookProcessingService for safe retry processing.
     */
    public void routeWebhookEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        String eventType = event.getType();
        log.info("Routing Stripe webhook event: type={}, id={}", eventType, event.getId());

        switch (eventType) {
            case "checkout.session.completed":
                handleCheckoutSessionCompleted(event, event.toJson());
                break;

            case "invoice.payment_succeeded":
                handleInvoicePaymentSucceeded(event);
                break;

            case "customer.subscription.deleted":
                handleSubscriptionDeleted(event);
                break;

            default:
                log.debug("Unhandled Stripe webhook event type: {}", eventType);
        }
    }

    /**
     * Creates a Stripe checkout session and returns complete checkout information.
     * This is the primary method for checkout creation, following Controller → Service → DTO pattern.
     * 
     * @param email customer email address (will be normalized)
     * @return CheckoutResponse with checkout URL, reference ID, and provider
     * @throws IllegalArgumentException if email is null or blank
     * @throws RuntimeException if Stripe session creation fails
     */
    public CheckoutResponse createCheckout(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required to create checkout session");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String referenceId = UUID.randomUUID().toString();

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                        .setCustomerEmail(normalizedEmail)
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setPrice(resolvePriceId("default"))
                                        .setQuantity(1L)
                                        .build()
                        )
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl)
                        .putMetadata("email", normalizedEmail)
                        .putMetadata("plan", "default")
                        .putMetadata("referenceId", referenceId)
                        .build();

        try {
            Session session = Session.create(params);
            savePendingCheckout(normalizedEmail, "default", referenceId);
            
            return new CheckoutResponse(
                session.getUrl(),
                referenceId,
                "stripe"
            );
        } catch (StripeException e) {
            log.error("Stripe checkout creation failed for email={}", normalizedEmail, e);
            throw new RuntimeException("Failed to create checkout session", e);
        }
    }

    /**
     * Processes a Stripe webhook event payload without signature verification.
     * Used when signature has already been validated by StripeWebhookValidator.
     *
     * @param payload raw webhook payload JSON from Stripe
     * @return event type that was processed
     */
    public String processWebhookEvent(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Webhook payload cannot be blank");
        }

        try {
            // Parse JSON payload directly without signature verification
            // (signature was already validated by StripeWebhookValidator)
            com.google.gson.JsonObject jsonPayload = com.google.gson.JsonParser.parseString(payload).getAsJsonObject();
            String eventType = jsonPayload.get("type").getAsString();
            String eventId = jsonPayload.get("id").getAsString();
            
            log.info("Processing Stripe webhook event: type={}, id={}", eventType, eventId);
            
            switch (eventType) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompletedFromPayload(jsonPayload, payload);
                    break;
                
                case "invoice.payment_succeeded":
                    log.info("Invoice payment succeeded: eventId={}", eventId);
                    break;
                
                case "customer.subscription.deleted":
                    log.info("Subscription deleted: eventId={}", eventId);
                    break;
                
                default:
                    log.debug("Unhandled Stripe webhook event type: {}", eventType);
            }
            
            return eventType;
            
        } catch (Exception e) {
            log.error("Error parsing webhook payload", e);
            throw new RuntimeException("Failed to process webhook event", e);
        }
    }

    /**
     * Handles checkout.session.completed event from JSON payload.
     */
    private void handleCheckoutSessionCompletedFromPayload(com.google.gson.JsonObject jsonPayload, String payload) {
        try {
            com.google.gson.JsonObject dataObject = jsonPayload.getAsJsonObject("data");
            com.google.gson.JsonObject objectData = dataObject.getAsJsonObject("object");
            
            String checkoutSessionId = objectData.get("id").getAsString();
            
            log.info("Processing checkout.session.completed: sessionId={}", checkoutSessionId);
            
            // Retrieve the Session object to maintain consistency with existing provisioning flow
            Session session = retrieveSession(checkoutSessionId);
            
            // Provision tenant with payment confirmed
            provisioningService.provisionFromCheckout(jsonPayload.get("id").getAsString(), session, payload);
            
        } catch (Exception e) {
            log.error("Error handling checkout session completed", e);
            throw new RuntimeException("Failed to handle checkout.session.completed event", e);
        }
    }

}
