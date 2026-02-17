package com.leadflow.backend.service.user;

import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    /* ======================================================
       READ
       ====================================================== */

    @Transactional(readOnly = true)
    public Page<User> listActiveUsers(Pageable pageable) {
        return userRepository.findByDeletedAtIsNull(pageable);
    }

    @Transactional(readOnly = true)
    public User getActiveById(@NonNull Long userId) {

        return userRepository.findById(userId)
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );
    }

    @Transactional(readOnly = true)
    public User getActiveByEmail(String email) {

        return userRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );
    }

    /* ======================================================
       UPDATE
       ====================================================== */

    @Transactional
    public User updateUser(
            @NonNull Long userId,
            String name,
            String email,
            @NonNull Integer roleId
    ) {

        User user = getActiveById(userId);

        String normalizedEmail = email.trim().toLowerCase();

        if (!user.getEmail().equalsIgnoreCase(normalizedEmail)
                && userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)) {
            throw new IllegalArgumentException("Email already in use");
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Role not found")
                );

        user.setName(name);
        user.setEmail(normalizedEmail);
        user.setRole(role);

        return user;
    }

    /* ======================================================
       SOFT DELETE
       ====================================================== */

    @Transactional
    public void softDelete(@NonNull Long userId) {

        User user = getActiveById(userId);
        user.setDeletedAt(LocalDateTime.now());
    }

    /* ======================================================
       RESTORE
       ====================================================== */

    @Transactional
    public void restore(@NonNull Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        user.setDeletedAt(null);
    }

    public Object listActiveUsers() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listActiveUsers'");
    }
}
