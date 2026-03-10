package com.leadflow.backend.service.vendor;

import com.leadflow.backend.entities.vendor.VendorFeature;
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
                .map(VendorFeature::isEnabled)
                .orElse(false);
    }

    public void upsertFeature(UUID vendorId, VendorFeatureKey featureKey, boolean enabled) {
        VendorFeature feature = vendorFeatureRepository
                .findByVendorIdAndFeatureKey(vendorId, featureKey)
                .orElseGet(() -> {
                    VendorFeature created = new VendorFeature();
                    created.setVendorId(vendorId);
                    created.setFeatureKey(featureKey);
                    return created;
                });

        feature.setEnabled(enabled);
        vendorFeatureRepository.save(feature);
    }
}
