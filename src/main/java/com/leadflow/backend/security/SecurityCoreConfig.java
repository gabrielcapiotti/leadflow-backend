package com.leadflow.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityCoreConfig {

    private final int bcryptStrength;

    public SecurityCoreConfig(
            @Value("${security.password.bcrypt.strength:10}")
            int bcryptStrength
    ) {

        if (bcryptStrength < 4 || bcryptStrength > 31) {
            throw new IllegalArgumentException(
                    "Invalid BCrypt strength. Allowed range: 4–31"
            );
        }

        this.bcryptStrength = bcryptStrength;
    }

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(bcryptStrength);
    }

}