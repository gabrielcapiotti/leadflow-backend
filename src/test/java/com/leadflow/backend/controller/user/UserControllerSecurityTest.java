package com.leadflow.backend.controller.user;

import com.leadflow.backend.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService; // Mock necessário

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403ForUserRole() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn200ForAdminRole() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }
}
