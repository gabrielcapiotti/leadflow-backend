package com.leadflow.backend.controller.role;

import com.leadflow.backend.exception.GlobalExceptionHandler;
import com.leadflow.backend.security.TestSecurityConfig;
import com.leadflow.backend.security.jwt.JwtService;
import com.leadflow.backend.service.RoleService;
import com.leadflow.backend.multitenancy.filter.TenantFilter;
import com.leadflow.backend.multitenancy.service.TenantService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static java.util.Collections.emptyList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = RoleController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = TenantFilter.class
    )
)
@ActiveProfiles("test")
@Import({ GlobalExceptionHandler.class, TestSecurityConfig.class })
class RoleControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoleService roleService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TenantService tenantService;

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String TENANT = "public";

    /* ==========================
       NOT AUTHENTICATED
       ========================== */

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {

        mockMvc.perform(get("/api/roles")
                        .header(TENANT_HEADER, TENANT))
                .andExpect(status().isUnauthorized());
    }

    /* ==========================
       USER ROLE
       ========================== */

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403ForUserRole() throws Exception {

        mockMvc.perform(get("/api/roles")
                        .header(TENANT_HEADER, TENANT))
                .andExpect(status().isForbidden());
    }

    /* ==========================
       ADMIN ROLE
       ========================== */

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn200ForAdminRole() throws Exception {

        when(roleService.listAll()).thenReturn(emptyList());

        mockMvc.perform(get("/api/roles")
                        .header(TENANT_HEADER, TENANT))
                .andExpect(status().isOk());
    }
}