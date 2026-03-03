package com.leadflow.backend.integration;

import com.leadflow.backend.security.RateLimitService;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.leadflow.backend.exception.GlobalExceptionHandler;

@WebMvcTest
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
class FlywayIntegrationTest extends FlywayTestBase {

    @MockBean
    private RateLimitService rateLimitService;

    

}