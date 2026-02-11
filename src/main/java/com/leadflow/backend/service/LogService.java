package com.leadflow.backend.service;

import com.leadflow.backend.entities.log.Log;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.log.LogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LogService {

    private final LogRepository logRepository;

    public LogService(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    /* ==========================
       CREATE
       ========================== */

    /**
     * Registra uma ação associada a um usuário.
     */
    @Transactional
    public void log(User user, String action) {
        Log log = new Log(user, action);
        logRepository.save(log);
    }

    /**
     * Registra uma ação do sistema (sem usuário).
     */
    @Transactional
    public void logSystem(String action) {
        Log log = new Log(null, action);
        logRepository.save(log);
    }

    /* ==========================
       READ (ADMIN / AUDITORIA)
       ========================== */

    /**
     * Retorna os últimos logs do sistema.
     */
    @Transactional(readOnly = true)
    public List<Log> getRecentLogs() {
        return logRepository.findTop10ByOrderByCreatedAtDesc();
    }

    /**
     * Retorna logs de um usuário específico.
     */
    @Transactional(readOnly = true)
    public List<Log> getLogsByUser(User user) {
        return logRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Retorna logs por intervalo de datas.
     */
    @Transactional(readOnly = true)
    public List<Log> getLogsBetween(
            LocalDateTime start,
            LocalDateTime end
    ) {
        return logRepository.findByCreatedAtBetween(start, end);
    }
}
