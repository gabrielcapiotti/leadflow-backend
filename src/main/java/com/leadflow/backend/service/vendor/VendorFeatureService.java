package com.leadflow.backend.service.vendor;

import com.leadflow.backend.entities.vendor.VendorFeatureKey;
import com.leadflow.backend.repository.VendorFeatureRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class VendorFeatureService {

    private final VendorFeatureRepository vendorFeatureRepository;

    public VendorFeatureService(VendorFeatureRepository vendorFeatureRepository) {
        this.vendorFeatureRepository = vendorFeatureRepository;
    }

    public boolean isEnabled(UUID vendorId, VendorFeatureKey featureKey) {
        return vendorFeatureRepository
                .findByVendorIdAndFeatureKey(vendorId, featureKey)
                .map(feature -> feature.isEnabled())
                .orElse(false);
    }
}
