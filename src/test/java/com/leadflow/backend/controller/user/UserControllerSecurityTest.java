package com.leadflow.backend.controller.user;

import com.leadflow.backend.security.jwt.JwtService;
import com.leadflow.backend.security.RateLimitInterceptor;
import com.leadflow.backend.security.RateLimitService;
import com.leadflow.backend.security.TestSecurityConfig;
import com.leadflow.backend.service.user.UserService;
import com.leadflow.backend.multitenancy.filter.TenantFilter;
import com.leadflow.backend.multitenancy.service.TenantService;
import com.leadflow.backend.repository.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = UserController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = TenantFilter.class
    )
)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TenantService tenantService;

    @MockBean
    private RateLimitInterceptor rateLimitInterceptor;

    @MockBean
    private RateLimitService rateLimitService;

    @BeforeEach
    void setup() {
        // Corrige inconsistência de tenant
        when(tenantService.resolveSchemaByTenantIdentifier(any()))
                .thenReturn(Optional.of("tenant_id_123"));

        // Página vazia simulada
        when(userService.listActiveUsers(any(Pageable.class)))
            .thenReturn(new PageImpl<>(Objects.requireNonNull(Collections.<com.leadflow.backend.entities.user.User>emptyList())));

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