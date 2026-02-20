package com.leadflow.backend.multitenancy;

import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.multitenancy.service.TenantService;
import com.leadflow.backend.util.TestTenantFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

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
    private TestTenantFactory testTenantFactory;

    @MockBean
    private TenantService tenantService;

    private static final String SCHEMA = "tenant_a";

    @BeforeEach
    void setup() {
        testTenantFactory.createTenant("Tenant A");

        when(tenantService.resolveSchemaByTenantIdentifier("tenant_a"))
                .thenReturn(Optional.of(SCHEMA));
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldResolveTenantAndClearContextAfterRequest() throws Exception {

        mockMvc.perform(
                get("/auth/login")
                        .header("X-Tenant-ID", "tenant_a")
        );

        verify(tenantService, times(1))
                .resolveSchemaByTenantIdentifier("tenant_a");

        assertThat(TenantContext.getTenant())
                .as("TenantContext deve estar limpo após o request")
                .isNull();
    }
}