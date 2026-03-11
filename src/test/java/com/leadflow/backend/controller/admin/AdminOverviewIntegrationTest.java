package com.leadflow.backend.controller.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.BackendApplication;
import com.leadflow.backend.IntegrationTestBase;
import com.leadflow.backend.config.TestBillingConfig;
import com.leadflow.backend.dto.auth.RegisterRequest;
import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.entities.vendor.SubscriptionStatus;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.repository.VendorAuditLogRepository;
import com.leadflow.backend.repository.VendorRepository;
import com.leadflow.backend.repository.audit.SecurityAuditLogRepository;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;
import com.leadflow.backend.util.TestTenantFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = BackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestBillingConfig.class)
class AdminOverviewIntegrationTest extends IntegrationTestBase {

    private static final String TENANT_NAME = "tenant_admin_test";
    private static final String PASSWORD = "Admin@123";
    private static final String USER_AGENT = "JUnit-AdminOverviewIntegrationTest";
    private static final String DEVICE_FINGERPRINT = "test-device-fingerprint";

    private String tenantSchema;

    private String adminEmail;
    private String userEmail;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestTenantFactory testTenantFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private SecurityAuditLogRepository securityAuditLogRepository;

    @MockitoBean
    private VendorAuditLogRepository vendorAuditLogRepository;

    @MockitoBean
    private VendorRepository vendorRepository;

