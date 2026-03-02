package com.leadflow.backend.multitenancy;

import com.leadflow.backend.IntegrationTestBase;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.util.TestTenantFactory;
import static org.junit.jupiter.api.Assertions.assertFalse; 

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "multitenancy.enabled=true",
        "jwt.secret=0123456789abcdef0123456789abcdef"
})
class TenantFilterIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestTenantFactory testTenantFactory;

    private static final String TENANT_IDENTIFIER = "tenant_a";

    @BeforeEach
    void setup() {
        testTenantFactory.createTenant("Tenant A");
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
        // O status não é relevante — apenas força execução do filtro

        assertFalse(TenantContext.isSet()); // Verifica se o contexto foi limpo corretamente
    }
}