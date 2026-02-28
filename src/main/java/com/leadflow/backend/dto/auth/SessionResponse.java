package com.leadflow.backend.dto.auth;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an active user session (device).
 */
public record SessionResponse(

        /**
         * Unique identifier of the session.
         */
        UUID sessionId,

        /**
         * IP address used during session creation.
         */
        String ipAddress,

        /**
         * User-Agent string of the client.
         */
        String userAgent,

        /**
         * Timestamp when the session was created (UTC).
         */
        Instant createdAt,

        /**
         * Indicates whether this session corresponds
         * to the currently authenticated device.
         */
        boolean current

) {}