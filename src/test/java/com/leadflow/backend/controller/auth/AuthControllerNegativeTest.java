package com.leadflow.backend.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.dto.auth.LoginRequest;
import com.leadflow.backend.dto.auth.RegisterRequest;
import com.leadflow.backend.exception.GlobalExceptionHandler;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.multitenancy.service.TenantService;
import com.leadflow.backend.security.jwt.JwtService;
import com.leadflow.backend.service.auth.AuthService;
import com.leadflow.backend.service.auth.RefreshTokenService;
import com.leadflow.backend.service.auth.UserSessionService;
import com.leadflow.domain.auth.service.PasswordResetService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class AuthControllerNegativeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TenantService tenantService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private PasswordResetService passwordResetService;

    @MockBean
    private UserSessionService userSessionService;

    /* =========================================================
       SETUP TENANT CONTEXT
       ========================================================= */

    @BeforeEach
    void setup() {
        TenantContext.setTenant("tenant_id_123");
        when(tenantService.getTenantIdBySchema("tenant_id_123"))
                .thenReturn(UUID.randomUUID());
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    /* =========================================================
       REGISTER - VALIDATION
       ========================================================= */

    @Test
    void shouldReturn400WhenEmailIsInvalidOnRegister() throws Exception {

        RegisterRequest request =
                new RegisterRequest(
                        "Valid Name",
                        "invalid-email",
                        "ValidPassword123"
                );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void shouldReturn400WhenPasswordIsTooShortOnRegister() throws Exception {

        RegisterRequest request =
                new RegisterRequest(
                        "Valid Name",
                        "valid@email.com",
                        "short"
                );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void shouldReturnBusinessErrorWhenAuthServiceThrowsIllegalArgument() throws Exception {

        RegisterRequest request =
                new RegisterRequest(
                        "Valid Name",
                        "valid@email.com",
                        "ValidPassword123"
                );

        doThrow(new IllegalArgumentException("Invalid data"))
                .when(authService)
                .registerUser(anyString(), anyString(), anyString());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid data"));
    }

    /* =========================================================
       LOGIN - VALIDATION
       ========================================================= */

    @Test
    void shouldReturn400WhenEmailIsInvalidOnLogin() throws Exception {

        LoginRequest request =
                new LoginRequest(
                        "invalid-email",
                        "ValidPassword123"
                );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void shouldReturn401WhenBadCredentialsExceptionIsThrownOnLogin() throws Exception {

        LoginRequest request =
                new LoginRequest(
                        "valid@email.com",
                        "ValidPassword123"
                );

        doThrow(new BadCredentialsException("Invalid credentials"))
                .when(authService)
                .authenticateUser(anyString(), anyString());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid email or password"));
    }
}