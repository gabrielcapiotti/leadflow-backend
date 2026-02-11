package com.leadflow.backend.repository.log;

import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.entities.log.Log;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LogRepository extends JpaRepository<Log, Long> {

    /* ==========================
       CONSULTAS POR USUÁRIO
       ========================== */

    // Logs de um usuário específico (mais recentes primeiro)
    List<Log> findByUserOrderByCreatedAtDesc(User user);

    /* ==========================
       AUDITORIA / RELATÓRIOS
       ========================== */

    // Logs por intervalo de datas
    List<Log> findByCreatedAtBetween(
        LocalDateTime start,
        LocalDateTime end
    );

    // Logs de um usuário em intervalo de datas
    List<Log> findByUserAndCreatedAtBetween(
        User user,
        LocalDateTime start,
        LocalDateTime end
    );

    /* ==========================
       MONITORAMENTO DO SISTEMA
       ========================== */

    // Últimos N logs do sistema (fixo em 10)
    List<Log> findTop10ByOrderByCreatedAtDesc();
}
