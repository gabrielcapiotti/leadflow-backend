package com.leadflow.backend.controller.vendor;

import com.leadflow.backend.controller.UsageController;
import com.leadflow.backend.dto.vendor.UsageResponse;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.multitenancy.service.TenantService;
import com.leadflow.backend.repository.VendorRepository;
import com.leadflow.backend.security.RateLimitService;
import com.leadflow.backend.security.SubscriptionGuard;
import com.leadflow.backend.service.vendor.QuotaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UsageController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UsageControllerTest {

    @Autowired
    private MockMvc mockMvc;

        @MockitoBean
    private QuotaService quotaService;

        @MockitoBean
    private VendorRepository vendorRepository;

        @MockitoBean
    private TenantService tenantService;

        @MockitoBean
    private RateLimitService rateLimitService;

        @MockitoBean
    private SubscriptionGuard subscriptionGuard;

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
}
