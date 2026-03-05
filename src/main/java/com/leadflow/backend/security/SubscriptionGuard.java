package com.leadflow.backend.security;

import com.leadflow.backend.entities.vendor.SubscriptionAccessLevel;
import com.leadflow.backend.entities.vendor.SubscriptionStatus;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.repository.VendorRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import com.leadflow.backend.service.vendor.SubscriptionService;

@Component
public class SubscriptionGuard {

    private final VendorRepository vendorRepository;
    private final SubscriptionService subscriptionService;

    public SubscriptionGuard(VendorRepository vendorRepository,
                             SubscriptionService subscriptionService) {
        this.vendorRepository = vendorRepository;
        this.subscriptionService = subscriptionService;
    }

    public SubscriptionAccessLevel resolveAccess() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Authentication required");
        }

        String email = authentication.getName();
        Vendor vendor = vendorRepository
                .findByUserEmail(email)
                .stream()
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Vendor not found"));

        SubscriptionAccessLevel level = subscriptionService.getAccessLevel(vendor);
        if ((level == SubscriptionAccessLevel.FULL || level == SubscriptionAccessLevel.READ_ONLY)
                && vendor.getSubscriptionExpiresAt() != null
                && vendor.getSubscriptionExpiresAt().isBefore(Instant.now())
                && vendor.getSubscriptionStatus() != SubscriptionStatus.INADIMPLENTE) {
            return SubscriptionAccessLevel.BLOCKED;
        }

        return level;
    }

    public boolean isActive() {
        try {
            return resolveAccess() != SubscriptionAccessLevel.BLOCKED;
        } catch (AccessDeniedException ex) {
            return false;
        }
    }

    public void assertActive() {
        if (resolveAccess() == SubscriptionAccessLevel.BLOCKED) {
            throw new AccessDeniedException("Assinatura inativa");
        }
    }

    public void assertFullAccess() {
        if (resolveAccess() != SubscriptionAccessLevel.FULL) {
            throw new AccessDeniedException("Assinatura sem permissão de escrita");
        }
    }
}
