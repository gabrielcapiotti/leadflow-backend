package com.leadflow.backend.security;

import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.user.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    /* ======================================================
       LOAD USER
       ====================================================== */

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException("Invalid email");
        }

        User user = userRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull(email.trim())
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found")
                );

        validateUser(user);

        // 🔐 Retorna seu próprio UserDetails customizado
        return new CustomUserDetails(user);
    }

    /* ======================================================
       VALIDATION
       ====================================================== */

    private void validateUser(User user) {

        if (user.getDeletedAt() != null) {
            throw new UsernameNotFoundException("User not active");
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalStateException("User password not configured");
        }

        if (user.getRole() == null || user.getRole().getName() == null) {
            throw new IllegalStateException("User role not configured properly");
        }
    }
}