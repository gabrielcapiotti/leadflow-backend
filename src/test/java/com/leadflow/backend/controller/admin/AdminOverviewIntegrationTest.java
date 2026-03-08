package com.leadflow.backend.controller.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.BackendApplication;
import com.leadflow.backend.IntegrationTestBase;
import com.leadflow.backend.dto.auth.RegisterRequest;
import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = BackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminOverviewIntegrationTest extends IntegrationTestBase {

    private static final String TENANT_NAME = "Tenant A";

    private static final String ADMIN_EMAIL = "admin.integration@test.com";
    private static final String USER_EMAIL = "user.integration@test.com";

    private static final String PASSWORD = "Admin@123";

    private static final String USER_AGENT =
            "JUnit-AdminOverviewIntegrationTest";

    private String tenantSchema;

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

        TenantContext.clear();

        Tenant tenant = testTenantFactory.createTenant(TENANT_NAME);
        tenantSchema = tenant.getSchemaName();

        ensureAuthTablesInTenantSchema();
        seedUsers();

        // Mock vendorRepository to return non-zero values
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

        cloneTableFromPublic("login_audit");
        cloneTableFromPublic("security_audit_logs");
        cloneTableFromPublic("user_sessions");
        cloneTableFromPublic("refresh_tokens");
        cloneTableFromPublic("users"); // Added users table
    }

    private void cloneTableFromPublic(String tableName) {

        TransactionTemplate tx =
                new TransactionTemplate(
                        Objects.requireNonNull(transactionManager)
                );

        tx.executeWithoutResult(status ->
                jdbcTemplate.execute(
                        String.format("""
                                CREATE TABLE IF NOT EXISTS %s.%s
                                (LIKE public.%s INCLUDING ALL)
                                """,
                                tenantSchema,
                                tableName,
                                tableName
                        )
                )
        );
    }

    @Test
    void shouldReturn401WhenNoToken() throws Exception {

        mockMvc.perform(
                get("/admin/overview")
                        .header("X-Tenant-ID", tenantSchema)
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403ForNonAdminToken() throws Exception {

        String userToken = loginAndGetAccessToken(USER_EMAIL, PASSWORD);

        mockMvc.perform(
                get("/admin/overview")
                        .header("X-Tenant-ID", tenantSchema)
                        .header("User-Agent", USER_AGENT)
                        .header("Authorization", "Bearer " + userToken)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200ForAdminToken() throws Exception {

        String adminToken = loginAndGetAccessToken(ADMIN_EMAIL, PASSWORD);

        mockMvc.perform(
                get("/admin/overview")
                        .header("X-Tenant-ID", tenantSchema)
                        .header("User-Agent", USER_AGENT)
                        .header("Authorization", "Bearer " + adminToken)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total_vendors").exists())
        .andExpect(jsonPath("$.active_subscriptions").exists())
        .andExpect(jsonPath("$.trial_subscriptions").exists())
        .andExpect(jsonPath("$.inadimplentes").exists())
        .andExpect(jsonPath("$.expiradas").exists())
        .andExpect(jsonPath("$.total_leads").exists())
        .andExpect(jsonPath("$.total_ai_executions_current_cycle").exists())
        .andExpect(jsonPath("$.estimated_monthly_revenue").exists());
    }

    @Test
    void shouldReturnTotalVendors() throws Exception {

        String adminToken = loginAndGetAccessToken(ADMIN_EMAIL, PASSWORD);

        mockMvc.perform(
                get("/admin/overview")
                        .header("X-Tenant-ID", tenantSchema) // Use tenant schema
                        .header("User-Agent", USER_AGENT)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total_vendors").exists()); // Corrected JSON path
    }

    private void seedUsers() {

        TenantContext.setTenant(tenantSchema);

        try {

            Role adminRole =
                    roleRepository.findByNameIgnoreCase("ROLE_ADMIN")
                            .orElseGet(() ->
                                    roleRepository.save(new Role("ROLE_ADMIN")));

            Role userRole =
                    roleRepository.findByNameIgnoreCase("ROLE_USER")
                            .orElseGet(() ->
                                    roleRepository.save(new Role("ROLE_USER")));

            userRepository.findByEmailIgnoreCase(ADMIN_EMAIL)
                    .orElseGet(() ->
                            userRepository.save(
                                    new User(
                                            "Admin Integration",
                                            ADMIN_EMAIL,
                                            passwordEncoder.encode(PASSWORD),
                                            adminRole
                                    )
                            )
                    );

            userRepository.findByEmailIgnoreCase(USER_EMAIL)
                    .orElseGet(() ->
                            userRepository.save(
                                    new User(
                                            "User Integration",
                                            USER_EMAIL,
                                            passwordEncoder.encode(PASSWORD),
                                            userRole
                                    )
                            )
                    );

        } finally {
            TenantContext.clear();
        }
    }

    private String loginAndGetAccessToken(String email, String password) throws Exception {

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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);

        // Adjusted to check for alternative token fields
        String token = json.path("accessToken").asText();
        if (token.isBlank()) {
            token = json.path("access_token").asText();
        }
        if (token.isBlank()) {
            token = json.path("token").asText();
        }

        assertThat(token).isNotBlank();

        return token;
    }

    @Test
    void shouldRegisterLoginAndReturnToken() throws Exception {

        String email = "admin+" + UUID.randomUUID() + "@test.com";

        RegisterRequest registerRequest =
                new RegisterRequest(
                        "Admin Test",
                        email,
                        "password123"
                );

        mockMvc.perform(
                post("/auth/register")
                .header("X-Tenant-ID", tenantSchema)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
        )
        .andExpect(status().isOk());

        String token = loginAndGetAccessToken(email, "password123");

        assertThat(token).isNotBlank();
    }
}