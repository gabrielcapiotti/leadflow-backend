package com.leadflow.backend.config;

import com.leadflow.backend.security.RateLimitInterceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Objects;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final BillingValidationInterceptor billingValidationInterceptor;

    public WebConfig(RateLimitInterceptor rateLimitInterceptor,
                    BillingValidationInterceptor billingValidationInterceptor) {
        this.rateLimitInterceptor =
                Objects.requireNonNull(rateLimitInterceptor, "RateLimitInterceptor must not be null");
        this.billingValidationInterceptor =
                Objects.requireNonNull(billingValidationInterceptor, "BillingValidationInterceptor must not be null");
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {

        // Register billing validation interceptor for all API routes
        registry.addInterceptor(
                        Objects.requireNonNull(
                                billingValidationInterceptor,
                                "BillingValidationInterceptor must not be null"
                        )
                )
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/auth/**",
                        "/api/v1/health/**",
                        "/api/v1/public/**"
                );

        // Register rate limit interceptor for AI routes
        registry.addInterceptor(
                        Objects.requireNonNull(
                                rateLimitInterceptor,
                                "RateLimitInterceptor must not be null"
                        )
                )
                .addPathPatterns("/ai/**");
    }
}