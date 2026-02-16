package com.leadflow.backend.controller.role;

import com.leadflow.backend.security.JwtAuthenticationFilter;
import com.leadflow.backend.security.TokenService;
import com.leadflow.backend.security.UserDetailsServiceImpl;
import com.leadflow.backend.service.RoleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static java.util.Collections.emptyList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoleController.class)
class RoleControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoleService roleService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403ForUserRole() throws Exception {
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn200ForAdminRole() throws Exception {

        when(roleService.listAll()).thenReturn(emptyList());

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk());
    }
}
