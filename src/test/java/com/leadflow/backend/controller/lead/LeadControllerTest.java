package com.leadflow.backend.controller.lead;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.dto.lead.CreateLeadRequest;
import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.exception.GlobalExceptionHandler;
import com.leadflow.backend.security.jwt.JwtService;
import com.leadflow.backend.service.lead.LeadService;
import com.leadflow.backend.service.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = LeadController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test") // Use o profile 'test' para padronizar os testes
@Import(GlobalExceptionHandler.class)
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
    private JwtService jwtService;

    private Lead lead;
    private User user;
    private UUID leadId;

    private static final String TENANT = "tenant_test";

    /* ====================================================== */
    /* SETUP / TEARDOWN                                       */
    /* ====================================================== */

    @BeforeEach
    void setUp() {

        Role role = new Role("ROLE_USER");

        user = new User(
                "Test User",
                "test@example.com",
                "password",
                role
        );

        UUID userId = UUID.randomUUID();
        ReflectionTestUtils.setField(user, "id", userId);

        leadId = UUID.randomUUID();

        lead = new Lead(
                userId,
                "Test Lead",
                "lead@example.com",
                "123456789"
        );

        ReflectionTestUtils.setField(lead, "id", leadId);
        ReflectionTestUtils.setField(lead, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(lead, "updatedAt", LocalDateTime.now());

        when(userService.getActiveByEmail("test@example.com"))
                .thenReturn(user);
    }

    @AfterEach
    void tearDown() {
    }

    /* ====================================================== */
    /* CREATE                                                 */
    /* ====================================================== */

    @Test
    @WithMockUser(username = "test@example.com")
    void createLead_ShouldReturnCreatedLead() throws Exception {

        // Arrange
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

        // Act & Assert
        mockMvc.perform(post("/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(leadId.toString()))
                .andExpect(jsonPath("$.name").value("Test Lead"))
                .andExpect(jsonPath("$.status").value("NEW"));

        verify(leadService).createLead(
                eq("Test Lead"),
                eq("lead@example.com"),
                eq("123456789"),
                eq(user)
        );
    }

    /* ====================================================== */
    /* UPDATE STATUS                                          */
    /* ====================================================== */

    @Test
    @WithMockUser(username = "test@example.com")
    void updateLeadStatus_ShouldReturnUpdatedLead() throws Exception {

        // Arrange
        lead.changeStatus(LeadStatus.CONTACTED);

        when(leadService.updateStatus(
                eq(leadId),
                eq(LeadStatus.CONTACTED),
                eq(user)
        )).thenReturn(lead);

        // Act & Assert
        mockMvc.perform(patch("/api/leads/{id}/status", leadId)
                        .param("status", "CONTACTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONTACTED"));

        verify(leadService)
                .updateStatus(leadId, LeadStatus.CONTACTED, user);
    }

    /* ====================================================== */
    /* DELETE                                                 */
    /* ====================================================== */

    @Test
    @WithMockUser(username = "test@example.com")
    void deleteLead_ShouldReturnNoContent() throws Exception {

        // Act & Assert
        mockMvc.perform(delete("/api/leads/{id}", leadId))
                .andExpect(status().isNoContent());

        verify(leadService).softDelete(leadId, user);
    }
}