package com.leadflow.backend.security;

import com.leadflow.backend.entities.user.User;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final String role;

    private final boolean enabled;
    private final boolean accountNonLocked;

    private final LocalDateTime credentialsUpdatedAt;

    public CustomUserDetails(User user) {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (user.getId() == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        if (user.getRole() == null) {
            throw new IllegalStateException("User role cannot be null");
        }

        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = normalizeRole(user.getRole().getName());

        this.enabled = !user.isDeleted();
        this.accountNonLocked = !user.isAccountLocked();
        this.credentialsUpdatedAt = user.getCredentialsUpdatedAt();
    }

    /* ======================================================
       INTERNAL
       ====================================================== */

    private String normalizeRole(String roleName) {

        if (roleName == null || roleName.isBlank()) {
            throw new IllegalStateException("Role name cannot be blank");
        }

        String normalized = roleName.trim().toUpperCase();

        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }

        return normalized;
    }

    /* ======================================================
       CUSTOM GETTERS
       ====================================================== */

    public UUID getId() {
        return id;
    }

    public LocalDateTime getCredentialsUpdatedAt() {
        return credentialsUpdatedAt;
    }

    /* ======================================================
       USERDETAILS IMPLEMENTATION
       ====================================================== */

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Não usamos expiração por tempo
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Controlado via JWT iat
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /* ======================================================
       EQUALS & HASHCODE
       ====================================================== */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomUserDetails that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}