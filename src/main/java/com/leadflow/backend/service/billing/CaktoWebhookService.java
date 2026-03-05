package com.leadflow.backend.service.billing;

import com.leadflow.backend.dto.billing.CaktoWebhookPayload;
import com.leadflow.backend.entities.billing.PaymentCheckoutRequest;
import com.leadflow.backend.entities.vendor.SubscriptionStatus;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.entities.vendor.VendorAuditLog;
import com.leadflow.backend.repository.VendorAuditLogRepository;
import com.leadflow.backend.repository.VendorRepository;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;
import com.leadflow.backend.service.monitoring.SystemAlertService;
import com.leadflow.backend.service.vendor.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CaktoWebhookService {

    private static final Logger log = LoggerFactory.getLogger(CaktoWebhookService.class);
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private final VendorRepository vendorRepository;
    private final PaymentEventService paymentEventService;
    private final CheckoutService checkoutService;
    private final VendorAuditLogRepository vendorAuditLogRepository;
    private final SubscriptionService subscriptionService;
    private final SystemAlertService systemAlertService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${cakto.webhook.secret:${billing.cakto.webhook-secret:}}")
    private String webhookSecret;

    public CaktoWebhookService(VendorRepository vendorRepository,
                               PaymentEventService paymentEventService,
                               CheckoutService checkoutService,
                               VendorAuditLogRepository vendorAuditLogRepository,
                               SubscriptionService subscriptionService,
                               SystemAlertService systemAlertService,
                               UserRepository userRepository,
                               RoleRepository roleRepository,
                               PasswordEncoder passwordEncoder) {
        this.vendorRepository = vendorRepository;
        this.paymentEventService = paymentEventService;
        this.checkoutService = checkoutService;
        this.vendorAuditLogRepository = vendorAuditLogRepository;
        this.subscriptionService = subscriptionService;
        this.systemAlertService = systemAlertService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void process(CaktoWebhookPayload payload, String signatureHeader) {

        try {

        validateSignature(signatureHeader);

        if (payload == null || payload.getId() == null || payload.getId().isBlank()) {
            return;
        }

        log.info("payment_webhook_received provider={} eventId={}", "cakto", payload.getId());

        if (!paymentEventService.registerIfFirstProcess(payload.getId(), "cakto")) {
            log.info("duplicate_webhook_ignored provider={} eventId={}", "cakto", payload.getId());
            return;
        }

        String email = resolveEmail(payload);
        String status = resolveStatus(payload);

        if (email == null || email.isBlank() || status == null || status.isBlank()) {
            return;
        }

        Vendor vendor = vendorRepository
                .findByUserEmail(email)
                .stream()
                .findFirst()
                .orElse(null);

        PaymentCheckoutRequest pendingCheckout = checkoutService.consumePendingByEmail(email);

        String normalizedStatus = status.toLowerCase(Locale.ROOT);

        if ("approved".equals(normalizedStatus) && vendor == null) {
            ensureUserAccount(email, pendingCheckout);
            vendor = createVendorFromPayment(email, pendingCheckout);
        }

        if (vendor == null) {
            return;
        }

        boolean transitioned;
        if ("approved".equals(normalizedStatus)) {
            transitioned = activateSubscription(vendor, payload);
        } else if ("canceled".equals(normalizedStatus)
                || "expired".equals(normalizedStatus)
                || "failed".equals(normalizedStatus)) {
            transitioned = deactivateSubscription(vendor, normalizedStatus, payload.getId());
        } else {
            return;
        }

        if (!transitioned) {
            vendorRepository.save(vendor);
        }

        registerAudit(vendor, payload, normalizedStatus);

        log.info("payment_webhook_processed provider={} eventId={} vendorId={} status={}",
            "cakto",
            payload.getId(),
            vendor.getId(),
            normalizedStatus);

        } catch (SecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            String eventId = payload != null ? payload.getId() : "unknown";
            systemAlertService.sendCriticalAlert(
                    "Falha no webhook Cakto. eventId=" + eventId + ", erro=" + ex.getMessage()
            );
            throw ex;
        }
    }

    private boolean activateSubscription(Vendor vendor, CaktoWebhookPayload payload) {
        if (vendor.getSubscriptionStartedAt() == null) {
            vendor.setSubscriptionStartedAt(Instant.now());
        }

        vendor.setExternalSubscriptionId(resolveSubscriptionId(payload));
        vendor.setLastPaymentAt(Instant.now());
        vendor.setSubscriptionExpiresAt(resolveNextBillingAt(payload));
        vendor.setNextBillingAt(resolveNextBillingAt(payload));

        if (vendor.getSubscriptionStatus() != SubscriptionStatus.ATIVA) {
            subscriptionService.transition(
                    vendor,
                    SubscriptionStatus.ATIVA,
                    "WEBHOOK_APPROVED",
                    payload.getId()
            );
            return true;
        }

        return false;
    }

    private boolean deactivateSubscription(Vendor vendor,
                                          String normalizedStatus,
                                          String externalEventId) {
        SubscriptionStatus targetStatus = switch (normalizedStatus) {
            case "canceled" -> SubscriptionStatus.CANCELADA;
            case "expired" -> SubscriptionStatus.EXPIRADA;
            case "failed" -> SubscriptionStatus.INADIMPLENTE;
            default -> SubscriptionStatus.SUSPENSA;
        };

        String reason = switch (normalizedStatus) {
            case "canceled" -> "WEBHOOK_CANCELED";
            case "expired" -> "WEBHOOK_EXPIRED";
            case "failed" -> "WEBHOOK_FAILED";
            default -> "WEBHOOK_STATUS_CHANGE";
        };

        if (vendor.getSubscriptionStatus() != targetStatus) {
            subscriptionService.transition(
                    vendor,
                    targetStatus,
                    reason,
                    externalEventId
            );
            return true;
        }

        return false;
    }

    private void validateSignature(String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new SecurityException("Missing webhook signature");
        }

        if (webhookSecret == null || webhookSecret.isBlank() || !signatureHeader.equals(webhookSecret)) {
            throw new SecurityException("Invalid webhook signature");
        }
    }

    private String resolveEmail(CaktoWebhookPayload payload) {
        if (payload == null || payload.getData() == null || payload.getData().getCustomer() == null) {
            return null;
        }

        return payload.getData().getCustomer().getEmail();
    }

    private String resolveStatus(CaktoWebhookPayload payload) {
        if (payload == null || payload.getData() == null) {
            return null;
        }

        return payload.getData().getStatus();
    }

    private String resolveSubscriptionId(CaktoWebhookPayload payload) {
        if (payload == null || payload.getData() == null) {
            return null;
        }

        return payload.getData().getSubscriptionId();
    }

    private Instant resolveNextBillingAt(CaktoWebhookPayload payload) {
        if (payload == null || payload.getData() == null) {
            return null;
        }

        return payload.getData().getNextBillingAt();
    }

    private void registerAudit(Vendor vendor,
                               CaktoWebhookPayload payload,
                               String normalizedStatus) {
        VendorAuditLog log = new VendorAuditLog();
        log.setVendorId(vendor.getId());
        log.setUserEmail(vendor.getUserEmail());
        log.setAcao("WEBHOOK_CAKTO_" + normalizedStatus.toUpperCase(Locale.ROOT));
        log.setEntityType("Vendor");
        log.setEntidadeId(vendor.getId() != null ? vendor.getId() : UUID.randomUUID());
        log.setDetalhes("eventId=" + payload.getId() + ", type=" + payload.getType());
        vendorAuditLogRepository.save(log);
    }

    private void ensureUserAccount(String email, PaymentCheckoutRequest pendingCheckout) {
        if (email == null || email.isBlank()) {
            return;
        }

        if (userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)) {
            return;
        }

        Role userRole = roleRepository
                .findByNameIgnoreCase("ROLE_USER")
                .orElse(null);

        if (userRole == null) {
            return;
        }

        String displayName = pendingCheckout != null && pendingCheckout.getNomeVendedor() != null
                ? pendingCheckout.getNomeVendedor()
            : localPart(email);

        String temporaryPassword = "tmp-" + UUID.randomUUID();

        User user = new User(
                displayName,
                email,
                passwordEncoder.encode(temporaryPassword),
                userRole
        );

        userRepository.save(user);
    }

    private Vendor createVendorFromPayment(String email, PaymentCheckoutRequest pendingCheckout) {

        Vendor vendor = new Vendor();
        vendor.setUserEmail(email);

        String nomeVendedor = pendingCheckout != null && pendingCheckout.getNomeVendedor() != null
                ? pendingCheckout.getNomeVendedor()
            : localPart(email);

        vendor.setNomeVendedor(nomeVendedor);

        String whatsapp = pendingCheckout != null ? pendingCheckout.getWhatsappVendedor() : null;
        vendor.setWhatsappVendedor(whatsapp != null && !whatsapp.isBlank() ? whatsapp : "0000000000");

        if (pendingCheckout != null) {
            vendor.setNomeEmpresa(pendingCheckout.getNomeEmpresa());
        }

        String baseSlug = pendingCheckout != null && pendingCheckout.getSlug() != null
                ? pendingCheckout.getSlug()
                : slugFromEmail(email);

        vendor.setSlug(resolveUniqueSlug(baseSlug));

        return vendorRepository.save(vendor);
    }

    private String resolveUniqueSlug(String baseSlug) {
        String normalized = normalizeSlug(baseSlug);
        if (normalized.isBlank()) {
            normalized = "vendor";
        }

        if (vendorRepository.findBySlug(normalized).isEmpty()) {
            return normalized;
        }

        return normalized + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String slugFromEmail(String email) {
        String prefix = localPart(email);
        return normalizeSlug(prefix);
    }

    private String localPart(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return "vendor";
        }
        return email.substring(0, email.indexOf('@'));
    }

    private String normalizeSlug(String raw) {
        if (raw == null) {
            return "";
        }

        return NON_ALNUM.matcher(raw.toLowerCase(Locale.ROOT))
                .replaceAll("-")
                .replaceAll("^-+|-+$", "");
    }
}
