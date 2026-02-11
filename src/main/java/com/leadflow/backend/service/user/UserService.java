package com.leadflow.backend.service.user;

import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.user.RoleRepository;
import com.leadflow.backend.repository.user.UserRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    /* ==========================
       READ
       ========================== */

    /**
     * Lista apenas usuários ativos (não deletados)
     */
    public List<User> listActiveUsers() {
        return userRepository.findByDeletedAtIsNull();
    }

    /**
     * Busca usuário ativo por ID
     */
    public User getActiveById(@NonNull Long userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    /**
     * Busca usuário ativo por email
     */
    public User getActiveByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    /* ==========================
       UPDATE
       ========================== */

    /**
     * Atualiza dados básicos do usuário
     */
    @Transactional
    public User updateUser(
            @NonNull Long userId,
            String name,
            String email,
            @NonNull Integer roleId
    ) {
        User user = getActiveById(userId);

        if (!user.getEmail().equals(email)
                && userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            throw new IllegalArgumentException("Email already in use");
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        user.setName(name);
        user.setEmail(email);
        user.setRole(role);

        return user;
    }

    /* ==========================
       DELETE (SOFT)
       ========================== */

    /**
     * Soft delete de usuário
     */
    @Transactional
    public void softDelete(@NonNull Long userId) {
        User user = getActiveById(userId);
        user.setDeletedAt(LocalDateTime.now());
    }

    /* ==========================
       RESTORE
       ========================== */

    /**
     * Restaura usuário deletado
     */
    @Transactional
    public void restore(@NonNull Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setDeletedAt(null);
    }
}
