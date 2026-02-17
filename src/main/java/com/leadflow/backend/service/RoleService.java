package com.leadflow.backend.service;

import com.leadflow.backend.entities.user.Role;
import com.leadflow.backend.repository.user.RoleRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /* ======================================================
       READ
       ====================================================== */

    @Transactional(readOnly = true)
    public List<Role> listAll() {
        return roleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Role getById(@NonNull Integer roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Role not found")
                );
    }

    @Transactional(readOnly = true)
    public Role getByName(String name) {

        String normalized = normalize(name);

        return roleRepository.findByNameIgnoreCase(normalized)
                .orElseThrow(() ->
                        new IllegalArgumentException("Role not found")
                );
    }

    /* ======================================================
       SEED / BOOTSTRAP
       ====================================================== */

    @Transactional
    public Role getOrCreate(String name) {

        String normalized = normalize(name);

        return roleRepository.findByNameIgnoreCase(normalized)
                .orElseGet(() ->
                        roleRepository.save(new Role(normalized))
                );
    }

    /* ======================================================
       INTERNAL
       ====================================================== */

    private String normalize(String name) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Role name cannot be null or blank");
        }

        return name.trim().toUpperCase();
    }
}
