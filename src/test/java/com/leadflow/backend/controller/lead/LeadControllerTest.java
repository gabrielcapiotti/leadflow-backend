package com.leadflow.backend.controller.lead;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.config.TestSecurityConfig;
import com.leadflow.backend.dto.lead.CreateLeadRequest;
import com.leadflow.backend.entities.enums.LeadStatus;
import com.leadflow.backend.entities.lead.Lead;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.service.lead.LeadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class LeadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LeadService leadService;

    private CreateLeadRequest createLeadRequest;
    private Lead lead;

    @BeforeEach
    void setUp() throws Exception {

        createLeadRequest = new CreateLeadRequest();
        setField(createLeadRequest, "name", "Test Lead");
        setField(createLeadRequest, "email", "lead@example.com");
        setField(createLeadRequest, "phone", "123456789");

        Role role = new Role("USER");
        User user = new User("Test User", "test@example.com", "password", role);
        setField(user, "id", 1L);

        lead = new Lead("Test Lead", "lead@example.com", "123456789");
        setField(lead, "id", 1L);
        setField(lead, "user", user);
        setField(lead, "status", LeadStatus.NEW);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void createLead_ShouldReturnCreatedLead() throws Exception {

        when(leadService.createLead(anyString(), anyString(), anyString(), any(User.class)))
                .thenReturn(lead);

        mockMvc.perform(post("/api/leads")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createLeadRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Lead"))
                .andExpect(jsonPath("$.email").value("lead@example.com"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void listActiveLeads_ShouldReturnLeadList() throws Exception {

        when(leadService.listActiveLeads(any(User.class)))
                .thenReturn(List.of(lead));

        mockMvc.perform(get("/api/leads").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Lead"))
                .andExpect(jsonPath("$[0].status").value("NEW"));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void getLeadById_ShouldReturnLead() throws Exception {

        when(leadService.getByIdForUser(eq(1L), any(User.class)))
                .thenReturn(lead);

        mockMvc.perform(get("/api/leads/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void updateLeadStatus_ShouldReturnUpdatedLead() throws Exception {

        setField(lead, "status", LeadStatus.CONTACTED);

        when(leadService.updateStatus(eq(1L), eq(LeadStatus.CONTACTED), any(User.class)))
                .thenReturn(lead);

        mockMvc.perform(patch("/api/leads/1/status")
                        .with(csrf())
                        .param("status", "CONTACTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CONTACTED"));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void deleteLead_ShouldReturnNoContent() throws Exception {

        mockMvc.perform(delete("/api/leads/1").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
