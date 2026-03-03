package com.leadflow.backend.security;

import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.repository.VendorRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class VendorContext {

    private final VendorRepository vendorRepository;

    public VendorContext(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    public Vendor getCurrentVendor() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return vendorRepository
                .findByUserEmail(email)
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("Usuário não pertence a vendor"));
    }
}
