package com.leadflow.backend.security;

import com.leadflow.backend.entities.vendor.SubscriptionAccessLevel;
import com.leadflow.backend.entities.vendor.SubscriptionStatus;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.repository.VendorRepository;
import com.leadflow.backend.service.vendor.SubscriptionService;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

@Component
public class SubscriptionGuard {

    private final VendorRepository vendorRepository;
    private final SubscriptionService subscriptionService;

    public SubscriptionGuard(VendorRepository vendorRepository,
                             SubscriptionService subscriptionService) {

        this.vendorRepository = Objects.requireNonNull(vendorRepository);
        this.subscriptionService = Objects.requireNonNull(subscriptionService);
    }

    /* ======================================================
       ACCESS RESOLUTION
       ====================================================== */

    public SubscriptionAccessLevel resolveAccess() {

        Vendor vendor = resolveVendor();

        SubscriptionAccessLevel level =
                subscriptionService.getAccessLevel(vendor);

        if (isExpired(vendor) &&
            level != SubscriptionAccessLevel.BLOCKED) {

            return SubscriptionAccessLevel.BLOCKED;
        }

        return level;
    }

    /* ======================================================
       ASSERTIONS
       ====================================================== */

    public boolean isActive() {

        try {
            return resolveAccess() != SubscriptionAccessLevel.BLOCKED;
        } catch (AccessDeniedException ex) {
            return false;
        }
    }

    public void assertActive() {

        if (resolveAccess() == SubscriptionAccessLevel.BLOCKED) {
            throw new AccessDeniedException("Subscription inactive");
        }
    }

    public void assertFullAccess() {

        if (resolveAccess() != SubscriptionAccessLevel.FULL) {
            throw new AccessDeniedException("Subscription does not allow write operations");
        }
    }

    /* ======================================================
       INTERNAL RESOLUTION
       ====================================================== */

    private Vendor resolveVendor() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
            !authentication.isAuthenticated() ||
            authentication instanceof AnonymousAuthenticationToken) {

            throw new AccessDeniedException("Authentication required");
        }

        String email = authentication.getName();

        if (email == null || email.isBlank()) {
            throw new AccessDeniedException("Invalid authentication principal");
        }

        return vendorRepository
                .findByUserEmail(email)
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new AccessDeniedException("Vendor not found for user"));
    }

    private boolean isExpired(Vendor vendor) {

        return vendor.getSubscriptionExpiresAt() != null &&
               vendor.getSubscriptionExpiresAt().isBefore(Instant.now()) &&
               vendor.getSubscriptionStatus() != SubscriptionStatus.INADIMPLENTE;
    }
}
