package com.leadflow.backend.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.leadflow.backend.multitenancy.service.TenantService;
import com.leadflow.backend.security.jwt.JwtService;
import com.leadflow.backend.security.filter.JwtAuthenticationFilter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.DummyController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.DummyController.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TenantService tenantService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        // Inicializar dados de teste ou configurar o banco de dados, se necessário
    }

    /* ==========================
       DUMMY CONTROLLER
       ========================== */

    @RestController
    @RequestMapping("/dummy")
    @Validated
    static class DummyController {

        @PostMapping("/validation-error")
        public void validateInput(@Valid @RequestBody DummyRequest request) {}

        @GetMapping("/illegal-argument")
        public void throwIllegalArgumentException() {
            throw new IllegalArgumentException("Invalid argument");
        }

        @GetMapping("/illegal-state")
        public void throwIllegalStateException() {
            throw new IllegalStateException("Invalid state");
        }

        @GetMapping("/bad-credentials")
        public void throwBadCredentialsException() {
            throw new BadCredentialsException("Invalid credentials");
        }

        @GetMapping("/access-denied")
        public void throwAccessDeniedException() {
            throw new AccessDeniedException("Access denied");
        }

        @GetMapping("/generic-error")
        public void throwGenericException() {
            throw new RuntimeException("Unexpected error");
        }
    }

    static class DummyRequest {

        @NotBlank(message = "name must not be blank")
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    /* ==========================
       TESTS
       ========================== */

    @Test
    @WithMockUser
    void shouldHandleValidationError() throws Exception {

        mockMvc.perform(
                post("/dummy/validation-error")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Validation Error"))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser
    void shouldHandleIllegalArgumentException() throws Exception {

        mockMvc.perform(get("/dummy/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Business Error"))
                .andExpect(jsonPath("$.message").value("Invalid argument"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser
    void shouldHandleIllegalStateException() throws Exception {

        mockMvc.perform(get("/dummy/illegal-state"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Invalid State"))
                .andExpect(jsonPath("$.message").value("Invalid state"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser
    void shouldHandleBadCredentialsException() throws Exception {

        mockMvc.perform(get("/dummy/bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Authentication Error"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser
    void shouldHandleAccessDeniedException() throws Exception {

        mockMvc.perform(get("/dummy/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Access Denied"))
                .andExpect(jsonPath("$.message")
                        .value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser
    void shouldHandleGenericException() throws Exception {

        mockMvc.perform(get("/dummy/generic-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message")
                        .value("An unexpected error occurred"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
