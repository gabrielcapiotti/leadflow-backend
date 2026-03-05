package com.leadflow.backend.repository;

import com.leadflow.backend.entities.vendor.VendorFeature;
import com.leadflow.backend.entities.vendor.VendorFeatureKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VendorFeatureRepository extends JpaRepository<VendorFeature, UUID> {

    Optional<VendorFeature> findByVendorIdAndFeatureKey(UUID vendorId,
                                                        VendorFeatureKey featureKey);
}
