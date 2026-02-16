package com.leadflow.backend.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadflow.backend.config.TestSecurityConfig;
import com.leadflow.backend.dto.auth.LoginRequest;
import com.leadflow.backend.dto.auth.RegisterRequest;
import com.leadflow.backend.exception.GlobalExceptionHandler;
import com.leadflow.backend.security.TokenService;
import com.leadflow.backend.service.auth.AuthService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
class AuthControllerNegativeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private TokenService tokenService;

    /* =========================================================
       REGISTER - VALIDATION
       ========================================================= */

    @Test
    void shouldReturn400WhenEmailIsInvalidOnRegister() throws Exception {

        RegisterRequest request = new RegisterRequest();
        request.setName("Valid Name");
        request.setEmail("invalid-email");
        request.setPassword("ValidPassword123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn400WhenPasswordIsTooShortOnRegister() throws Exception {

        RegisterRequest request = new RegisterRequest();
        request.setName("Valid Name");
        request.setEmail("valid@email.com");
        request.setPassword("short");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));
    }

    @Test
    void shouldReturnBusinessErrorWhenAuthServiceThrowsIllegalArgument() throws Exception {

        RegisterRequest request = new RegisterRequest();
        request.setName("Valid Name");
        request.setEmail("valid@email.com");
        request.setPassword("ValidPassword123");

        doThrow(new IllegalArgumentException("Invalid data"))
                .when(authService)
                .registerUser(anyString(), anyString(), anyString());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Business Error"))
                .andExpect(jsonPath("$.message").value("Invalid data"));
    }

    /* =========================================================
       LOGIN - VALIDATION
       ========================================================= */

    @Test
    void shouldReturn400WhenEmailIsInvalidOnLogin() throws Exception {

        LoginRequest request = new LoginRequest();
        request.setEmail("invalid-email");
        request.setPassword("ValidPassword123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));
    }

    @Test
    void shouldReturn401WhenBadCredentialsExceptionIsThrownOnLogin() throws Exception {

        LoginRequest request = new LoginRequest();
        request.setEmail("valid@email.com");
        request.setPassword("ValidPassword123");

        doThrow(new BadCredentialsException("Invalid credentials"))
                .when(authService)
                .authenticateUser(anyString(), anyString());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication Error"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid email or password"));
    }

    /* =========================================================
       ME - NOT AUTHENTICATED
       ========================================================= */

    @Test
    void shouldReturn401WhenNotAuthenticatedOnMe() throws Exception {

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("")); // corpo vazio conforme controller
    }
}
