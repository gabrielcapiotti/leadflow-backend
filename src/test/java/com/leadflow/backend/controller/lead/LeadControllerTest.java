package com.leadflow.backend.controller.lead;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.config.TestSecurityConfig;
import com.leadflow.backend.dto.lead.CreateLeadRequest;
import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.exception.GlobalExceptionHandler;
import com.leadflow.backend.multitenancy.filter.TenantFilter;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeadController.class)
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

    @MockBean
    private TenantFilter tenantFilter;

    @MockBean
    private JwtService jwtService;

    private Lead lead;
    private User user;

    @BeforeEach
    void setUp() {

        Role role = new Role("USER");
        user = new User("Test User", "test@example.com", "password", role);

        lead = new Lead("Test Lead", "lead@example.com", "123456789");
        lead.setUser(user);

        when(userService.getActiveByEmail("test@example.com"))
                .thenReturn(user);
    }

    /* ==========================
       AUTHENTICATION
       ========================== */

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/leads"))
                .andExpect(status().isUnauthorized());
    }

    /* ==========================
       CREATE
       ========================== */

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void createLead_ShouldReturnCreatedLead() throws Exception {

        when(leadService.createLead(anyString(), anyString(), anyString(), any(User.class)))
                .thenReturn(lead);

        CreateLeadRequest request =
                new CreateLeadRequest(
                        "Test Lead",
                        "lead@example.com",
                        "123456789"
                );

        mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Lead"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void createLead_ShouldReturn400WhenBusinessError() throws Exception {

        when(leadService.createLead(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Email already in use"));

        CreateLeadRequest request =
                new CreateLeadRequest(
                        "Test",
                        "duplicate@example.com",
                        "123"
                );

        mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Business Error"));
    }

    /* ==========================
       LIST
       ========================== */

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void listActiveLeads_ShouldReturnLeadList() throws Exception {

        when(leadService.listActiveLeads(any(User.class)))
                .thenReturn(List.of(lead));

        mockMvc.perform(get("/api/leads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Lead"));
    }

    /* ==========================
       UPDATE STATUS
       ========================== */

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void updateLeadStatus_ShouldReturnUpdatedLead() throws Exception {

        lead.changeStatus(LeadStatus.CONTACTED);

        when(leadService.updateStatus(eq(1L), eq(LeadStatus.CONTACTED), any(User.class)))
                .thenReturn(lead);

        mockMvc.perform(patch("/api/leads/1/status")
                        .param("status", "CONTACTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONTACTED"));
    }

    /* ==========================
       DELETE
       ========================== */

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void deleteLead_ShouldReturnNoContent() throws Exception {

        mockMvc.perform(delete("/api/leads/1"))
                .andExpect(status().isNoContent());
    }
}
