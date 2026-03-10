package com.leadflow.backend.service.vendor;

import com.leadflow.backend.entities.vendor.SubscriptionStatus;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.repository.VendorRepository;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;

    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    public Vendor createVendor(String email) {
        Vendor vendor = new Vendor();
        vendor.setUserEmail(normalizeEmail(email));
        vendor.setNomeVendedor(localPart(email));
        vendor.setWhatsappVendedor("0000000000");
        vendor.setSlug(generateSlug(email));
        vendor.setSubscriptionStatus(SubscriptionStatus.TRIAL);
        return vendorRepository.save(vendor);
    }

    private String generateSlug(String email) {
        String prefix = localPart(email)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (prefix.isBlank()) {
            prefix = "vendor";
        }

        return prefix + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String localPart(String email) {
        if (email == null || !email.contains("@")) {
            return "vendor";
        }
        return email.substring(0, email.indexOf('@'));
    }

    @Transactional
    public void assignSchema(Vendor vendor, String schema) {
        vendor.setSchemaName(schema);
        vendorRepository.save(vendor);
    }
}
