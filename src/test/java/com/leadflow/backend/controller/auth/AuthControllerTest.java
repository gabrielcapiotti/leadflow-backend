package com.leadflow.backend.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.config.TestSecurityConfig;
import com.leadflow.backend.dto.auth.LoginRequest;
import com.leadflow.backend.dto.auth.RegisterRequest;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.exception.GlobalExceptionHandler;
import com.leadflow.backend.security.jwt.JwtService;
import com.leadflow.backend.service.auth.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    private User mockUser;

    @BeforeEach
    void setup() {

        mockUser = new User(
                "Test User",
                "metest@test.com",
                "password",
                new Role("USER")
        );

        when(authService.findByEmail("metest@test.com"))
                .thenReturn(mockUser);

        when(authService.registerUser(anyString(), anyString(), anyString()))
                .thenReturn(mockUser);

        when(authService.authenticateUser(anyString(), anyString()))
                .thenReturn(mockUser);

        when(jwtService.generateToken(any(User.class)))
                .thenReturn("mocked-jwt-token");
    }

    /* =========================================================
       REGISTER - SUCCESS
       ========================================================= */

    @Test
    @DisplayName("Should register user and return JWT token")
    void shouldRegisterUserAndReturnToken() throws Exception {

        RegisterRequest request =
                new RegisterRequest(
                        "Test User",
                        "user" + UUID.randomUUID() + "@test.com",
                        "12345678"
                );

        mockMvc.perform(
                post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.token").value("mocked-jwt-token"));
    }

    /* =========================================================
       REGISTER - INVALID EMAIL
       ========================================================= */

    @Test
    @DisplayName("Should return detailed validation error when email is invalid")
    void shouldReturnValidationErrorForInvalidEmail() throws Exception {

        RegisterRequest request =
                new RegisterRequest(
                        "Test User",
                        "invalid-email",
                        "12345678"
                );

        mockMvc.perform(
                post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Validation Error"))
        .andExpect(jsonPath("$.message", containsString("Email")))
        .andExpect(jsonPath("$.timestamp").exists());
    }

    /* =========================================================
       REGISTER - SHORT PASSWORD
       ========================================================= */

    @Test
    @DisplayName("Should return validation error when password is too short")
    void shouldReturnValidationErrorForShortPassword() throws Exception {

        RegisterRequest request =
                new RegisterRequest(
                        "Test User",
                        "valid@email.com",
                        "123"
                );

        mockMvc.perform(
                post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Validation Error"))
        .andExpect(jsonPath("$.message",
                containsString("Senha deve ter no mínimo")))
        .andExpect(jsonPath("$.timestamp").exists());
    }

    /* =========================================================
       REGISTER - MULTIPLE ERRORS
       ========================================================= */

    @Test
    @DisplayName("Should aggregate multiple validation errors in single response")
    void shouldReturnMultipleValidationErrors() throws Exception {

        RegisterRequest request =
                new RegisterRequest(
                        "",
                        "invalid",
                        "123"
                );

        mockMvc.perform(
                post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation Error"))
        .andExpect(jsonPath("$.message", containsString("Nome")))
        .andExpect(jsonPath("$.message", containsString("Email")))
        .andExpect(jsonPath("$.message", containsString("Senha")));
    }

    /* =========================================================
       LOGIN - SUCCESS
       ========================================================= */

    @Test
    @DisplayName("Should login and return JWT")
    void shouldLoginAndReturnToken() throws Exception {

        LoginRequest request =
                new LoginRequest(
                        "metest@test.com",
                        "password"
                );

        mockMvc.perform(
                post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("mocked-jwt-token"));
    }

    /* =========================================================
       LOGIN - BUSINESS ERROR
       ========================================================= */

    @Test
    @DisplayName("Should map IllegalArgumentException to Business Error")
    void shouldMapIllegalArgumentException() throws Exception {

        when(authService.authenticateUser(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Invalid credentials"));

        LoginRequest request =
                new LoginRequest(
                        "wrong@email.com",
                        "wrong"
                );

        mockMvc.perform(
                post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Business Error"))
        .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    /* =========================================================
       ME - AUTHENTICATED
       ========================================================= */

    @Test
    @WithMockUser(username = "metest@test.com", roles = {"USER"})
    @DisplayName("Should return authenticated user")
    void shouldReturnAuthenticatedUser() throws Exception {

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("metest@test.com"));
    }

    /* =========================================================
       ME - NOT AUTHENTICATED
       ========================================================= */

    @Test
    @DisplayName("Should return 401 when not authenticated")
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
