package com.leadflow.backend.multitenancy;

import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.multitenancy.resolver.JwtTenantResolver;
import com.leadflow.backend.util.TestTenantFactory;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class TenantFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestTenantFactory testTenantFactory;   // ✅ ADICIONADO

    @MockBean
    private JwtTenantResolver jwtTenantResolver;

    @BeforeEach
    void setup() {
        testTenantFactory.createTenant("Tenant A");
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldResolveTenantAndClearContextAfterRequest() throws Exception {

        when(jwtTenantResolver.resolveTenant(any(HttpServletRequest.class)))
                .thenReturn("tenant_a");

        mockMvc.perform(
                get("/auth/login")
        );

        verify(jwtTenantResolver, times(1))
                .resolveTenant(any(HttpServletRequest.class));

        assertThat(TenantContext.getTenant())
                .as("TenantContext deve estar limpo após o request")
                .isNull();
    }
}
