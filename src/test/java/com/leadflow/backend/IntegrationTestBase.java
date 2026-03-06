package com.leadflow.backend;

import com.leadflow.backend.multitenancy.service.TenantProvisioningService;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    private static final String IMAGE = "postgres:16-alpine";

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(IMAGE)
                    .withDatabaseName("leadflow_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    static {
        postgres.start();
    }

    /* ======================================================
       Infra mocks (dependências externas / cross-cutting)
       ====================================================== */

    // WebSocket (AlertNotificationService)
    @MockBean
    protected SimpMessagingTemplate messagingTemplate;

    // Contexto de segurança do vendor
    @MockBean
    protected VendorContext vendorContext;

    // Métricas
    @MockBean
    protected MetricsService metricsService;

    // Email (SendGrid)
    @MockBean
    protected SendGridEmailService sendGridEmailService;

    // IA (usado por AiController e ResumoService)
    @MockBean
    protected AiService aiService;

    // Auditoria (usado por ResumoService)
    @MockBean
    protected AuditService auditService;

    // AI Rate Limiter (used by AiController)
    @MockBean
    protected AiRateLimiter aiRateLimiter;

    // AI Metrics Service (used by AiController)
    @MockBean
    protected AiMetricsService aiMetricsService;

    // Conversation Service (used by ResumoService and AiController)
    @MockBean
    protected ConversationService conversationService;

    // Vendor Feature Service (used by AiController)
    @MockBean
    protected VendorFeatureService vendorFeatureService;

    // Vendor Lead Service (used by AiController)
    @MockBean
    protected VendorLeadService vendorLeadService;

    // Admin Service (used by AiController)
    @MockBean
    protected AdminService adminService;

    // Tenant Provisioning Service (used by AiController)
    @MockBean
    protected TenantProvisioningService tenantProvisioningService;

    /* ======================================================
       Configuração dinâmica do banco (Testcontainers)
       ====================================================== */

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        // Datasource
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);

        // JPA
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.open-in-view", () -> "false");
        registry.add("spring.jpa.properties.hibernate.multiTenancy", () -> "SCHEMA");

        // Flyway desabilitado para testes
        registry.add("spring.flyway.enabled", () -> "false");

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