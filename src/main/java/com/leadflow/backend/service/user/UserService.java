package com.leadflow.backend.service.user;

import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

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
    public User getById(UUID id) {
        return userRepository.findById(id)
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
    public User updateUser(UUID id, String name, String email, UUID roleId) {

        User user = getById(id);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Role not found")
                );

        user.setName(name);
        user.setEmail(email.toLowerCase());
        user.setRole(role);

        return userRepository.save(user);
    }

    /* ======================================================
       SOFT DELETE
       ====================================================== */

    @Transactional
    public void softDelete(UUID id) {
        User user = getById(id);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}