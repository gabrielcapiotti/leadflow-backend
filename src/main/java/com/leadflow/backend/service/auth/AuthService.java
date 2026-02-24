package com.leadflow.backend.service.auth;

import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger logger =
            LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /* ======================================================
       REGISTER
       ====================================================== */

    @Transactional
    public User registerUser(String name, String email, String password) {

        validateInput(name, email, password);

        String schema = resolveCurrentTenantSchema();
        String normalizedEmail = email.trim().toLowerCase();

        if (userRepository
                .existsByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)) {

            throw new IllegalArgumentException("Email already in use");
        }

        Role userRole = roleRepository
                .findByNameIgnoreCase("ROLE_USER")
                .orElseThrow(() ->
                        new IllegalStateException("Default role ROLE_USER not found")
                );

        User user = new User(
                name.trim(),
                normalizedEmail,
                passwordEncoder.encode(password),
                userRole
        );

        logger.info("User registered in schema {} with email {}",
                schema, normalizedEmail);

        return userRepository.save(user);
    }

    /* ======================================================
       LOGIN
       ====================================================== */

    @Transactional(readOnly = true)
    public User authenticateUser(String email, String password) {

        if (email == null || email.isBlank() ||
            password == null || password.isBlank()) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String schema = resolveCurrentTenantSchema();
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid credentials")
                );

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        logger.info("User authenticated in schema {}: {}",
                schema, normalizedEmail);

        return user;
    }

    /* ======================================================
       FIND BY EMAIL
       ====================================================== */

    @Transactional(readOnly = true)
    public User findByEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }

        String normalizedEmail = email.trim().toLowerCase();

        return userRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );
    }

    /* ======================================================
       INTERNAL
       ====================================================== */

    private String resolveCurrentTenantSchema() {

        String schema = TenantContext.getTenant();

        if (schema == null || schema.isBlank()) {
            throw new IllegalStateException(
                    "No tenant defined in TenantContext"
            );
        }

        return schema.trim().toLowerCase();
    }

    private void validateInput(String name, String email, String password) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }

        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException(
                    "Password must contain at least 6 characters"
            );
        }
    }
}