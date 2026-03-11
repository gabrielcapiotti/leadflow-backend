package com.leadflow.backend.service.vendor;

import com.leadflow.backend.entities.Plan;
import com.leadflow.backend.entities.Subscription;
import com.leadflow.backend.entities.SubscriptionAudit;
import com.leadflow.backend.entities.vendor.SubscriptionAccessLevel;
import com.leadflow.backend.entities.vendor.SubscriptionStatus;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.exception.SubscriptionInactiveException;
import com.leadflow.backend.repository.SubscriptionAuditRepository;
import com.leadflow.backend.repository.SubscriptionRepository;
import com.leadflow.backend.repository.VendorRepository;
import com.leadflow.backend.service.PlanService;
import com.leadflow.backend.service.notification.SubscriptionNotificationService;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.checkout.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class SubscriptionService {

    private final VendorRepository vendorRepository;
    private final SubscriptionAuditService auditService;
    private final PlanService planService;
    private final UsageService usageService;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionAuditRepository subscriptionAuditRepository;
    private final SubscriptionNotificationService notificationService;

    public SubscriptionService(VendorRepository vendorRepository,
                               SubscriptionAuditService auditService,
                               PlanService planService,
                               UsageService usageService,
                               SubscriptionRepository subscriptionRepository,
                               SubscriptionAuditRepository subscriptionAuditRepository,
                               SubscriptionNotificationService notificationService) {
        this.vendorRepository = vendorRepository;
        this.auditService = auditService;
        this.planService = planService;
        this.usageService = usageService;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionAuditRepository = subscriptionAuditRepository;
        this.notificationService = notificationService;
    }

    public void transition(Vendor vendor,
                           SubscriptionStatus newStatus,
                           String reason,
                           String externalEventId) {

        SubscriptionStatus previous = vendor.getSubscriptionStatus();

        if (!previous.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Transição inválida: " + previous + " → " + newStatus
            );
        }

        vendor.setSubscriptionStatus(newStatus);
        vendorRepository.save(vendor);

        auditService.record(
                vendor.getId(),
                previous,
                newStatus,
                reason,
                externalEventId
        );
    }

    public SubscriptionAccessLevel getAccessLevel(Vendor vendor) {

        SubscriptionStatus status = vendor.getSubscriptionStatus();

        return switch (status) {
            case ATIVA, TRIAL -> SubscriptionAccessLevel.FULL;
            case INADIMPLENTE -> SubscriptionAccessLevel.READ_ONLY;
            case CANCELADA, EXPIRADA, SUSPENSA -> SubscriptionAccessLevel.BLOCKED;
        };
    }

    @Transactional
    public void expireSubscriptions() {

        List<Vendor> vendors = vendorRepository.findBySubscriptionStatus(SubscriptionStatus.ATIVA);
        Instant now = Instant.now();

        for (Vendor vendor : vendors) {
            if (vendor.getSubscriptionExpiresAt() != null && vendor.getSubscriptionExpiresAt().isBefore(now)) {
                transition(vendor, SubscriptionStatus.EXPIRADA, "AUTO_EXPIRED_BY_SCHEDULER", null);
            }
        }
    }

    
    @Transactional
    public void activateAccount(Session session) {

        String stripeCustomerId = session.getCustomer();
        String stripeSubscriptionId = session.getSubscription();

        log.info("Stripe checkout completed");

        /*
         ----------------------------------------
         IDEMPOTENCY CHECK
         ----------------------------------------
         */

        Optional<Subscription> existing =
                subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);

        if (existing.isPresent()) {

            log.warn("Subscription already processed: {}", stripeSubscriptionId);
            return;

        }

        /*
         ----------------------------------------
         EXTRACT METADATA
         ----------------------------------------
         */

        String tenantIdString = session.getMetadata().get("tenantId");
        String email = session.getMetadata().get("email");

        if (tenantIdString == null) {
            log.error("Missing tenantId in Stripe checkout session metadata");
            throw new IllegalStateException("TenantId not found in session metadata");
        }

        UUID tenantId = UUID.fromString(tenantIdString);

        log.info("Activating tenant {} for {}", tenantId, email);

        /*
         ----------------------------------------
         GET PLAN
         ----------------------------------------
         */

        Plan plan = planService.getActivePlan();

        /*
         ----------------------------------------
         CREATE SUBSCRIPTION
         ----------------------------------------
         */

        Subscription subscription = new Subscription();

        subscription.setTenantId(tenantId);
        subscription.setStripeCustomerId(stripeCustomerId);
        subscription.setStripeSubscriptionId(stripeSubscriptionId);
        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        subscription.setPlan(plan);
        subscription.setStartedAt(LocalDateTime.now());

        subscriptionRepository.save(subscription);

        /*
         ----------------------------------------
         INITIALIZE USAGE LIMITS
         ----------------------------------------
         */

        usageService.initializeUsage(tenantId, plan);

        log.info("Tenant {} activated with plan {}", tenantId, plan.getName());

    }

    @Transactional
    public void handlePaymentSucceeded(Event event) {
        if (event == null) {
            return;
        }

        log.info("Processing invoice.payment_succeeded event");

        Optional<Object> object = event.getDataObjectDeserializer().getObject().map(o -> (Object) o);
        if (object.isEmpty() || !(object.get() instanceof Invoice invoice)) {
            log.warn("Stripe invoice.payment_succeeded received without Invoice payload. eventId={}", event.getId());
            return;
        }

        String stripeSubscriptionId = invoice.getSubscription();

        Optional<Subscription> subscriptionOpt =
                subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);

        if (subscriptionOpt.isEmpty()) {
            log.warn("Subscription not found for renewal: {}", stripeSubscriptionId);
            return;
        }

        Subscription subscription = subscriptionOpt.get();

        // Renovar período da assinatura
        LocalDateTime nextPeriod = LocalDateTime.now().plusMonths(1);

        subscription.setExpiresAt(nextPeriod);
        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);

        subscriptionRepository.save(subscription);

        log.info("Subscription renewed for tenant: {}", subscription.getTenantId());
    }

    @Transactional
    public void handleSubscriptionCancelled(Event event) {
        if (event == null) {
            return;
        }

        log.info("Processing customer.subscription.deleted event");

        Optional<Object> object = event.getDataObjectDeserializer().getObject().map(o -> (Object) o);
        if (object.isEmpty() || !(object.get() instanceof com.stripe.model.Subscription stripeSubscription)) {
            log.warn("Stripe customer.subscription.deleted received without Subscription payload. eventId={}", event.getId());
            return;
        }

        String stripeSubscriptionId = stripeSubscription.getId();

        Optional<Subscription> subscriptionOpt =
                subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);

        if (subscriptionOpt.isEmpty()) {
            log.warn("Subscription not found for cancellation: {}", stripeSubscriptionId);
            return;
        }

        Subscription subscription = subscriptionOpt.get();

        subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);

        subscriptionRepository.save(subscription);

        log.info("Subscription cancelled for tenant: {}", subscription.getTenantId());
    }

    @Transactional
    public void handlePaymentFailed(Event event) {
        if (event == null) {
            return;
        }

        log.info("Processing invoice.payment_failed event");

        Optional<Object> object = event.getDataObjectDeserializer().getObject().map(o -> (Object) o);
        if (object.isEmpty() || !(object.get() instanceof Invoice invoice)) {
            log.warn("Stripe invoice.payment_failed received without Invoice payload. eventId={}", event.getId());
            return;
        }

        String stripeSubscriptionId = invoice.getSubscription();

        Optional<Subscription> subscriptionOpt =
                subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);

        if (subscriptionOpt.isEmpty()) {
            log.warn("Subscription not found for payment failure: {}", stripeSubscriptionId);
            return;
        }

        Subscription subscription = subscriptionOpt.get();

        // Atualizar status para PAST_DUE se ainda está ACTIVE
        if (subscription.getStatus() == Subscription.SubscriptionStatus.ACTIVE) {
            subscription.setStatus(Subscription.SubscriptionStatus.PAST_DUE);
            subscriptionRepository.save(subscription);
            
            log.warn("Payment failed - Subscription marked as PAST_DUE for tenant: {}", subscription.getTenantId());
        }
    }

    @Transactional
    public void handleSubscriptionUpdated(Event event) {
        if (event == null) {
            return;
        }

        log.info("Processing customer.subscription.updated event");

        Optional<Object> object = event.getDataObjectDeserializer().getObject().map(o -> (Object) o);
        if (object.isEmpty() || !(object.get() instanceof com.stripe.model.Subscription stripeSubscription)) {
            log.warn("Stripe customer.subscription.updated received without Subscription payload. eventId={}", event.getId());
            return;
        }

        String stripeSubscriptionId = stripeSubscription.getId();

        Optional<Subscription> subscriptionOpt =
                subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);

        if (subscriptionOpt.isEmpty()) {
            log.warn("Subscription not found for update: {}", stripeSubscriptionId);
            return;
        }

        Subscription subscription = subscriptionOpt.get();

        // Sincronizar status principal
        String stripeStatus = stripeSubscription.getStatus();
        if ("active".equalsIgnoreCase(stripeStatus)) {
            subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        } else if ("past_due".equalsIgnoreCase(stripeStatus)) {
            subscription.setStatus(Subscription.SubscriptionStatus.PAST_DUE);
        } else if ("canceled".equalsIgnoreCase(stripeStatus)) {
            subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
        }

        // Sincronizar plano se foi alterado
        Long currentPeriodEnd = stripeSubscription.getCurrentPeriodEnd();
        if (currentPeriodEnd != null) {
            LocalDateTime newExpiresAt = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(currentPeriodEnd),
                java.time.ZoneId.systemDefault()
            );
            subscription.setExpiresAt(newExpiresAt);
        }

        subscriptionRepository.save(Objects.requireNonNull(subscription));

        log.info("Subscription updated for tenant: {} - Status: {}", 
            subscription.getTenantId(), subscription.getStatus());
    }

    @Transactional
    public void recordAuditTrail(Subscription subscription, 
                                  Subscription.SubscriptionStatus statusFrom,
                                  Subscription.SubscriptionStatus statusTo,
                                  String reason,
                                  String stripeEventId) {
        try {
            subscriptionAuditRepository.save(
                Objects.requireNonNull(
                    SubscriptionAudit.builder()
                        .subscriptionId(subscription.getId())
                        .tenantId(subscription.getTenantId())
                        .stripeSubscriptionId(subscription.getStripeSubscriptionId())
                        .statusFrom(statusFrom)
                        .statusTo(statusTo)
                        .reason(reason)
                        .stripeEventId(stripeEventId)
                        .build()
                )
            );
            
            log.info("Audit trail recorded: {} -> {} for tenant {}", 
                statusFrom, statusTo, subscription.getTenantId());
        } catch (Exception e) {
            log.error("Failed to record audit trail", e);
        }
    }

    @Transactional
    public void notifyExpiringSubscriptions() {
        List<Subscription> expiringSubscriptions = subscriptionRepository
            .findByStatusAndExpiresAtBetween(
                Subscription.SubscriptionStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(7)
            );

        for (Subscription subscription : expiringSubscriptions) {
            try {
                notificationService.sendExpirationReminder(subscription);
                log.info("Expiration reminder sent for tenant: {}", subscription.getTenantId());
            } catch (Exception e) {
                log.error("Failed to send expiration reminder for tenant: {}", 
                    subscription.getTenantId(), e);
            }
        }
    }

    /**
     * Validates if the subscription associated with the given tenant is active.
     * Throws SubscriptionInactiveException if subscription is not active.
     * 
     * @param tenantId the tenant ID to validate
     * @throws SubscriptionInactiveException if subscription does not exist or is not ACTIVE
     */
    public void validateActiveSubscription(UUID tenantId) {
        Subscription subscription = subscriptionRepository.findByTenantId(tenantId)
            .orElseThrow(() -> {
                log.warn("Subscription not found for tenant: {}", tenantId);
                return new SubscriptionInactiveException(
                    "Subscription not found for tenant: " + tenantId,
                    "SUBSCRIPTION_NOT_FOUND"
                );
            });

        if (subscription.getStatus() != Subscription.SubscriptionStatus.ACTIVE) {
            log.warn("Subscription is not active. TenantId: {}, Status: {}", 
                tenantId, subscription.getStatus());
            throw new SubscriptionInactiveException(
                "Subscription is " + subscription.getStatus().name() + ". Expires at: " + subscription.getExpiresAt(),
                "SUBSCRIPTION_INACTIVE"
            );
        }

        log.debug("Subscription validation passed for tenant: {}", tenantId);
    }

    /**
     * Retrieves the subscription associated with the given tenant.
     * 
     * @param tenantId the tenant ID
     * @return Subscription details
     * @throws IllegalStateException if subscription not found
     */
    public Subscription getSubscriptionByTenant(UUID tenantId) {
        return subscriptionRepository.findByTenantId(tenantId)
            .orElseThrow(() -> {
                log.warn("Subscription not found for tenant: {}", tenantId);
                return new IllegalStateException("Subscription not found for tenant: " + tenantId);
            });
    }

    /**
     * Cancels the subscription for a tenant by calling Stripe API.
     * Updates local subscription status to CANCELLED.
     * 
     * @param tenantId the tenant ID
     * @throws IllegalStateException if subscription not found
     * @throws RuntimeException if Stripe API call fails
     */
    @Transactional
    public void cancelSubscription(UUID tenantId) {
        Subscription subscription = getSubscriptionByTenant(tenantId);

        try {
            // Call Stripe API to cancel subscription
            String stripeSubscriptionId = subscription.getStripeSubscriptionId();
            
            if (stripeSubscriptionId == null || stripeSubscriptionId.isBlank()) {
                throw new IllegalStateException("Stripe subscription ID not set for tenant: " + tenantId);
            }

            com.stripe.model.Subscription stripeSubscription = 
                com.stripe.model.Subscription.retrieve(stripeSubscriptionId);

            stripeSubscription.cancel();

            // Update local subscription status
            subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
            subscriptionRepository.save(subscription);

            // Record audit trail
            recordAuditTrail(
                subscription,
                Subscription.SubscriptionStatus.ACTIVE,
                Subscription.SubscriptionStatus.CANCELLED,
                "USER_REQUESTED_CANCELLATION",
                stripeSubscriptionId
            );

            log.info("Subscription cancelled for tenant: {} (Stripe: {})", tenantId, stripeSubscriptionId);

        } catch (com.stripe.exception.StripeException e) {
            log.error("Failed to cancel Stripe subscription for tenant: {}", tenantId, e);
            throw new RuntimeException("Stripe cancellation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Mark a payment as successful from webhook event.
     * Called when invoice.payment_succeeded webhook is received.
     * 
     * @param stripeSubscriptionId the Stripe subscription ID
     * @param invoiceId the Stripe invoice ID
     */
    @Transactional
    public void markPaymentSuccessful(String stripeSubscriptionId, String invoiceId) {
        try {
            Subscription subscription = subscriptionRepository
                .findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseThrow(() -> {
                    log.warn("Subscription not found for Stripe ID: {}", stripeSubscriptionId);
                    return new IllegalArgumentException("Subscription not found: " + stripeSubscriptionId);
                });

            subscription.setLastPaymentDate(LocalDateTime.now());
            subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(subscription);

            recordAuditTrail(
                subscription,
                subscription.getStatus(),
                Subscription.SubscriptionStatus.ACTIVE,
                "PAYMENT_SUCCESSFUL",
                invoiceId
            );

            log.info("✅ Marked payment successful: stripeSubId={}, invoiceId={}", stripeSubscriptionId, invoiceId);

        } catch (Exception e) {
            log.error("❌ Error marking payment successful", e);
            throw new RuntimeException("Failed to mark payment successful", e);
        }
    }

    /**
     * Mark a subscription as deleted from Stripe.
     * Called when customer.subscription.deleted webhook is received.
     * 
     * @param stripeSubscriptionId the Stripe subscription ID
     */
    @Transactional
    public void markAsDeletedFromStripe(String stripeSubscriptionId) {
        try {
            Subscription subscription = subscriptionRepository
                .findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseThrow(() -> {
                    log.warn("Subscription not found for Stripe ID: {}", stripeSubscriptionId);
                    return new IllegalArgumentException("Subscription not found: " + stripeSubscriptionId);
                });

            Subscription.SubscriptionStatus previousStatus = subscription.getStatus();
            subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
            subscription.setCancelledAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);

            recordAuditTrail(
                subscription,
                previousStatus,
                Subscription.SubscriptionStatus.CANCELLED,
                "DELETED_FROM_STRIPE",
                stripeSubscriptionId
            );

            log.info("✅ Marked subscription as deleted: stripeSubId={}", stripeSubscriptionId);

        } catch (Exception e) {
            log.error("❌ Error marking subscription as deleted", e);
            throw new RuntimeException("Failed to mark subscription as deleted", e);
        }
    }

    /**
     * Sync subscription state with data from Stripe.
     * Called when customer.subscription.updated webhook is received.
     * 
     * @param stripeSubscription the Stripe subscription object
     */
    @Transactional
    public void syncWithStripe(com.stripe.model.Subscription stripeSubscription) {
        try {
            String stripeSubscriptionId = stripeSubscription.getId();
            
            Subscription subscription = subscriptionRepository
                .findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseThrow(() -> {
                    log.warn("Subscription not found for Stripe ID: {}", stripeSubscriptionId);
                    return new IllegalArgumentException("Subscription not found: " + stripeSubscriptionId);
                });

            Subscription.SubscriptionStatus previousStatus = subscription.getStatus();

            // Sync status from Stripe
            String stripeStatus = stripeSubscription.getStatus();
            Subscription.SubscriptionStatus newStatus = switch (stripeStatus) {
                case "active" -> Subscription.SubscriptionStatus.ACTIVE;
                case "past_due" -> Subscription.SubscriptionStatus.PAST_DUE;
                case "canceled", "cancelled" -> Subscription.SubscriptionStatus.CANCELLED;
                case "incomplete" -> Subscription.SubscriptionStatus.INCOMPLETE;
                case "incomplete_expired" -> Subscription.SubscriptionStatus.INCOMPLETE;
                default -> previousStatus;
            };

            subscription.setStatus(newStatus);

            // Sync period dates
            if (stripeSubscription.getCurrentPeriodStart() != null) {
                subscription.setStartedAt(
                    LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodStart()),
                        java.time.ZoneId.systemDefault()
                    )
                );
            }

            if (stripeSubscription.getCurrentPeriodEnd() != null) {
                subscription.setExpiresAt(
                    LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodEnd()),
                        java.time.ZoneId.systemDefault()
                    )
                );
            }

            subscriptionRepository.save(subscription);

            if (previousStatus != newStatus) {
                recordAuditTrail(
                    subscription,
                    previousStatus,
                    newStatus,
                    "SYNCED_FROM_STRIPE",
                    stripeSubscriptionId
                );
                log.info("✅ Synced subscription with Stripe: stripeSubId={}, status: {} → {}", 
                    stripeSubscriptionId, previousStatus, newStatus);
            } else {
                log.debug("Subscription in sync with Stripe: stripeSubId={}", stripeSubscriptionId);
            }

        } catch (Exception e) {
            log.error("❌ Error syncing subscription with Stripe", e);
            throw new RuntimeException("Failed to sync subscription with Stripe", e);
        }
    }
}
