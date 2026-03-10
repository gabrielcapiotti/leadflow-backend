package com.leadflow.backend.service.user;

import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.entities.vendor.Vendor;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;
import com.leadflow.backend.service.vendor.UsageService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsageService usageService;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            UsageService usageService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.usageService = usageService;
    }

    /* ======================================================
       READ
       ====================================================== */

    @Transactional(readOnly = true)
    public Page<User> listActiveUsers(Pageable pageable) {

        if (pageable == null) {
            throw new IllegalArgumentException("Pageable cannot be null");
        }

        // Não é necessário chamar orElse() pois o método findAll já retorna um Page<User>
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {

        if (id == null) {
            throw new IllegalArgumentException("User id cannot be null");
        }

        return userRepository.findById(id)
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );
    }

    @Transactional(readOnly = true)
    public User getActiveByEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }

        return userRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull(email.trim())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );
    }

    /* ======================================================
       UPDATE
       ====================================================== */

    @Transactional
    public User updateUser(UUID id, String name, String email, UUID roleId) {

        if (id == null || roleId == null) {
            throw new IllegalArgumentException("Id and roleId cannot be null");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }

        User user = getById(id);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Role not found")
                );

        String normalizedEmail = email.trim().toLowerCase();

        boolean emailExists = userRepository
                .existsByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail);

        if (!user.getEmail().equalsIgnoreCase(normalizedEmail) && emailExists) {
            throw new IllegalArgumentException("Email already in use");
        }

        // ✅ Usa métodos de domínio
        user.changeName(name.trim());
        user.changeEmail(normalizedEmail);
        user.changeRole(role);

        return userRepository.save(user);
    }

    /* ======================================================
       SOFT DELETE
       ====================================================== */

    @Transactional
    public void softDelete(UUID id) {

        User user = getById(id);

        if (user.isDeleted()) {
            return; // idempotência
        }

        user.softDelete();

        userRepository.save(user);
    }

    @Transactional
    public void createAdminUser(Vendor vendor, String email) {

        if (vendor == null || email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)) {
            return;
        }

        Role role = roleRepository
                .findByNameIgnoreCase("ROLE_ADMIN")
                .or(() -> roleRepository.findByNameIgnoreCase("ROLE_USER"))
                .orElseThrow(() -> new IllegalStateException("No default role found"));

        String temporaryPassword = "tmp-" + UUID.randomUUID();

        usageService.consumeUser(vendor.getId());

        User admin = new User(
                vendor.getNomeVendedor() != null ? vendor.getNomeVendedor() : normalizedEmail,
                normalizedEmail,
                passwordEncoder.encode(temporaryPassword),
                role
        );

        userRepository.save(admin);
    }
}