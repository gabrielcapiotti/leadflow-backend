package com.leadflow.backend.controller.lead;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.config.TestSecurityConfig;
import com.leadflow.backend.dto.lead.CreateLeadRequest;
import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.exception.GlobalExceptionHandler;
import com.leadflow.backend.multitenancy.resolver.JwtTenantResolver;
import com.leadflow.backend.multitenancy.service.TenantService;
import com.leadflow.backend.security.jwt.JwtService;
import com.leadflow.backend.service.lead.LeadService;
import com.leadflow.backend.service.user.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = LeadController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class LeadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /* ================================
       MOCKS NECESSÁRIOS PARA CONTEXTO
       ================================ */

    @MockBean
    private LeadService leadService;

    // filtros dependem disso
    @MockBean
    private JwtService jwtService;

    @MockBean
    private TenantService tenantService;

    @MockBean
    private JwtTenantResolver jwtTenantResolver;

    @MockBean
    private UserService userService;

    private Lead lead;
    private User user;
    private UUID leadId;

    @BeforeEach
    void setUp() {

        Tenant tenant = new Tenant("Test Tenant", "test_schema");
        Role role = new Role("USER");

        user = new User(
                "Test User",
                "test@example.com",
                "password",
                role,
                tenant
        );

        lead = new Lead("Test Lead", "lead@example.com", "123456789");

        leadId = UUID.randomUUID();

        ReflectionTestUtils.setField(lead, "id", leadId);
        ReflectionTestUtils.setField(lead, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(lead, "updatedAt", LocalDateTime.now());

        lead.setUser(user);

        when(userService.getActiveByEmail("test@example.com"))
                .thenReturn(user);

        when(tenantService.resolveSchemaByTenantId("test_schema"))
                .thenReturn("test_schema");

        when(leadService.createLead(
                anyString(),
                anyString(),
                anyString(),
                eq(user)
        )).thenReturn(lead);
    }

    private RequestPostProcessor tenant() {
        return request -> {
            request.addHeader("X-Tenant-ID", "test_schema");
            return request;
        };
    }

    /* ======================================================
       AUTHENTICATION
       ====================================================== */

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/leads").with(tenant()))
                .andExpect(status().isUnauthorized());
    }

    /* ======================================================
       CREATE
       ====================================================== */

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void createLead_ShouldReturnCreatedLead() throws Exception {

        when(tenantService.resolveSchemaByTenantId("test_schema"))
                .thenReturn("test_schema");

        when(leadService.createLead(
                anyString(),
                anyString(),
                anyString(),
                eq(user)
        )).thenReturn(lead);

        CreateLeadRequest request = new CreateLeadRequest(
                "Test Lead",
                "lead@example.com",
                "123456789"
        );

        mockMvc.perform(post("/api/leads")
                        .with(tenant())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(leadId.toString()))
                .andExpect(jsonPath("$.name").value("Test Lead"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    /* ======================================================
       LIST
       ====================================================== */

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void listActiveLeads_ShouldReturnLeadList() throws Exception {
        mockMvc.perform(get("/api/leads").with(tenant()))
                .andExpect(status().isOk());
    }

    /* ======================================================
       UPDATE STATUS
       ====================================================== */

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void updateLeadStatus_ShouldReturnUpdatedLead() throws Exception {

        lead.changeStatus(LeadStatus.CONTACTED);

        when(leadService.updateStatus(
                eq(leadId),
                eq(LeadStatus.CONTACTED),
                eq(user)
        )).thenReturn(lead);

        mockMvc.perform(patch("/api/leads/{id}/status", leadId)
                .with(tenant())
                .param("status", "CONTACTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(leadId.toString()))
                .andExpect(jsonPath("$.status").value("CONTACTED"));
    }

    /* ======================================================
       DELETE
       ====================================================== */

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void deleteLead_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/leads/{id}", leadId)
                .header("X-Tenant-ID", "test_schema"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void getLeads_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/leads")
                .header("X-Tenant-ID", "test_schema"))
                .andExpect(status().isOk());
    }

    // Mock TenantService to resolve schema correctly
    @BeforeEach
    void mockTenantService() {
        when(tenantService.resolveSchemaByTenantId("test_schema"))
                .thenReturn("test_schema");
    }
}
