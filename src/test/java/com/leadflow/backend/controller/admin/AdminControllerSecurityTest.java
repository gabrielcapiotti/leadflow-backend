package com.leadflow.backend.controller.admin;

import com.leadflow.backend.dto.admin.AdminOverviewResponse;
import com.leadflow.backend.dto.admin.CohortResponse;
import com.leadflow.backend.dto.admin.ForecastPoint;
import com.leadflow.backend.dto.admin.GrowthPoint;
import com.leadflow.backend.dto.admin.GrowthResponse;
import com.leadflow.backend.dto.admin.VendorHealthResponse;
import com.leadflow.backend.multitenancy.filter.TenantFilter;
import com.leadflow.backend.multitenancy.service.TenantService;
import com.leadflow.backend.security.RateLimitService;
import com.leadflow.backend.security.TestSecurityConfig;
import com.leadflow.backend.security.jwt.JwtService;
import com.leadflow.backend.service.admin.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@WebMvcTest(
        controllers = AdminController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TenantFilter.class
        )
)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class AdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

        @MockitoBean
    private AdminService adminService;

                @MockitoBean
        private JwtService jwtService;

                @MockitoBean
        private TenantService tenantService;

                @MockitoBean
        private RateLimitService rateLimitService;

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
                        42,
                        35,
                        4,
                        2,
                        1,
                        8123,
                        21450,
                        6895,
                        6895,
                        0.2,
                        0.5,
                        197,
                        0.2,
                        985
                )
        );

        mockMvc.perform(get("/admin/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_vendors").value(42))
                .andExpect(jsonPath("$.active_subscriptions").value(35))
                .andExpect(jsonPath("$.trial_subscriptions").value(4))
                .andExpect(jsonPath("$.inadimplentes").value(2))
                .andExpect(jsonPath("$.expiradas").value(1))
                .andExpect(jsonPath("$.total_leads").value(8123))
                .andExpect(jsonPath("$.total_ai_executions_current_cycle").value(21450))
                .andExpect(jsonPath("$.estimated_monthly_revenue").value(6895))
                .andExpect(jsonPath("$.mrr_real").value(6895))
                .andExpect(jsonPath("$.churn_rate_30d").value(0.2))
                .andExpect(jsonPath("$.trial_to_paid_conversion_30d").value(0.5))
                .andExpect(jsonPath("$.arpu").value(197))
                .andExpect(jsonPath("$.churn_rate").value(0.2))
                .andExpect(jsonPath("$.ltv").value(985));
    }

    @Test
    void shouldReturn401ForGrowthWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/admin/metrics/growth"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403ForGrowthWithUserRole() throws Exception {
        mockMvc.perform(get("/admin/metrics/growth"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn200ForGrowthWithAdminRole() throws Exception {

        when(adminService.getGrowth(30)).thenReturn(
                new GrowthResponse(
                        List.of(new GrowthPoint(LocalDate.of(2026, 3, 1), 2L)),
                        List.of(new GrowthPoint(LocalDate.of(2026, 3, 1), 394L)),
                        List.of(new GrowthPoint(LocalDate.of(2026, 3, 1), 12L)),
                        List.of(new GrowthPoint(LocalDate.of(2026, 3, 1), 55L))
                )
        );

        mockMvc.perform(get("/admin/metrics/growth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendors[0].date").value("2026-03-01"))
                .andExpect(jsonPath("$.vendors[0].value").value(2))
                .andExpect(jsonPath("$.revenue[0].value").value(394))
                .andExpect(jsonPath("$.leads[0].value").value(12))
                .andExpect(jsonPath("$.ai_executions[0].value").value(55));
    }

    @Test
    void shouldReturn401ForCohortsWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/admin/metrics/cohorts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403ForCohortsWithUserRole() throws Exception {
        mockMvc.perform(get("/admin/metrics/cohorts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn200ForCohortsWithAdminRole() throws Exception {

        when(adminService.calculateCohorts()).thenReturn(
                List.of(
                        new CohortResponse("2026-01", Map.of(0, 100.0, 1, 85.0, 2, 72.0))
                )
        );

        mockMvc.perform(get("/admin/metrics/cohorts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cohort").value("2026-01"))
                .andExpect(jsonPath("$[0].retention.0").value(100.0))
                .andExpect(jsonPath("$[0].retention.1").value(85.0));
    }

    @Test
    void shouldReturn401ForForecastWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/admin/metrics/forecast"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403ForForecastWithUserRole() throws Exception {
        mockMvc.perform(get("/admin/metrics/forecast"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn200ForForecastWithAdminRole() throws Exception {

        when(adminService.forecastMRR(6)).thenReturn(
                List.of(
                        new ForecastPoint("2026-04", 8200.0),
                        new ForecastPoint("2026-05", 8900.0)
                )
        );

        mockMvc.perform(get("/admin/metrics/forecast"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value("2026-04"))
                .andExpect(jsonPath("$[0].projected_mrr").value(8200.0))
                .andExpect(jsonPath("$[1].month").value("2026-05"));
    }

        @Test
        void shouldReturn401ForHealthWhenNotAuthenticated() throws Exception {
                UUID vendorId = UUID.randomUUID();

                mockMvc.perform(get("/admin/metrics/health/{vendorId}", vendorId))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        void shouldReturn403ForHealthWithUserRole() throws Exception {
                UUID vendorId = UUID.randomUUID();

                mockMvc.perform(get("/admin/metrics/health/{vendorId}", vendorId))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldReturn200ForHealthWithAdminRole() throws Exception {
                UUID vendorId = UUID.randomUUID();

                when(adminService.calculateHealth(vendorId))
                                .thenReturn(new VendorHealthResponse(vendorId, 78, "LOW"));

                mockMvc.perform(get("/admin/metrics/health/{vendorId}", vendorId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.vendorId").value(vendorId.toString()))
                                .andExpect(jsonPath("$.score").value(78))
                                .andExpect(jsonPath("$.riskLevel").value("LOW"));
        }
}
