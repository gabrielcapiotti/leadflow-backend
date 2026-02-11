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

    /* ==========================
       READ
       ========================== */

    /**
     * Lista todas as roles do sistema.
     * Uso: admin, dropdowns controlados.
     */
    @Transactional(readOnly = true)
    public List<Role> listAll() {
        return roleRepository.findAll();
    }

    /**
     * Busca role por ID.
     * Uso interno / administrativo.
     */
    @Transactional(readOnly = true)
    public Role getById(@NonNull Integer roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Role not found")
                );
    }

    /**
     * Busca role por nome.
     * Uso: autenticação e RBAC.
     */
    @Transactional(readOnly = true)
    public Role getByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() ->
                        new IllegalArgumentException("Role not found")
                );
    }

    /* ==========================
       SEED / BOOTSTRAP
       ========================== */

    /**
     * Garante que uma role exista.
     * ⚠️ Deve ser usado APENAS em seed / bootstrap.
     */
    @Transactional
    public Role getOrCreate(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() ->
                        roleRepository.save(new Role(name))
                );
    }
}
