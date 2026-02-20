package com.leadflow.backend.service.auth;

import com.leadflow.backend.entities.Tenant;
import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.multitenancy.context.TenantContext;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;
import com.leadflow.backend.repository.tenant.TenantRepository;

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
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tenantRepository = tenantRepository;
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

        Tenant tenant = tenantRepository
                .findBySchemaNameIgnoreCaseAndDeletedAtIsNull(schema)
                .orElseThrow(() ->
                        new IllegalStateException("Tenant not found: " + schema)
                );

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
                userRole,
                tenant
        );

        logger.info("User registered for tenant {} with email {}",
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

        validateTenantAccess(user, schema);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        logger.info("User authenticated for tenant {}: {}",
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

        String schema = resolveCurrentTenantSchema();
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        validateTenantAccess(user, schema);

        return user;
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

    private void validateTenantAccess(User user, String schema) {

        if (user.getTenant() == null ||
                user.getTenant().getSchemaName() == null ||
                !user.getTenant().getSchemaName().equalsIgnoreCase(schema)) {

            throw new IllegalArgumentException("Invalid credentials");
        }
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