package com.leadflow.backend.service.vendor;

import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.repository.VendorRepository;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;

    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    public Vendor createVendor(String email) {
        Vendor vendor = new Vendor();
        vendor.setUserEmail(email);
        vendor.setNomeVendedor(email);
        vendor.setSlug(generateSlug(email));
        vendor.setStatusAssinatura("ativo");
        return vendor;
    }

    private String generateSlug(String email) {
        return email.split("@")[0] + "-" + java.util.UUID.randomUUID().toString().substring(0, 6);
    }

    @Transactional
    public void assignSchema(Vendor vendor, String schema) {
        vendor.setSchemaName(schema);
        vendorRepository.save(vendor);
    }
}