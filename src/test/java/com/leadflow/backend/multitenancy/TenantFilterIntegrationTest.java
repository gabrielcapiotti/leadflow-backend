package com.leadflow.backend.multitenancy;

import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.multitenancy.resolver.JwtTenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TenantFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTenantResolver jwtTenantResolver;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldResolveTenantAndClearContextAfterRequest() throws Exception {

        // Arrange
        when(jwtTenantResolver.resolveTenant(any(HttpServletRequest.class)))
                .thenReturn("tenant_a");

        // Act
        mockMvc.perform(
                get("/auth/me")
                        .header("Authorization", "Bearer valid-token")
        );

        // Assert 1: Resolver foi chamado
        verify(jwtTenantResolver, times(1))
                .resolveTenant(any(HttpServletRequest.class));

        // Assert 2: Após o request, contexto deve estar limpo
        assertThat(TenantContext.getTenant())
                .as("TenantContext deve estar limpo após o request")
                .isNull();
    }
}
