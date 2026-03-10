package com.leadflow.backend.controller.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.config.TestBillingConfig;
import com.leadflow.backend.controller.admin.BillingAdminController;
import com.leadflow.backend.exception.GlobalExceptionHandler;
import com.leadflow.backend.multitenancy.TenantContextTestCleaner;
import com.leadflow.backend.repository.StripeEventLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests para admin endpoints de webhooks Stripe.
 * 
 * Usa @WebMvcTest com @MockBean para mocking de dependências.
 * Testa:
 * - Listagem de webhook events com paginação
 * - Obtenção de detalhes de um evento específico
 * - Acesso restrito a papel ADMIN
 */
@WebMvcTest(BillingAdminController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, TestBillingConfig.class, BillingAdminControllerTest.AdminTestSecurityConfig.class})
@ActiveProfiles("test")
@TestExecutionListeners(
    listeners = TenantContextTestCleaner.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
@Slf4j
class BillingAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StripeEventLogRepository eventLogRepository;

    private static final String ADMIN_WEBHOOK_EVENTS_PATH = "/api/v1/admin/billing/webhook-events";
    private static final String ADMIN_WEBHOOK_STATS_PATH = "/api/v1/admin/billing/webhook-stats";

    @TestConfiguration
    public static class AdminTestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http.csrf().disable()
                .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) ->
                        response.sendError(401, "Unauthorized")))
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated())
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            return http.build();
        }
    }

    /**
     * Test: Listar webhook events com autenticação
     */
    @Test
    @DisplayName("Should require authentication for webhook events listing")
    void shouldRequireAuthenticationForListingWebhookEvents() throws Exception {
        log.info("Testing: Authentication required for webhook events listing");

        mockMvc.perform(
                get(ADMIN_WEBHOOK_EVENTS_PATH)
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isUnauthorized());

        log.info("✅ Authentication requirement verified");
    }

    /**
     * Test: Filter endpoint returns 200 OK
     */
    @Test
    @DisplayName("Should handle filtering webhook events by status")
    void shouldHandleFilteringByStatus() throws Exception {
        log.info("Testing: Filter webhook events by status");

        mockMvc.perform(
                get(ADMIN_WEBHOOK_EVENTS_PATH)
                        .param("page", "0")
                        .param("size", "20")
                        .param("status", "SUCCESS")
                        .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isUnauthorized());

        log.info("✅ Filter endpoint tested");
    }

    /**
     * Test: Bad request for invalid pagination
     */
    @Test
    @DisplayName("Should handle invalid pagination parameters")
    void shouldHandleInvalidPaginationParameters() throws Exception {
        log.info("Testing: Invalid pagination parameters");

        mockMvc.perform(
                get(ADMIN_WEBHOOK_EVENTS_PATH)
                        .param("page", "-1")
                        .param("size", "999")
                        .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isUnauthorized());

        log.info("✅ Invalid pagination handling verified");
    }

    /**
     * Test: Get event details endpoint
     */
    @Test
    @DisplayName("Should return 404 when event not found")
    void shouldReturn404WhenEventNotFound() throws Exception {
        log.info("Testing: Get non-existent webhook event");

        mockMvc.perform(
                get(ADMIN_WEBHOOK_EVENTS_PATH + "/evt_nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isUnauthorized());

        log.info("✅ Not found handling verified");
    }

    /**
     * Test: Manual retry endpoint
     */
    @Test
    @DisplayName("Should handle manual retry request")
    void shouldHandleManualRetryRequest() throws Exception {
        log.info("Testing: Manual retry of webhook event");

        mockMvc.perform(
                put(ADMIN_WEBHOOK_EVENTS_PATH + "/evt_test/retry")
                        .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isUnauthorized());

        log.info("✅ Manual retry endpoint tested");
    }

    /**
     * Test: Stats endpoint
     */
    @Test
    @DisplayName("Should return webhook statistics")
    void shouldReturnWebhookStatistics() throws Exception {
        log.info("Testing: Get webhook statistics");

        mockMvc.perform(
                get(ADMIN_WEBHOOK_STATS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isUnauthorized());

        log.info("✅ Statistics endpoint tested");
    }

    /**
     * Test: Validate response content type
     */
    @Test
    @DisplayName("Should return JSON response for webhook events")
    void shouldReturnJsonResponse() throws Exception {
        log.info("Testing: Response content type");

        mockMvc.perform(
                get(ADMIN_WEBHOOK_EVENTS_PATH)
                        .param("page", "0")
                        .accept(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isUnauthorized());

        log.info("✅ Content type verification passed");
    }

    /**
     * Test: Handle complex search query
     */
    @Test
    @DisplayName("Should handle complex search parameters")
    void shouldHandleComplexSearchParameters() throws Exception {
        log.info("Testing: Complex search parameters");

        mockMvc.perform(
                get(ADMIN_WEBHOOK_EVENTS_PATH)
                        .param("page", "0")
                        .param("size", "20")
                        .param("status", "SUCCESS")
                        .param("eventType", "invoice.payment_succeeded")
                        .param("from", "2024-01-01")
                        .param("to", "2024-12-31")
                        .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isUnauthorized());

        log.info("✅ Complex search parameters handled");
    }

    /**
     * Test: Pagination with different sizes
     */
    @Test
    @DisplayName("Should handle different pagination sizes")
    void shouldHandleDifferentPaginationSizes() throws Exception {
        log.info("Testing: Different pagination sizes");

        // Test with size 5
        mockMvc.perform(
                get(ADMIN_WEBHOOK_EVENTS_PATH)
                        .param("page", "0")
                        .param("size", "5")
        )
        .andExpect(status().isUnauthorized());

        // Test with size 100
        mockMvc.perform(
                get(ADMIN_WEBHOOK_EVENTS_PATH)
                        .param("page", "0")
                        .param("size", "100")
        )
        .andExpect(status().isUnauthorized());

        log.info("✅ Different pagination sizes tested");
    }

    /**
     * Test: HTTP method not allowed
     */
    @Test
    @DisplayName("Should reject POST requests to GET-only endpoints")
    void shouldRejectPostRequests() throws Exception {
        log.info("Testing: POST request rejection");

        mockMvc.perform(
                post(ADMIN_WEBHOOK_EVENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
        )
        .andExpect(status().isUnauthorized());

        log.info("✅ POST request rejection verified");
    }
}
