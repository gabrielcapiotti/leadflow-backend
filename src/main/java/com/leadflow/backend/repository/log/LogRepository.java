package com.leadflow.backend.repository.log;

import com.leadflow.backend.entities.log.Log;
import com.leadflow.backend.entities.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<Log, Long> {

    /* ======================================================
       CONSULTAS POR USUÁRIO
       ====================================================== */

    @EntityGraph(attributePaths = {"user"})
    Page<Log> findByUserOrderByCreatedAtDesc(
            User user,
            Pageable pageable
    );

    /* ======================================================
       AUDITORIA / RELATÓRIOS
       ====================================================== */

    Page<Log> findByCreatedAtBetween(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    Page<Log> findByUserAndCreatedAtBetween(
            User user,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    /* ======================================================
       MONITORAMENTO DO SISTEMA
       ====================================================== */

    Page<Log> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Mantido por compatibilidade (opcional)
    List<Log> findTop10ByOrderByCreatedAtDesc();
}
