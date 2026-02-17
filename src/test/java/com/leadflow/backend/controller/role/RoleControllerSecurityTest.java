package com.leadflow.backend.controller.role;

import com.leadflow.backend.config.TestSecurityConfig;
import com.leadflow.backend.exception.GlobalExceptionHandler;
import com.leadflow.backend.service.RoleService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static java.util.Collections.emptyList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoleController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class RoleControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoleService roleService;

    /* ==========================
       NOT AUTHENTICATED
       ========================== */

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isUnauthorized());
    }

    /* ==========================
       USER ROLE
       ========================== */

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403ForUserRole() throws Exception {
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isForbidden());
    }

    /* ==========================
       ADMIN ROLE
       ========================== */

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn200ForAdminRole() throws Exception {

        when(roleService.listAll()).thenReturn(emptyList());

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk());
    }
}