    @BeforeEach
    void setup() {

        adminEmail = generateUniqueEmail();
        userEmail = generateUniqueEmail();

        TenantContext.clear();

        Tenant tenant = testTenantFactory.createTenant(TENANT_NAME);
        tenantSchema = tenant.getSchemaName();

        ensureAuthTablesInTenantSchema();

        TenantContext.setTenant(tenantSchema);
        try {
            new TransactionTemplate(transactionManager)
                    .executeWithoutResult(tx -> seedUsers());
        } finally {
            TenantContext.clear();
        }

        when(vendorRepository.countAllGlobal()).thenReturn(10L);
        when(vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.ATIVA)).thenReturn(5L);
        when(vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.TRIAL)).thenReturn(3L);
        when(vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.INADIMPLENTE)).thenReturn(1L);
        when(vendorRepository.countBySubscriptionStatusGlobal(SubscriptionStatus.EXPIRADA)).thenReturn(1L);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    private void ensureAuthTablesInTenantSchema() {

        cloneTableFromPublic("roles");
        cloneTableFromPublic("users");
        cloneTableFromPublic("login_audit");
        cloneTableFromPublic("security_audit_logs");
        cloneTableFromPublic("user_sessions");
        cloneTableFromPublic("refresh_tokens");
    }

    private void cloneTableFromPublic(String tableName) {

        String sourceTable = switch (tableName) {
            case "users" -> "template_users";
            case "user_sessions" -> "template_user_sessions";
            case "refresh_tokens" -> "template_refresh_tokens";
            default -> tableName;
        };

        jdbcTemplate.execute(
                String.format("""
                CREATE TABLE IF NOT EXISTS "%s"."%s"
                (LIKE public."%s" INCLUDING ALL)
                        """,
                        tenantSchema,
                        tableName,
                        sourceTable
                )
        );
    }

    private void seedUsers() {

        UUID adminRoleId = UUID.randomUUID();
        UUID userRoleId = UUID.randomUUID();

        jdbcTemplate.execute(
                String.format("""
                        CREATE TABLE IF NOT EXISTS "%s"."roles" (
                            id UUID PRIMARY KEY,
                            name VARCHAR(50) NOT NULL UNIQUE,
                            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """, tenantSchema)
        );

        jdbcTemplate.execute(
                String.format("""
                        CREATE TABLE IF NOT EXISTS "%s"."users" (
                            id UUID PRIMARY KEY,
                            name VARCHAR(255),
                            email VARCHAR(255),
                            password VARCHAR(255),
                            role_id UUID,
                            failed_attempts INTEGER NOT NULL DEFAULT 0,
                            lock_until TIMESTAMPTZ,
                            credentials_updated_at TIMESTAMPTZ,
                            created_at TIMESTAMPTZ,
                            updated_at TIMESTAMPTZ,
                            deleted_at TIMESTAMPTZ
                        )
                        """, tenantSchema)
        );

        jdbcTemplate.execute(String.format("DELETE FROM \"%s\".\"users\"", tenantSchema));
        jdbcTemplate.execute(String.format("DELETE FROM \"%s\".\"roles\"", tenantSchema));

        jdbcTemplate.update(
                String.format("""
                        INSERT INTO "%s"."roles" (id, name, created_at, updated_at)
                        VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """, tenantSchema),
                adminRoleId,
                "ROLE_ADMIN"
        );

        jdbcTemplate.update(
                String.format("""
                        INSERT INTO "%s"."roles" (id, name, created_at, updated_at)
                        VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """, tenantSchema),
                userRoleId,
                "ROLE_USER"
        );

        jdbcTemplate.update(
                String.format("""
                        INSERT INTO "%s"."users"
                            (id, name, email, password, role_id, failed_attempts, credentials_updated_at, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """, tenantSchema),
                UUID.randomUUID(),
                "Admin Integration",
                adminEmail,
                passwordEncoder.encode(PASSWORD),
                adminRoleId
        );

        jdbcTemplate.update(
                String.format("""
                        INSERT INTO "%s"."users"
                            (id, name, email, password, role_id, failed_attempts, credentials_updated_at, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """, tenantSchema),
                UUID.randomUUID(),
                "User Integration",
                userEmail,
                passwordEncoder.encode(PASSWORD),
                userRoleId
        );
    }

    private String loginAndGetAccessToken(String email, String password) throws Exception {

        TenantContext.setTenant(tenantSchema);

        String body =
                objectMapper.createObjectNode()
                        .put("email", email)
                        .put("password", password)
                        .toString();

        String response =
                mockMvc.perform(
                        post("/auth/login")
                                .header("X-Tenant-ID", tenantSchema)
                                .header("User-Agent", USER_AGENT)
                                .header("X-Device-Fingerprint", DEVICE_FINGERPRINT)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        JsonNode json = objectMapper.readTree(response);

        String token = json.path("accessToken").asText();

        if (token.isBlank())
            token = json.path("access_token").asText();

        if (token.isBlank())
            token = json.path("token").asText();

        assertThat(token).isNotBlank();

        return token;
    }

    private String generateUniqueEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void shouldReturn401WhenNoToken() throws Exception {

        mockMvc.perform(
                get("/admin/overview")
                        .header("X-Tenant-ID", tenantSchema)
                        .header("User-Agent", USER_AGENT)
                        .header("X-Device-Fingerprint", DEVICE_FINGERPRINT)
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403ForNonAdminToken() throws Exception {

        String userToken = loginAndGetAccessToken(userEmail, PASSWORD);

        mockMvc.perform(
                get("/admin/overview")
                        .header("X-Tenant-ID", tenantSchema)
                        .header("User-Agent", USER_AGENT)
                        .header("X-Device-Fingerprint", DEVICE_FINGERPRINT)
                        .header("Authorization", "Bearer " + userToken)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200ForAdminToken() throws Exception {

        String adminToken = loginAndGetAccessToken(adminEmail, PASSWORD);

        mockMvc.perform(
                get("/admin/overview")
                        .header("X-Tenant-ID", tenantSchema)
                        .header("User-Agent", USER_AGENT)
                        .header("X-Device-Fingerprint", DEVICE_FINGERPRINT)
                        .header("Authorization", "Bearer " + adminToken)
        )
        .andExpect(status().isOk());
    }

    @Test
    void shouldRegisterLoginAndReturnToken() throws Exception {

        String email = generateUniqueEmail();

        RegisterRequest request =
                new RegisterRequest(
                        "Admin Test",
                        email,
                        "password123"
                );

        TenantContext.setTenant(tenantSchema);

        try {

            mockMvc.perform(
                    post("/auth/register")
                            .header("X-Tenant-ID", tenantSchema)
                            .header("User-Agent", USER_AGENT)
                            .header("X-Device-Fingerprint", DEVICE_FINGERPRINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isCreated());

        } finally {
            TenantContext.clear();
        }

        String token = loginAndGetAccessToken(email, "password123");

        assertThat(token).isNotBlank();
    }
}