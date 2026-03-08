package com.leadflow.backend.service.vendor;

import com.leadflow.backend.entities.vendor.QuotaType;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.entities.vendor.VendorUsage;
import com.leadflow.backend.repository.VendorRepository;
import com.leadflow.backend.repository.VendorUsageRepository;
import com.leadflow.backend.service.notification.SendGridEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotaServiceTest {

    @Mock
    private VendorUsageRepository repository;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private SendGridEmailService emailService;

    private QuotaService quotaService;

    @BeforeEach
    void setUp() {
        quotaService = new QuotaService(repository, vendorRepository, emailService);

        lenient().when(repository.save(any(VendorUsage.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldMark80PercentAlertOnlyOnceInSamePeriod() {

        UUID vendorId = UUID.randomUUID();
        VendorUsage usage = buildUsage(vendorId, QuotaType.ACTIVE_LEADS, 399, Instant.now().plusSeconds(3600));
        when(repository.findByVendorIdAndQuotaType(vendorId, QuotaType.ACTIVE_LEADS)).thenReturn(Optional.of(usage));
        Vendor vendor = buildVendor(vendorId);
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));

        quotaService.increment(vendorId, QuotaType.ACTIVE_LEADS);
        quotaService.increment(vendorId, QuotaType.ACTIVE_LEADS);

        ArgumentCaptor<VendorUsage> captor = ArgumentCaptor.forClass(VendorUsage.class);
        verify(repository, atLeastOnce()).save(captor.capture());

        VendorUsage latestSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(401, latestSaved.getUsed());
        assertTrue(latestSaved.isAlert80Sent());
        assertFalse(latestSaved.isAlert100Sent());
        verify(emailService).sendEmail(eq("vendor@test.com"), contains("80%"), any(String.class));
    }

    @Test
    void shouldMark100PercentAlertWhenLimitIsReached() {

        UUID vendorId = UUID.randomUUID();

        VendorUsage usage = buildUsage(
                vendorId,
                QuotaType.ACTIVE_LEADS,
                499,
                Instant.now().plusSeconds(3600)
        );

        usage.setAlert80Sent(true);

        Vendor vendor = buildVendor(vendorId);

        when(repository.findByVendorIdAndQuotaType(vendorId, QuotaType.ACTIVE_LEADS))
                .thenReturn(Optional.of(usage));

        when(vendorRepository.findById(vendorId))
                .thenReturn(Optional.of(vendor));

        quotaService.increment(vendorId, QuotaType.ACTIVE_LEADS);

        ArgumentCaptor<VendorUsage> captor =
                ArgumentCaptor.forClass(VendorUsage.class);

        verify(repository).save(captor.capture());

        VendorUsage saved = captor.getValue();

        assertEquals(500, saved.getUsed());
        assertTrue(saved.isAlert80Sent());
        assertTrue(saved.isAlert100Sent());

        verify(emailService)
                .sendEmail(eq("vendor@test.com"), contains("Limite"), any(String.class));
    }

    @Test
    void shouldResetAlertsWhenPeriodRenews() {

        UUID vendorId = UUID.randomUUID();

        VendorUsage usage = buildUsage(
                vendorId,
                QuotaType.ACTIVE_LEADS,
                500,
                Instant.now().minusSeconds(30)
        );

        usage.setAlert80Sent(true);
        usage.setAlert100Sent(true);

        when(repository.findByVendorIdAndQuotaType(vendorId, QuotaType.ACTIVE_LEADS))
            .thenReturn(Optional.of(usage));

        when(repository.save(any(VendorUsage.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        quotaService.increment(vendorId, QuotaType.ACTIVE_LEADS);

        ArgumentCaptor<VendorUsage> captor = ArgumentCaptor.forClass(VendorUsage.class);

        verify(repository, atLeastOnce()).save(captor.capture());

        VendorUsage latestSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);

        assertEquals(1, latestSaved.getUsed());
        assertFalse(latestSaved.isAlert80Sent());
        assertFalse(latestSaved.isAlert100Sent());
        assertTrue(latestSaved.getPeriodEnd().isAfter(latestSaved.getPeriodStart()));
    }

    private VendorUsage buildUsage(UUID vendorId, QuotaType type, int used, Instant periodEnd) {

        VendorUsage usage = new VendorUsage();
        usage.setVendorId(vendorId);
        usage.setQuotaType(type);
        usage.setUsed(used);
        usage.setPeriodStart(Instant.now().minusSeconds(60));
        usage.setPeriodEnd(periodEnd);
        usage.setAlert80Sent(false);
        usage.setAlert100Sent(false);

        return usage;
    }

    private Vendor buildVendor(UUID vendorId) {
        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setUserEmail("vendor@test.com");
        return vendor;
    }
}