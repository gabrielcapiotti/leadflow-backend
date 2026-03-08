package com.leadflow.backend.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.dto.auth.LoginRequest;
import com.leadflow.backend.dto.auth.RegisterRequest;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.exception.GlobalExceptionHandler;
import com.leadflow.backend.multitenancy.TenantContextTestCleaner;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.multitenancy.service.TenantService;
import com.leadflow.backend.security.CustomUserDetails;
import com.leadflow.backend.security.RateLimitInterceptor;
import com.leadflow.backend.security.RateLimitService;
import com.leadflow.backend.security.jwt.JwtService;
import com.leadflow.backend.security.jwt.JwtToken;
import com.leadflow.backend.service.auth.AuthService;
import com.leadflow.backend.service.auth.RefreshTokenService;
import com.leadflow.backend.service.auth.UserSessionService;
import com.leadflow.backend.service.user.UserService;
import com.leadflow.domain.auth.service.PasswordResetService;
import com.leadflow.backend.repository.user.UserRepository;

import org.junit.jupiter.api.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.boot.test.mock.mockito.MockBean;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, AuthControllerTest.AuthTestSecurityConfig.class})
@ActiveProfiles("test")
@TestExecutionListeners(
    listeners = TenantContextTestCleaner.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /* ======================================================
       MOCKED DEPENDENCIES
       ====================================================== */

    @MockBean
    private AuthService authService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private PasswordResetService passwordResetService;

    @MockBean
    private UserSessionService userSessionService;

    @MockBean
    private TenantService tenantService;

    @MockBean
    private RateLimitInterceptor rateLimitInterceptor;

    @MockBean
    private RateLimitService rateLimitService;

    @MockBean
    private UserService userService;

    private User mockUser;

    private static final String TENANT = "tenant_test";

    @BeforeEach
    void setup() {

        TenantContext.setTenant(TENANT);

        Role role = new Role("ROLE_USER");

        mockUser = new User(
                "Test User",
                "metest@test.com",
                "password",
                role
        );
        ReflectionTestUtils.setField(mockUser, "id", UUID.randomUUID());

        when(authService.registerUser(anyString(), anyString(), anyString()))
                .thenReturn(mockUser);

        when(authService.authenticateUser(anyString(), anyString()))
                .thenReturn(mockUser);

        when(jwtService.generateToken(any(User.class), anyString()))
                .thenReturn(new JwtToken(
                        "mocked-jwt-token",
                        UUID.randomUUID().toString(),
                        Instant.now().plusSeconds(3600)
                ));

        when(refreshTokenService.generate(any(User.class), anyString(), anyString()))
                .thenReturn("mocked-refresh-token");

        when(tenantService.resolveSchemaByTenantIdentifier(anyString()))
                .thenReturn(Optional.of(TENANT));

        when(tenantService.getTenantIdBySchema(anyString()))
                .thenReturn(UUID.randomUUID());
        when(userService.getActiveByEmail(anyString()))
                .thenReturn(mockUser);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    /* ======================================================
       TESTS
       ====================================================== */

    @Test
    @DisplayName("Should register user and return JWT token")
    void shouldRegisterUserAndReturnToken() throws Exception {

        RegisterRequest request = new RegisterRequest(
                "Test User",
                "user@test.com",
                "12345678"
        );

        mockMvc.perform(
                post("/auth/register")
                        .header("X-Tenant-ID", TENANT)
                        .header("User-Agent", "JUnit-Test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessToken").value("mocked-jwt-token"))
        .andExpect(jsonPath("$.refreshToken").value("mocked-refresh-token"));
    }

    @Test
    @DisplayName("Should login and return JWT")
    void shouldLoginAndReturnToken() throws Exception {

        LoginRequest request = new LoginRequest(
                "metest@test.com",
                "password"
        );

        mockMvc.perform(
                post("/auth/login")
                        .header("X-Tenant-ID", TENANT)
                        .header("User-Agent", "JUnit-Test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("mocked-jwt-token"))
        .andExpect(jsonPath("$.refreshToken").value("mocked-refresh-token"));
    }

    @Test
    @DisplayName("Should return authenticated user")
    void shouldReturnAuthenticatedUser() throws Exception {

        CustomUserDetails customUser = new CustomUserDetails(mockUser);

        mockMvc.perform(
                get("/auth/me")
                        .header("X-Tenant-ID", TENANT)
                        .with(user(customUser))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(mockUser.getEmail()));
    }

    @Test
    @DisplayName("Should return 401 when not authenticated")
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {

        mockMvc.perform(
                get("/auth/me")
        )
        .andExpect(status().isUnauthorized());
    }

    /* ======================================================
       TEST SECURITY CONFIG
       ====================================================== */

    @TestConfiguration
    static class AuthTestSecurityConfig {

        @Bean
        SecurityFilterChain authTestFilterChain(HttpSecurity http) throws Exception {

            return http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    )
                    .exceptionHandling(ex ->
                            ex.authenticationEntryPoint(
                                    (req, res, ex2) -> res.sendError(401)
                            )
                    )
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/auth/register", "/auth/login").permitAll()
                            .anyRequest().authenticated()
                    )
                    .build();
        }
    }
}