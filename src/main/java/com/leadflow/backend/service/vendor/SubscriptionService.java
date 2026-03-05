package com.leadflow.backend.service.vendor;

import com.leadflow.backend.entities.vendor.SubscriptionAccessLevel;
import com.leadflow.backend.entities.vendor.SubscriptionStatus;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.repository.VendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SubscriptionService {

    private final VendorRepository vendorRepository;
    private final SubscriptionAuditService auditService;

    public SubscriptionService(VendorRepository vendorRepository,
                               SubscriptionAuditService auditService) {
        this.vendorRepository = vendorRepository;
        this.auditService = auditService;
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
}
