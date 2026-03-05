package com.leadflow.backend.service.billing;

import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.multitenancy.provisioning.TenantSchemaProvisioner;
import com.leadflow.backend.service.vendor.VendorService;
import com.leadflow.backend.service.user.UserService;
import com.stripe.model.checkout.Session;
import org.springframework.stereotype.Service;

@Service
public class TenantProvisioningService {

    private final VendorService vendorService;
    private final UserService userService;
    private final TenantSchemaProvisioner schemaProvisioner;

    public TenantProvisioningService(
            VendorService vendorService,
            UserService userService,
            TenantSchemaProvisioner schemaProvisioner
    ) {
        this.vendorService = vendorService;
        this.userService = userService;
        this.schemaProvisioner = schemaProvisioner;
    }

    public void provisionTenant(Session session) {

        String email = session.getCustomerEmail();

        Vendor vendor = vendorService.createVendor(email);

        String schema = schemaProvisioner
                .provisionTenantSchema(vendor.getId());

        vendorService.assignSchema(vendor, schema);

        userService.createAdminUser(vendor, email);

    }
}