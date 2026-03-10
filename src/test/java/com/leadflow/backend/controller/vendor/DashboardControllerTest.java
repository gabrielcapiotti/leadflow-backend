package com.leadflow.backend.controller.vendor;

import com.leadflow.backend.config.TestBillingConfig;
import com.leadflow.backend.controller.DashboardController;
import com.leadflow.backend.dto.vendor.DashboardResponse;
import com.leadflow.backend.entities.vendor.LeadStage;
import com.leadflow.backend.entities.vendor.VendorLead;
import com.leadflow.backend.multitenancy.service.TenantService;
import com.leadflow.backend.security.RateLimitInterceptor;
import com.leadflow.backend.security.RateLimitService;
import com.leadflow.backend.security.SubscriptionGuard;
import com.leadflow.backend.service.vendor.DashboardService;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(TestBillingConfig.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private SubscriptionGuard subscriptionGuard;

    @MockBean
    private RateLimitInterceptor rateLimitInterceptor;

    @MockBean
    private RateLimitService rateLimitService;

    @Test
    void getDashboard_ShouldReturnDashboardResponse() throws Exception {

        UUID vendorId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();

        VendorLead lead = new VendorLead();
        ReflectionTestUtils.setField(lead, "id", leadId);
        lead.setVendorId(vendorId);
        lead.setNomeCompleto("Lead Top");
        lead.setWhatsapp("11999999999");
        lead.setStage(LeadStage.PROPOSTA);
        lead.setScore(92);

        DashboardResponse response = new DashboardResponse(
                List.of(lead),
                Map.of("PROPOSTA", 4L, "NOVO", 2L),
                Map.of("NOVO→CONTATO", 50.0),
                Map.of("NOVO", 12.0),
                3L,
                48L
        );

        when(dashboardService.getDashboardForCurrentVendor()).thenReturn(response);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rankingTop5[0].id").value(leadId.toString()))
                .andExpect(jsonPath("$.rankingTop5[0].nomeCompleto").value("Lead Top"))
                .andExpect(jsonPath("$.leadsPorStage.PROPOSTA").value(4))
                .andExpect(jsonPath("$.taxaConversao['NOVO→CONTATO']").value(50.0))
                .andExpect(jsonPath("$.tempoMedioPorStage.NOVO").value(12.0))
                .andExpect(jsonPath("$.leadsQuentes").value(3))
                .andExpect(jsonPath("$.totalLeads").value(48));
    }
}
