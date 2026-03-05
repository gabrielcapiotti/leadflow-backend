package com.leadflow.backend.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.dto.auth.LoginRequest;
import com.leadflow.backend.dto.auth.RegisterRequest;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.exception.GlobalExceptionHandler;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.multitenancy.service.TenantService;
import com.leadflow.backend.security.CustomUserDetails;
import com.leadflow.backend.security.RateLimitService;
import com.leadflow.backend.security.jwt.JwtService;
import com.leadflow.backend.security.jwt.JwtToken;
import com.leadflow.backend.service.auth.AuthService;
import com.leadflow.backend.service.auth.RefreshTokenService;
import com.leadflow.backend.service.auth.UserSessionService;
import com.leadflow.domain.auth.service.PasswordResetService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

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
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @MockitoBean private AuthService authService;
        @MockitoBean private JwtService jwtService;
        @MockitoBean private RefreshTokenService refreshTokenService;
        @MockitoBean private PasswordResetService passwordResetService;
        @MockitoBean private UserSessionService userSessionService;
        @MockitoBean private TenantService tenantService;
        @MockitoBean private RateLimitService rateLimitService;

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

        when(refreshTokenService.generate(
                any(User.class),
                anyString(),
                anyString()
        )).thenReturn("mocked-refresh-token");

        when(tenantService.resolveSchemaByTenantIdentifier(anyString()))
                .thenReturn(Optional.of(TENANT));

        when(tenantService.getTenantIdBySchema(anyString()))
                .thenReturn(UUID.randomUUID());
    }

        @AfterEach
        void cleanup() {
                TenantContext.clear();
        }

    @Test
    @DisplayName("Should register user and return JWT token")
    void shouldRegisterUserAndReturnToken() throws Exception {

        RegisterRequest registerRequest = new RegisterRequest(
                "Test User",
                "user" + UUID.randomUUID() + "@test.com",
                "12345678"
        );

        mockMvc.perform(
                post("/auth/register")
                        .header("X-Tenant-ID", TENANT)
                        .header("User-Agent", "JUnit-Test")
                        .with(req -> {
                            req.setRemoteAddr("127.0.0.1");
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessToken").value("mocked-jwt-token"))
        .andExpect(jsonPath("$.refreshToken").value("mocked-refresh-token"));
    }

    @Test
    @DisplayName("Should login and return JWT")
    void shouldLoginAndReturnToken() throws Exception {

        LoginRequest loginRequest = new LoginRequest(
                "metest@test.com",
                "password"
        );

        mockMvc.perform(
                post("/auth/login")
                        .header("X-Tenant-ID", TENANT)
                        .header("User-Agent", "JUnit-Test")
                        .with(req -> {
                            req.setRemoteAddr("127.0.0.1");
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("mocked-jwt-token"))
        .andExpect(jsonPath("$.refreshToken").value("mocked-refresh-token"));
    }

    @Test
    @DisplayName("Should return authenticated user")
    void shouldReturnAuthenticatedUser() throws Exception {

        CustomUserDetails customUser =
                new CustomUserDetails(mockUser);

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
                        .header("X-Tenant-ID", TENANT)
        )
        .andExpect(status().isUnauthorized());
    }

        @TestConfiguration
        static class AuthTestSecurityConfig {

                @Bean
                SecurityFilterChain authTestFilterChain(HttpSecurity http) throws Exception {
                        return http
                                        .csrf(csrf -> csrf.disable())
                                        .sessionManagement(session ->
                                                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                                        )
                                        .authorizeHttpRequests(auth -> auth
                                                        .requestMatchers("/auth/register", "/auth/login").permitAll()
                                                        .anyRequest().authenticated()
                                        )
                                            .exceptionHandling(ex -> ex
                                                    .authenticationEntryPoint((request, response, authException) ->
                                                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                                                    )
                                                    .accessDeniedHandler((request, response, accessDeniedException) ->
                                                            response.sendError(HttpServletResponse.SC_FORBIDDEN)
                                                    )
                                            )
                                        .build();
                }
        }
}