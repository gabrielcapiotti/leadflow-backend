package com.leadflow.backend;

import com.leadflow.backend.multitenancy.identifier.CurrentTenantIdentifierResolverImpl;
import com.leadflow.backend.multitenancy.provider.MultiTenantConnectionProviderImpl;
import com.leadflow.backend.multitenancy.service.TenantProvisioningService;
import com.leadflow.backend.security.RateLimitService;
import com.leadflow.backend.security.VendorContext;
import com.leadflow.backend.service.admin.AdminService;
import com.leadflow.backend.service.ai.AiRateLimiter;
import com.leadflow.backend.service.ai.AiService;
import com.leadflow.backend.service.audit.AuditService;
import com.leadflow.backend.service.conversation.ConversationService;
import com.leadflow.backend.service.monitoring.AiMetricsService;
import com.leadflow.backend.service.monitoring.MetricsService;
import com.leadflow.backend.service.notification.SendGridEmailService;
import com.leadflow.backend.service.vendor.VendorFeatureService;
import com.leadflow.backend.service.vendor.VendorLeadService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public abstract class IntegrationTestBase {

    private static final String IMAGE = "postgres:16-alpine";

    /* ======================================================
       Testcontainer PostgreSQL
       ====================================================== */

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(IMAGE)
                    .withDatabaseName("leadflow_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    /* ======================================================
       Infra mocks
       ====================================================== */

    @MockBean
    protected SimpMessagingTemplate messagingTemplate;

    @MockBean
    protected VendorContext vendorContext;

    @MockBean
    protected MetricsService metricsService;

    @MockBean
    protected SendGridEmailService sendGridEmailService;

    @MockBean
    protected AiService aiService;

    @MockBean
    protected AuditService auditService;

    @MockBean
    protected AiRateLimiter aiRateLimiter;

    @MockBean
    protected AiMetricsService aiMetricsService;

    @MockBean
    protected ConversationService conversationService;

    @MockBean
    protected VendorFeatureService vendorFeatureService;

    @MockBean
    protected VendorLeadService vendorLeadService;

    @MockBean
    protected AdminService adminService;

    @MockBean
    protected TenantProvisioningService tenantProvisioningService;

    @MockBean
    protected RateLimitService rateLimitService;

    /* ======================================================
       Configuração dinâmica do banco
       ====================================================== */

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);

        /* ==============================
           HIBERNATE
           ============================== */

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.open-in-view", () -> "false");

        registry.add(
                "spring.jpa.properties.hibernate.multiTenancy",
                () -> "SCHEMA"
        );

        registry.add(
                "spring.jpa.properties.hibernate.multi_tenant_connection_provider",
                () -> MultiTenantConnectionProviderImpl.class.getName()
        );

        registry.add(
                "spring.jpa.properties.hibernate.multi_tenant_identifier_resolver",
                () -> CurrentTenantIdentifierResolverImpl.class.getName()
        );

        /* ==============================
           FLYWAY
           ============================== */

        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.clean-disabled", () -> "false");

        /* ==============================
           MULTITENANCY
           ============================== */

        registry.add("multitenancy.enabled", () -> "true");

        /* ==============================
           JWT
           ============================== */

        registry.add(
                "jwt.secret",
                () -> "0123456789abcdef0123456789abcdef"
        );

        registry.add(
                "jwt.expiration",
                () -> "3600000"
        );
    }

    /* ======================================================
       MockMvc
       ====================================================== */

    @Autowired
    protected MockMvc mockMvc;
}