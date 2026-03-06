package com.leadflow.backend.multitenancy;

import com.leadflow.backend.IntegrationTestBase;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.util.TestTenantFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestTenantFactory.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "multitenancy.enabled=true",
        "jwt.secret=0123456789abcdef0123456789abcdef"
})
class TenantFilterIntegrationTest extends IntegrationTestBase {

    private static final String TENANT_IDENTIFIER = "tenant_a";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestTenantFactory testTenantFactory;

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
        // endpoint não precisa existir; objetivo é executar o filtro
        .andExpect(status().is4xxClientError());

        // garante que o TenantContext foi limpo após a requisição
        assertFalse(TenantContext.isSet());
    }
}