package com.leadflow.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.Objects;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(
            @Value("${security.cors.allowed-origins:https://seusite.com}")
            String allowedOrigins
    ) {

        String safeOrigins =
                Objects.requireNonNullElse(allowedOrigins, "https://seusite.com");

        String[] parsedOrigins = Arrays.stream(safeOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);

        this.allowedOrigins =
                parsedOrigins.length == 0
                        ? new String[]{"https://seusite.com"}
                        : parsedOrigins;
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {

        CorsRegistry safeRegistry =
                Objects.requireNonNull(registry, "CorsRegistry must not be null");

        String[] safeOrigins =
                Objects.requireNonNull(allowedOrigins, "Allowed origins must not be null");

        safeRegistry.addMapping("/**")
                .allowedOrigins(safeOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}