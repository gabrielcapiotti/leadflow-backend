package com.leadflow.backend.service.billing;

import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.multitenancy.provisioning.TenantSchemaProvisioner;
import com.leadflow.backend.service.user.UserService;
import com.leadflow.backend.service.vendor.VendorService;
import com.stripe.model.checkout.Session;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class BillingTenantProvisioningService {

    private final VendorService vendorService;
    private final UserService userService;
    private final TenantSchemaProvisioner schemaProvisioner;

    public BillingTenantProvisioningService(
            VendorService vendorService,
            UserService userService,
            TenantSchemaProvisioner schemaProvisioner
    ) {
        this.vendorService = Objects.requireNonNull(vendorService);
        this.userService = Objects.requireNonNull(userService);
        this.schemaProvisioner = Objects.requireNonNull(schemaProvisioner);
    }

    /* ======================================================
       STRIPE CHECKOUT FLOW
       ====================================================== */

    @Transactional
    public Vendor provisionFromCheckout(Session session) {

        if (session == null) {
            throw new IllegalArgumentException("Stripe session cannot be null");
        }

        String email = session.getCustomerEmail();

        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Stripe session missing customer email");
        }

        return provisionInternal(email);
    }

    /* ======================================================
       INTERNAL PROVISIONING
       ====================================================== */

    @Transactional
    public Vendor provision(String tenantIdentifier) {

        if (tenantIdentifier == null || tenantIdentifier.isBlank()) {
            throw new IllegalArgumentException("Tenant identifier cannot be empty");
        }

        return provisionInternal(tenantIdentifier);
    }

    /* ======================================================
       CORE PROVISIONING LOGIC
       ====================================================== */

    private Vendor provisionInternal(String identifier) {

        Vendor vendor = vendorService.createVendor(identifier);

        String schema =
                schemaProvisioner.provisionTenantSchema(vendor.getId());

        vendorService.assignSchema(vendor, schema);

        userService.createAdminUser(vendor, identifier);

        return vendor;
    }
}