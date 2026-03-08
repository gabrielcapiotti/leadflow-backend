package com.leadflow.backend;

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
       Infra mocks (dependências externas / cross-cutting)
       ====================================================== */

    // WebSocket
    @MockBean
    protected SimpMessagingTemplate messagingTemplate;

    // Contexto de segurança
    @MockBean
    protected VendorContext vendorContext;

    // Métricas
    @MockBean
    protected MetricsService metricsService;

    // Email
    @MockBean
    protected SendGridEmailService sendGridEmailService;

    // AI
    @MockBean
    protected AiService aiService;

    // Auditoria
    @MockBean
    protected AuditService auditService;

    // AI Rate Limiter
    @MockBean
    protected AiRateLimiter aiRateLimiter;

    // AI Metrics
    @MockBean
    protected AiMetricsService aiMetricsService;

    // Conversation
    @MockBean
    protected ConversationService conversationService;

    // Vendor features
    @MockBean
    protected VendorFeatureService vendorFeatureService;

    // Vendor leads
    @MockBean
    protected VendorLeadService vendorLeadService;

    // Admin
    @MockBean
    protected AdminService adminService;

    // Tenant provisioning
    @MockBean
    protected TenantProvisioningService tenantProvisioningService;

    // Rate limit
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

        // JPA
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.open-in-view", () -> "false");
        registry.add("spring.jpa.properties.hibernate.multiTenancy", () -> "SCHEMA");

        // Flyway ativado para testes
        registry.add("spring.flyway.enabled", () -> "true");

        // Multitenancy
        registry.add("multitenancy.enabled", () -> "true");

        // JWT
        registry.add("jwt.secret", () -> "0123456789abcdef0123456789abcdef");
        registry.add("jwt.expiration", () -> "3600000");
    }

    /* ======================================================
       MockMvc
       ====================================================== */

    @Autowired
    protected MockMvc mockMvc;
}