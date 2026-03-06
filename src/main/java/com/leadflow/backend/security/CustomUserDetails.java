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

    private final List<GrantedAuthority> authorities;

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

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("User email cannot be null or blank");
        }

        if (user.getPassword() == null) {
            throw new IllegalArgumentException("User password cannot be null");
        }

        if (user.getRole() == null || user.getRole().getName() == null) {
            throw new IllegalStateException("User role cannot be null");
        }

        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();

        this.authorities = List.of(
                new SimpleGrantedAuthority(normalizeRole(user.getRole().getName()))
        );

        this.enabled = !user.isDeleted();
        this.accountNonLocked = !user.isAccountLocked();
        this.credentialsUpdatedAt = user.getCredentialsUpdatedAt();
    }

    /* ======================================================
       INTERNAL
       ====================================================== */

    private String normalizeRole(String roleName) {

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
        return authorities;
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
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
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
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}