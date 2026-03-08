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

    public WebConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor =
                Objects.requireNonNull(rateLimitInterceptor, "RateLimitInterceptor must not be null");
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {

        registry.addInterceptor(
                        Objects.requireNonNull(
                                rateLimitInterceptor,
                                "RateLimitInterceptor must not be null"
                        )
                )
                .addPathPatterns("/ai/**");
    }
}