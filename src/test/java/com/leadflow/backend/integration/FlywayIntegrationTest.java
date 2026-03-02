package com.leadflow.backend.integration;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.leadflow.backend.exception.GlobalExceptionHandler;

@WebMvcTest
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
class FlywayIntegrationTest extends FlywayTestBase {

    // seus testes aqui

}