package com.leadflow.backend.service.vendor;

import com.leadflow.backend.entities.vendor.VendorFeature;
import com.leadflow.backend.entities.vendor.VendorFeatureKey;
import com.leadflow.backend.repository.VendorFeatureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorFeatureServiceTest {

    @Mock
    private VendorFeatureRepository vendorFeatureRepository;

    private VendorFeatureService vendorFeatureService;

    @BeforeEach
    void setUp() {
        vendorFeatureService = new VendorFeatureService(vendorFeatureRepository);
    }

    @Test
    void shouldReturnFalseWhenFeatureIsMissing() {

        UUID vendorId = UUID.randomUUID();

        when(vendorFeatureRepository.findByVendorIdAndFeatureKey(vendorId, VendorFeatureKey.AI_CHAT))
                .thenReturn(Optional.empty());

        assertFalse(vendorFeatureService.isEnabled(vendorId, VendorFeatureKey.AI_CHAT));
    }

    @Test
    void shouldReturnStoredFeatureValue() {

        UUID vendorId = UUID.randomUUID();

        VendorFeature feature = new VendorFeature();
        feature.setVendorId(vendorId);
        feature.setFeatureKey(VendorFeatureKey.AI_CHAT);
        feature.setEnabled(true);

        when(vendorFeatureRepository.findByVendorIdAndFeatureKey(vendorId, VendorFeatureKey.AI_CHAT))
                .thenReturn(Optional.of(feature));

        assertTrue(vendorFeatureService.isEnabled(vendorId, VendorFeatureKey.AI_CHAT));
    }
}
