package com.leadflow.backend.repository.settings;

import com.leadflow.backend.entities.Setting;
import com.leadflow.backend.entities.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettingRepository extends JpaRepository<Setting, UUID> {

    /* ======================================================
       CONSULTA PRINCIPAL
       ====================================================== */

    @EntityGraph(attributePaths = {"user"})
    Optional<Setting> findByUser(User user);

    /* ======================================================
       VALIDAÇÕES
       ====================================================== */

    boolean existsByUser(User user);
}
