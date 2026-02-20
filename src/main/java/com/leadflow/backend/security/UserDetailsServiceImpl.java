package com.leadflow.backend.security;

import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.user.UserRepository;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

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

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                mapAuthorities(user)
        );
    }

    private Collection<? extends GrantedAuthority> mapAuthorities(User user) {

        if (user.getRole() == null || user.getRole().getName() == null) {
            throw new IllegalStateException("User role not configured properly");
        }

        // Spring Security espera ROLE_ prefixado
        String roleName = user.getRole().getName().toUpperCase();

        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }

        return List.of(new SimpleGrantedAuthority(roleName));
    }
}