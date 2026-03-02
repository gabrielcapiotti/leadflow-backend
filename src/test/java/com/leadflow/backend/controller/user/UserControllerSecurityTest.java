package com.leadflow.backend.controller.user;

import com.leadflow.backend.security.jwt.JwtService;
import com.leadflow.backend.service.user.UserService;
import com.leadflow.backend.multitenancy.service.TenantService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TenantService tenantService;

    @BeforeEach
    void setup() {

        // Corrige inconsistência de tenant
        when(tenantService.resolveSchemaByTenantIdentifier(any()))
                .thenReturn(Optional.of("tenant_id_123"));

        // Página vazia simulada
        when(userService.listActiveUsers(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        // JWT válido por padrão
        when(jwtService.isValid(any())).thenReturn(true);
    }

    /* ==========================
       USER ROLE → 403
       ========================== */

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403ForUserRole() throws Exception {

        mockMvc.perform(get("/api/users")
                        .header("X-Tenant-ID", "tenant_id_123"))
                .andExpect(status().isForbidden());
    }

    /* ==========================
       ADMIN ROLE → 200
       ========================== */

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn200ForAdminRole() throws Exception {

        mockMvc.perform(get("/api/users")
                        .header("X-Tenant-ID", "tenant_id_123"))
                .andExpect(status().isOk());
    }

    /* ==========================
       NOT AUTHENTICATED → 401
       ========================== */

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {

        mockMvc.perform(get("/api/users")
                        .header("X-Tenant-ID", "tenant_id_123"))
                .andExpect(status().isUnauthorized());
    }

    /* ==========================
       INVALID JWT → 401
       ========================== */

    @Test
    void shouldReturn401ForInvalidJwt() throws Exception {

        when(jwtService.isValid(any())).thenReturn(false);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer invalid.jwt.token")
                        .header("X-Tenant-ID", "tenant_id_123"))
                .andExpect(status().isUnauthorized());
    }
}