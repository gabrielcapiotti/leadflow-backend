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
import com.leadflow.backend.service.lead.LeadService;
import com.leadflow.backend.service.user.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = LeadController.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class LeadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LeadService leadService;

    @MockBean
    private UserService userService;

    private Lead lead;
    private User user;
    private UUID leadId;

    @BeforeEach
    void setUp() {

        // ===== TENANT =====
        Tenant tenant = new Tenant("Test Tenant", "test_schema");

        // ===== USER =====
        Role role = new Role("USER");
        user = new User(
                "Test User",
                "test@example.com",
                "password",
                role,
                tenant
        );

        // ===== LEAD =====
        lead = new Lead("Test Lead", "lead@example.com", "123456789");

        leadId = UUID.randomUUID();
        ReflectionTestUtils.setField(lead, "id", leadId);
        ReflectionTestUtils.setField(lead, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(lead, "updatedAt", LocalDateTime.now());

        lead.setUser(user);
        lead.setTenant(tenant);

        // Mock do UserService
        when(userService.getActiveByEmail("test@example.com"))
                .thenReturn(user);
    }

    /* ======================================================
       AUTHENTICATION
       ====================================================== */

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/leads"))
                .andExpect(status().isUnauthorized());
    }

    /* ======================================================
       CREATE
       ====================================================== */

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void createLead_ShouldReturnCreatedLead() throws Exception {

        when(leadService.createLead(
                anyString(),
                anyString(),
                anyString(),
                any(User.class)
        )).thenReturn(lead);

        CreateLeadRequest request = new CreateLeadRequest(
                "Test Lead",
                "lead@example.com",
                "123456789"
        );

        mockMvc.perform(post("/api/leads")
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

        when(leadService.listActiveLeads(any(User.class)))
                .thenReturn(List.of(lead));

        mockMvc.perform(get("/api/leads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(leadId.toString()))
                .andExpect(jsonPath("$[0].name").value("Test Lead"))
                .andExpect(jsonPath("$[0].status").value("NEW"));
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
                any(User.class)
        )).thenReturn(lead);

        mockMvc.perform(patch("/api/leads/{id}/status", leadId)
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

        mockMvc.perform(delete("/api/leads/{id}", leadId))
                .andExpect(status().isNoContent());
    }
}
