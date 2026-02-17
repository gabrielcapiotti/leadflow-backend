package com.leadflow.backend.service;

import com.leadflow.backend.entities.log.Log;
import com.leadflow.backend.entities.user.User;
import com.leadflow.backend.repository.log.LogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LogService {

    private static final int DEFAULT_RECENT_LIMIT = 10;

    private final LogRepository logRepository;

    public LogService(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    /* ======================================================
       CREATE
       ====================================================== */

    @Transactional
    public void log(User user, String action) {

        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Log action cannot be null or blank");
        }

        Log log = new Log(user, action.trim());
        logRepository.save(log);
    }

    @Transactional
    public void logSystem(String action) {

        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("System log action cannot be null");
        }

        Log log = new Log(null, action.trim());
        logRepository.save(log);
    }

    /* ======================================================
       READ (PAGINADO)
       ====================================================== */

    @Transactional(readOnly = true)
    public Page<Log> getRecentLogs() {

        Pageable pageable = PageRequest.of(0, DEFAULT_RECENT_LIMIT);
        return logRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Log> getLogsByUser(User user, Pageable pageable) {

        return logRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Log> getLogsBetween(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    ) {

        return logRepository.findByCreatedAtBetween(start, end, pageable);
    }
}
