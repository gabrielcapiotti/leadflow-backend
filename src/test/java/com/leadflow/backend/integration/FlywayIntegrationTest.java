package com.leadflow.backend.integration;

import com.leadflow.backend.security.RateLimitService;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.leadflow.backend.exception.GlobalExceptionHandler;

@WebMvcTest
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
class FlywayIntegrationTest extends FlywayTestBase {

    @MockitoBean
    private RateLimitService rateLimitService;

    

}