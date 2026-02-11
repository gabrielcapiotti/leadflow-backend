package com.leadflow.backend.service.auth;

import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MockBean
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            BCryptPasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /* ==========================
       REGISTRO (APENAS USER)
       ========================== */

    public User registerUser(String name, String email, String password) {
        logger.info("Attempting to register user with email: {}", email);

        // Não permitir email duplicado ativo
        if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            logger.warn("Email already in use: {}", email);
            throw new IllegalArgumentException("Email already in use");
        }

        logger.info("Email is available: {}", email);

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() ->
                        new IllegalStateException("Default role USER not found")
                );

        User user = new User(
                name,
                email,
                passwordEncoder.encode(password),
                userRole
        );

        logger.info("User successfully registered with email: {}", email);

        return userRepository.save(user);
    }

    /* ==========================
       LOGIN
       ========================== */

    public User authenticateUser(String email, String password) {

        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return user;
    }

    public User findByEmail(String email) {
        logger.info("Searching for user with email: {}", email);
        return userRepository.findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> {
                logger.error("User not found with email: {}", email);
                return new IllegalArgumentException("User not found");
            });
    }

}
