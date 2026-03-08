package com.leadflow.backend.service.admin;

import com.leadflow.backend.dto.admin.AdminOverviewResponse;
import com.leadflow.backend.dto.admin.CohortResponse;
import com.leadflow.backend.dto.admin.ForecastPoint;
import com.leadflow.backend.dto.admin.GrowthResponse;
import com.leadflow.backend.dto.admin.VendorHealthResponse;
import com.leadflow.backend.entities.vendor.QuotaType;
import com.leadflow.backend.entities.vendor.SubscriptionStatus;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.entities.vendor.VendorRiskAlert;
import com.leadflow.backend.repository.SubscriptionHistoryRepository;
import com.leadflow.backend.repository.VendorLeadRepository;
import com.leadflow.backend.repository.VendorRiskAlertRepository;
import com.leadflow.backend.repository.VendorRepository;
import com.leadflow.backend.repository.VendorUsageRepository;
import com.leadflow.backend.service.notification.SendGridEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private VendorLeadRepository leadRepository;

    @Mock
    private VendorUsageRepository usageRepository;

    @Mock
    private SubscriptionHistoryRepository historyRepository;

    @Mock
    private VendorRiskAlertRepository riskAlertRepository;

    @Mock
    private SendGridEmailService emailService;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                vendorRepository,
                leadRepository,
                usageRepository,
                historyRepository,
                riskAlertRepository,
                emailService
        );
    }

    @Test
    void shouldReturnOverviewWithAggregatedMetrics() {
        when(vendorRepository.countAllGlobal()).thenReturn(42L);
        when(vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.ATIVA)).thenReturn(35L);
        when(vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.TRIAL)).thenReturn(4L);
        when(vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.INADIMPLENTE)).thenReturn(2L);
        when(vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.EXPIRADA)).thenReturn(1L);
        when(vendorRepository.countActiveSubscriptionsGlobal()).thenReturn(35L);
        when(leadRepository.countAllGlobal()).thenReturn(8123L);
        when(usageRepository.sumUsedByQuotaTypeGlobal(QuotaType.AI_EXECUTIONS.name())).thenReturn(21450L);
        when(historyRepository.countCancellationsSinceGlobal(any())).thenReturn(7L);
        when(historyRepository.countTrialConversionsSinceGlobal(any())).thenReturn(2L);

        AdminOverviewResponse response = adminService.getOverview();

        assertEquals(42L, response.getTotalVendors());
        assertEquals(35L, response.getActiveSubscriptions());
        assertEquals(4L, response.getTrialSubscriptions());
        assertEquals(2L, response.getOverdueSubscriptions());
        assertEquals(1L, response.getExpiredSubscriptions());
        assertEquals(8123L, response.getTotalLeads());
        assertEquals(21450L, response.getTotalAiExecutionsCurrentCycle());

        assertEquals(6895.0, response.getEstimatedMonthlyRevenue().doubleValue());
        assertEquals(6895.0, response.getMrrReal().doubleValue());

        assertEquals(0.2, response.getChurnRate30d());
        assertEquals(0.5, response.getTrialToPaidConversion30d());

        assertEquals(197.0, response.getArpu().doubleValue());
        assertEquals(0.2, response.getChurnRate());
        assertEquals(985.0, response.getLtv().doubleValue());

        verify(vendorRepository).countAllGlobal();
    }

        @Test
        void shouldReturnGrowthWithDailySeries() {

        when(vendorRepository.countVendorsPerDayGlobal(any())).thenReturn(List.of(
            new Object[]{Date.valueOf(LocalDate.of(2026, 3, 1)), 2L},
            new Object[]{Date.valueOf(LocalDate.of(2026, 3, 2)), 5L}
        ));

        when(leadRepository.countLeadsPerDayGlobal(any())).thenReturn(List.of(
            new Object[]{Date.valueOf(LocalDate.of(2026, 3, 1)), 11L},
            new Object[]{Date.valueOf(LocalDate.of(2026, 3, 2)), 14L}
        ));

        when(usageRepository.sumUsagePerDayGlobal(
            org.mockito.ArgumentMatchers.eq(QuotaType.AI_EXECUTIONS.name()),
            any())
        ).thenReturn(List.of(
            new Object[]{Date.valueOf(LocalDate.of(2026, 3, 1)), 40L},
            new Object[]{Date.valueOf(LocalDate.of(2026, 3, 2)), 55L}
        ));

        GrowthResponse response = adminService.getGrowth(30);

        assertEquals(2, response.getVendors().size());
        assertEquals(LocalDate.of(2026, 3, 1), response.getVendors().get(0).getDate());
        assertEquals(2L, response.getVendors().get(0).getValue());

        assertEquals(394L, response.getRevenue().get(0).getValue());
        assertEquals(985L, response.getRevenue().get(1).getValue());

        assertEquals(11L, response.getLeads().get(0).getValue());
        assertEquals(55L, response.getAi_executions().get(1).getValue());
        }

        @Test
        void shouldReturnCohortRetention() {

        Vendor januaryActive = buildVendor(
            Instant.parse("2026-01-10T00:00:00Z"),
            null,
            SubscriptionStatus.ATIVA
        );

        Vendor januaryExpired = buildVendor(
            Instant.parse("2026-01-20T00:00:00Z"),
            Instant.parse("2026-02-15T00:00:00Z"),
            SubscriptionStatus.ATIVA
        );

        when(vendorRepository.findAllWithSubscriptionStart())
            .thenReturn(List.of(januaryActive, januaryExpired));

        List<CohortResponse> response = adminService.calculateCohorts();

        assertFalse(response.isEmpty());

        CohortResponse january = response.stream()
            .filter(item -> "2026-01".equals(item.getCohort()))
            .findFirst()
            .orElseThrow();

        assertEquals(100.0, january.getRetention().get(0));
        assertEquals(100.0, january.getRetention().get(1));
        assertEquals(50.0, january.getRetention().get(2));
        }

    @Test
    void shouldForecastMRRForMonths() {

        when(historyRepository.countCancellationsSinceGlobal(any())).thenReturn(5L);
        when(historyRepository.countTrialConversionsSinceGlobal(any())).thenReturn(4L);
        when(vendorRepository.countActiveSubscriptionsGlobal()).thenReturn(100L);
        when(vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.ATIVA)).thenReturn(100L);
        when(vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.TRIAL)).thenReturn(20L);

        List<ForecastPoint> forecast = adminService.forecastMRR(3);

        assertEquals(3, forecast.size());
        assertEquals(19503.0, forecast.get(0).getProjected_mrr());
        assertEquals(19306.0, forecast.get(1).getProjected_mrr());
        assertEquals(19109.0, forecast.get(2).getProjected_mrr());
    }

    @Test
    void shouldUseDefaultForecastMonthsWhenInvalid() {

        when(historyRepository.countCancellationsSinceGlobal(any())).thenReturn(5L);
        when(historyRepository.countTrialConversionsSinceGlobal(any())).thenReturn(4L);
        when(vendorRepository.countActiveSubscriptionsGlobal()).thenReturn(100L);
        when(vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.ATIVA)).thenReturn(100L);
        when(vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.TRIAL)).thenReturn(20L);

        List<ForecastPoint> forecast = adminService.forecastMRR(0);

        assertEquals(6, forecast.size());
    }

    @Test
    void shouldCapForecastMonthsAtThirtySix() {

        when(historyRepository.countCancellationsSinceGlobal(any())).thenReturn(5L);
        when(historyRepository.countTrialConversionsSinceGlobal(any())).thenReturn(4L);
        when(vendorRepository.countActiveSubscriptionsGlobal()).thenReturn(100L);
        when(vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.ATIVA)).thenReturn(100L);
        when(vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.TRIAL)).thenReturn(20L);

        List<ForecastPoint> forecast = adminService.forecastMRR(72);

        assertEquals(36, forecast.size());
    }

    @Test
    void shouldCalculateVendorHealth() {

        UUID vendorId = UUID.randomUUID();
        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setSubscriptionStatus(SubscriptionStatus.ATIVA);

        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(usageRepository.sumLast30Days(any(), any(), any())).thenReturn(600L);
        when(leadRepository.countLast30Days(any(), any())).thenReturn(30L);
        when(usageRepository.lastActivity(any())).thenReturn(Instant.now().minus(2, java.time.temporal.ChronoUnit.DAYS));

        VendorHealthResponse response = adminService.calculateHealth(Objects.requireNonNull(vendorId));

        assertEquals(vendorId, response.getVendorId());
        assertEquals("LOW", response.getRiskLevel());
        assertEquals(91, response.getScore());
    }

    @Test
    void shouldCreateAlertAndNotifyWhenVendorIsHighRisk() {

        UUID vendorId = UUID.randomUUID();
        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setSubscriptionStatus(SubscriptionStatus.INADIMPLENTE);
        vendor.setUserEmail("vendor@example.com");

        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(usageRepository.sumLast30Days(any(), any(), any())).thenReturn(10L);
        when(leadRepository.countLast30Days(any(), any())).thenReturn(2L);
        when(usageRepository.lastActivity(any())).thenReturn(Instant.now().minus(20, java.time.temporal.ChronoUnit.DAYS));
        when(riskAlertRepository.existsByVendorIdAndResolvedFalse(vendorId)).thenReturn(false);

        adminService.evaluateRisk(vendorId);

        verify(riskAlertRepository).save(any(VendorRiskAlert.class));
        verify(emailService).sendEmail(
            eq("vendor@example.com"),
            contains("Percebemos pouca atividade na sua conta"),
            anyString()
        );
    }

    @Test
    void shouldNotCreateDuplicateAlertWhenUnresolvedAlertAlreadyExists() {

        UUID vendorId = UUID.randomUUID();
        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setSubscriptionStatus(SubscriptionStatus.INADIMPLENTE);

        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(riskAlertRepository.existsByVendorIdAndResolvedFalse(vendorId)).thenReturn(true);

        adminService.evaluateRisk(vendorId);

        verify(riskAlertRepository, never()).save(any(VendorRiskAlert.class));
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void shouldEvaluateAllVendorsRiskDaily() {

        AdminService spyService = spy(adminService);

        Vendor vendor1 = new Vendor();
        vendor1.setId(UUID.randomUUID());

        Vendor vendor2 = new Vendor();
        vendor2.setId(UUID.randomUUID());

        when(vendorRepository.findAll()).thenReturn(List.of(vendor1, vendor2));
        doNothing().when(spyService).evaluateRisk(argThat((UUID id) -> id != null));

        spyService.evaluateAllVendorsRiskDaily();

        verify(spyService, times(2)).evaluateRisk(any(UUID.class));
    }

        private Vendor buildVendor(Instant startedAt,
                       Instant expiresAt,
                       SubscriptionStatus status) {
        Vendor vendor = new Vendor();
        vendor.setSubscriptionStartedAt(startedAt);
        vendor.setSubscriptionExpiresAt(expiresAt);
        vendor.setSubscriptionStatus(status);
        return vendor;
        }
}
