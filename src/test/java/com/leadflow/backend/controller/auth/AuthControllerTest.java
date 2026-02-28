package com.leadflow.backend.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.config.TestSecurityConfig;
import com.leadflow.backend.dto.auth.LoginRequest;
import com.leadflow.backend.dto.auth.RegisterRequest;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.exception.GlobalExceptionHandler;
import com.leadflow.backend.security.jwt.JwtService;
import com.leadflow.backend.security.jwt.JwtToken;
import com.leadflow.backend.service.auth.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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

        Role role = new Role("ROLE_USER");

        mockUser = new User(
                "Test User",
                "metest@test.com",
                "password",
                role
        );

        ReflectionTestUtils.setField(mockUser, "id", UUID.randomUUID());

        when(authService.findByEmail("metest@test.com"))
                .thenReturn(mockUser);

        when(authService.registerUser(anyString(), anyString(), anyString()))
                .thenReturn(mockUser);

        when(authService.authenticateUser(anyString(), anyString()))
                .thenReturn(mockUser);

        // ✅ Agora o mock reflete o método correto
        when(jwtService.generateToken(any(User.class), anyString()))
                .thenReturn(
                        new JwtToken(
                                "mocked-jwt-token",
                                UUID.randomUUID().toString(),
                                Instant.now().plusSeconds(3600)
                        )
                );
    }

    private RequestPostProcessor tenant() {
        return request -> {
            request.addHeader("X-Tenant-ID", "test_schema");
            return request;
        };
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
                        .with(tenant())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.token").value("mocked-jwt-token"));
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
                        .with(tenant())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("mocked-jwt-token"));
    }

    /* =========================================================
       ME - AUTHENTICATED
       ========================================================= */

    @Test
    @WithMockUser(username = "metest@test.com", roles = {"USER"})
    @DisplayName("Should return authenticated user")
    void shouldReturnAuthenticatedUser() throws Exception {

        mockMvc.perform(get("/auth/me").with(tenant()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("metest@test.com"));
    }

    /* =========================================================
       ME - NOT AUTHENTICATED
       ========================================================= */

    @Test
    @DisplayName("Should return 401 when not authenticated")
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {

        mockMvc.perform(get("/auth/me").with(tenant()))
                .andExpect(status().isUnauthorized());
    }
}