package com.leadflow.backend.controller.admin;

import com.leadflow.backend.dto.admin.*;
import com.leadflow.backend.multitenancy.service.TenantService;
import com.leadflow.backend.repository.SubscriptionHistoryRepository;
import com.leadflow.backend.repository.VendorLeadRepository;
import com.leadflow.backend.repository.VendorRepository;
import com.leadflow.backend.repository.VendorRiskAlertRepository;
import com.leadflow.backend.repository.VendorUsageRepository;
import com.leadflow.backend.repository.lead.LeadRepository;
import com.leadflow.backend.security.RateLimitService;
import com.leadflow.backend.security.TestSecurityConfig;
import com.leadflow.backend.security.VendorContext;
import com.leadflow.backend.security.jwt.JwtService;
import com.leadflow.backend.service.admin.AdminService;
import com.leadflow.backend.service.ai.AiService;
import com.leadflow.backend.service.audit.AuditService;
import com.leadflow.backend.service.lead.LeadService;
import com.leadflow.backend.service.monitoring.MetricsService;
import com.leadflow.backend.service.notification.SendGridEmailService;
import com.leadflow.backend.service.user.UserService;
import com.leadflow.backend.service.vendor.VendorService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(TestSecurityConfig.class)
class AdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TenantService tenantService;

    @MockBean
    private RateLimitService rateLimitService;

    @MockBean
    private VendorContext vendorContext;

    @MockBean
    private MetricsService metricsService;

    @MockBean
    private SendGridEmailService sendGridEmailService;

    @MockBean
    private AiService aiService;

    @MockBean
    private AuditService auditService;

    @MockBean
    private VendorService vendorService;

    @MockBean
    private LeadService leadService;

    @MockBean
    private UserService userService;

    @MockBean
    private VendorRepository vendorRepository;

    @MockBean
    private LeadRepository leadRepository;

    @MockBean
    private VendorUsageRepository vendorUsageRepository;

    @MockBean
    private SubscriptionHistoryRepository subscriptionHistoryRepository;

    @MockBean
    private VendorRiskAlertRepository vendorRiskAlertRepository;

    @MockBean
    private javax.persistence.EntityManagerFactory entityManagerFactory;

    @MockBean
    private VendorLeadRepository vendorLeadRepository;

    /* ======================================================
       OVERVIEW
       ====================================================== */

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {

        mockMvc.perform(get("/admin/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403ForUserRole() throws Exception {

        mockMvc.perform(get("/admin/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn200ForAdminRole() throws Exception {

        when(adminService.getOverview()).thenReturn(
                new AdminOverviewResponse(
                        42,35,4,2,1,
                        8123,21450,
                        BigDecimal.valueOf(6895),
                        BigDecimal.valueOf(6895),
                        0.2,0.5,
                        BigDecimal.valueOf(197),
                        0.2,
                        BigDecimal.valueOf(985)
                )
        );

        mockMvc.perform(get("/admin/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_vendors").value(42));
    }

    /* ======================================================
       GROWTH
       ====================================================== */

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn200ForGrowthWithAdminRole() throws Exception {

        when(adminService.getGrowth(30)).thenReturn(
                new GrowthResponse(
                        List.of(new GrowthPoint(LocalDate.of(2026,3,1),2L)),
                        List.of(new GrowthPoint(LocalDate.of(2026,3,1),394L)),
                        List.of(new GrowthPoint(LocalDate.of(2026,3,1),12L)),
                        List.of(new GrowthPoint(LocalDate.of(2026,3,1),55L))
                )
        );

        mockMvc.perform(
                get("/admin/metrics/growth").param("days","30")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendors[0].value").value(2));
    }

    /* ======================================================
       COHORTS
       ====================================================== */

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn200ForCohortsWithAdminRole() throws Exception {

        when(adminService.calculateCohorts()).thenReturn(
                List.of(
                        new CohortResponse(
                                "2026-01",
                                Map.of(0,100.0,1,85.0,2,72.0)
                        )
                )
        );

        mockMvc.perform(get("/admin/metrics/cohorts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cohort").value("2026-01"));
    }

    /* ======================================================
       FORECAST
       ====================================================== */

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn200ForForecastWithAdminRole() throws Exception {

        when(adminService.forecastMRR(6)).thenReturn(
                List.of(
                        new ForecastPoint("2026-04",8200.0),
                        new ForecastPoint("2026-05",8900.0)
                )
        );

        mockMvc.perform(
                get("/admin/metrics/forecast").param("months","6")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value("2026-04"));
    }

    /* ======================================================
       HEALTH
       ====================================================== */

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn200ForHealthWithAdminRole() throws Exception {

        UUID vendorId = UUID.randomUUID();

        when(adminService.calculateHealth(vendorId))
                .thenReturn(new VendorHealthResponse(vendorId,78,"LOW"));

        mockMvc.perform(
                get("/admin/metrics/health/{vendorId}",vendorId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(78));
    }
}