package com.leadflow.backend.controller.admin;

import com.leadflow.backend.entities.vendor.VendorAuditLog;
import com.leadflow.backend.multitenancy.filter.TenantFilter;
import com.leadflow.backend.multitenancy.service.TenantService;
import com.leadflow.backend.repository.VendorAuditLogRepository;
import com.leadflow.backend.repository.audit.SecurityAuditLogRepository;
import com.leadflow.backend.security.RateLimitService;
import com.leadflow.backend.security.TestSecurityConfig;
import com.leadflow.backend.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminAuditController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TenantFilter.class
        )
)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class AdminAuditControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SecurityAuditLogRepository securityAuditLogRepository;

    @MockitoBean
    private VendorAuditLogRepository vendorAuditLogRepository;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/admin/audit/vendor"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403ForUserRole() throws Exception {
        mockMvc.perform(get("/admin/audit/vendor"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn400WhenDateRangeIsInvalid() throws Exception {
        mockMvc.perform(get("/admin/audit/vendor")
                        .param("from", "2026-03-04T15:00:00Z")
                        .param("to", "2026-03-04T10:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid date range: 'from' must be before 'to'"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn200ForAdminRoleAndListVendorAuditLogs() throws Exception {

        UUID vendorId = UUID.randomUUID();
        UUID entidadeId = UUID.randomUUID();

        VendorAuditLog log = new VendorAuditLog();
        log.setVendorId(vendorId);
        log.setUserEmail("admin@leadflow.com");
        log.setAcao("LEAD_OWNER_CHANGED");
        log.setEntityType("VendorLead");
        log.setEntidadeId(entidadeId);
        log.setDetalhes("owner changed");

        when(vendorAuditLogRepository.findAll(org.mockito.ArgumentMatchers.<Specification<VendorAuditLog>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/admin/audit/vendor")
                        .param("vendorId", vendorId.toString())
                        .param("acao", "LEAD_OWNER_CHANGED")
                        .param("entityType", "VendorLead")
                        .param("from", "2026-03-04T00:00:00Z")
                        .param("to", "2026-03-04T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].vendorId").value(vendorId.toString()))
                .andExpect(jsonPath("$.content[0].userEmail").value("admin@leadflow.com"))
                .andExpect(jsonPath("$.content[0].acao").value("LEAD_OWNER_CHANGED"))
                .andExpect(jsonPath("$.content[0].entityType").value("VendorLead"))
                .andExpect(jsonPath("$.content[0].entidadeId").value(entidadeId.toString()))
                .andExpect(jsonPath("$.content[0].detalhes").value("owner changed"));
    }
}
