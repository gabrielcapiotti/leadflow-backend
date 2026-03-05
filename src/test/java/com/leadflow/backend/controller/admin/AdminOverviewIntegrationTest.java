package com.leadflow.backend.controller.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.IntegrationTestBase;
import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;
import com.leadflow.backend.util.TestTenantFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminOverviewIntegrationTest extends IntegrationTestBase {

        private static final String TENANT_NAME = "Tenant A";
    private static final String ADMIN_EMAIL = "admin.integration@test.com";
    private static final String USER_EMAIL = "user.integration@test.com";
    private static final String PASSWORD = "Admin@123";
        private static final String USER_AGENT = "JUnit-AdminOverviewIntegrationTest";

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

    @BeforeEach
    void setup() {
                Tenant tenant = testTenantFactory.createTenant(TENANT_NAME);
                tenantSchema = tenant.getSchemaName();
                ensureAuthTablesInTenantSchema();
        seedUsers();
    }

        private void ensureAuthTablesInTenantSchema() {
                cloneTableFromPublic("login_audit");
                cloneTableFromPublic("security_audit_logs");
                cloneTableFromPublic("user_sessions");
                cloneTableFromPublic("refresh_tokens");
        }

        private void cloneTableFromPublic(String tableName) {
                TransactionTemplate tx = new TransactionTemplate(transactionManager);
                tx.executeWithoutResult(status ->
                        jdbcTemplate.execute(
                                String.format(
                                        "CREATE TABLE IF NOT EXISTS %s.%s (LIKE public.%s INCLUDING ALL)",
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

    private void seedUsers() {
        TenantContext.setTenant(tenantSchema);
        try {
            Role adminRole = roleRepository.findByNameIgnoreCase("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));

            Role userRole = roleRepository.findByNameIgnoreCase("ROLE_USER")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

            userRepository.findByEmailIgnoreCase(ADMIN_EMAIL)
                    .orElseGet(() -> userRepository.save(
                            new User(
                                    "Admin Integration",
                                    ADMIN_EMAIL,
                                    passwordEncoder.encode(PASSWORD),
                                    adminRole
                            )
                    ));

            userRepository.findByEmailIgnoreCase(USER_EMAIL)
                    .orElseGet(() -> userRepository.save(
                            new User(
                                    "User Integration",
                                    USER_EMAIL,
                                    passwordEncoder.encode(PASSWORD),
                                    userRole
                            )
                    ));
        } finally {
            TenantContext.clear();
        }
    }

    private String loginAndGetAccessToken(String email, String password) throws Exception {
        String body = objectMapper.createObjectNode()
                .put("email", email)
                .put("password", password)
                .toString();

        String response = mockMvc.perform(
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
        return json.get("accessToken").asText();
    }
}
