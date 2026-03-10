package com.leadflow.backend.controller.vendor;

import com.leadflow.backend.controller.UsageController;
import com.leadflow.backend.dto.vendor.UsageResponse;
import com.leadflow.backend.entities.UsageLimit;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.multitenancy.service.TenantService;
import com.leadflow.backend.repository.VendorRepository;
import org.springframework.context.annotation.FilterType;
import com.leadflow.backend.security.RateLimitInterceptor;
import com.leadflow.backend.security.RateLimitService;
import com.leadflow.backend.security.SubscriptionGuard;
import com.leadflow.backend.security.VendorContext;
import com.leadflow.backend.service.vendor.QuotaService;
import com.leadflow.backend.service.vendor.UsageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.leadflow.backend.config.TestBillingConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = UsageController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = RateLimitInterceptor.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(TestBillingConfig.class)
class UsageControllerTest {

    @Autowired
    private MockMvc mockMvc;

        @MockBean
    private QuotaService quotaService;

        @MockBean
    private VendorRepository vendorRepository;

        @MockBean
    private TenantService tenantService;

        @MockBean
    private SubscriptionGuard subscriptionGuard;

        @MockBean
    private RateLimitInterceptor rateLimitInterceptor;

        @MockBean
    private RateLimitService rateLimitService;

        @MockBean
    private UsageService usageService;

        @MockBean
    private VendorContext vendorContext;

    @Test
    @WithMockUser(username = "vendor@test.com")
    void getUsage_ShouldReturnUsageResponse() throws Exception {

        UUID vendorId = UUID.randomUUID();
        Instant periodEnd = Instant.parse("2026-04-02T00:00:00Z");

        Vendor vendor = new Vendor();
        ReflectionTestUtils.setField(vendor, "id", vendorId);
        vendor.setUserEmail("vendor@test.com");

        UsageResponse response = new UsageResponse(
                new UsageResponse.ResourceUsage(120, 500),
                new UsageResponse.ResourceUsage(310, 700),
                periodEnd
        );

        when(vendorRepository.findByUserEmail("vendor@test.com"))
                .thenReturn(List.of(vendor));

        when(quotaService.getUsage(vendorId)).thenReturn(response);

        mockMvc.perform(get("/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active_leads.used").value(120))
                .andExpect(jsonPath("$.active_leads.limit").value(500))
                .andExpect(jsonPath("$.active_leads.percentage").value(24))
                .andExpect(jsonPath("$.ai_executions.used").value(310))
                .andExpect(jsonPath("$.ai_executions.limit").value(700))
                .andExpect(jsonPath("$.ai_executions.percentage").value(44))
                .andExpect(jsonPath("$.period_end").value("2026-04-02T00:00:00Z"));
    }

    @Test
    @WithMockUser(username = "vendor@test.com")
    void getUsageLimits_ShouldReturnUsageLimit() throws Exception {
        UUID vendorId = UUID.randomUUID();

        UsageLimit usage = new UsageLimit();
        usage.setTenantId(vendorId);
        usage.setLeadsUsed(120);
        usage.setUsersUsed(8);
        usage.setAiExecutionsUsed(300);

        when(vendorContext.getCurrentVendorId()).thenReturn(vendorId);
        when(usageService.getUsage(vendorId)).thenReturn(usage);

        mockMvc.perform(get("/usage/limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(vendorId.toString()))
                .andExpect(jsonPath("$.leadsUsed").value(120))
                .andExpect(jsonPath("$.usersUsed").value(8))
                .andExpect(jsonPath("$.aiExecutionsUsed").value(300));
    }
}
