package com.leadflow.backend.multitenancy;

import com.leadflow.backend.IntegrationTestBase;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.multitenancy.service.TenantService;
import com.leadflow.backend.util.TestTenantFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("integration")
@TestPropertySource(properties = "multitenancy.enabled=true")
class TenantFilterIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestTenantFactory testTenantFactory;

    @MockBean
    private TenantService tenantService;

    private static final String TENANT_IDENTIFIER = "tenant_a";
    private static final String SCHEMA = "tenant_a";

    @BeforeEach
    void setup() {

        testTenantFactory.createTenant("Tenant A");

        when(tenantService.resolveSchemaByTenantIdentifier(TENANT_IDENTIFIER))
                .thenReturn(Optional.of(SCHEMA));

        doNothing().when(tenantService).validateSchemaName(SCHEMA);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldResolveTenantAndResetContextAfterRequest() throws Exception {

        mockMvc.perform(
                get("/api/test")
                        .header("X-Tenant-ID", TENANT_IDENTIFIER)
        )
        .andExpect(status().is4xxClientError());
        // O status não importa, apenas precisamos que o filtro execute

        verify(tenantService, atLeastOnce())
                .resolveSchemaByTenantIdentifier(TENANT_IDENTIFIER);

        assertThat(TenantContext.getTenant())
                .as("TenantContext deve voltar para o schema default após o request")
                .isEqualTo("public");
    }
}