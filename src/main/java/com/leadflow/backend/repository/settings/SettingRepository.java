package com.leadflow.backend.repository.settings;

import com.leadflow.backend.entities.Setting;
import com.leadflow.backend.entities.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettingRepository extends JpaRepository<Setting, Long> {

    /* ==========================
       CONSULTAS PRINCIPAIS
       ========================== */

    // Busca configuração ativa por usuário
    Optional<Setting> findByUser(User user);

    /* ==========================
       VALIDAÇÕES / APOIO
       ========================== */

    // Verifica se o usuário já possui configuração
    boolean existsByUser(User user);
}
