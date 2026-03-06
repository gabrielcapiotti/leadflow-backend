package com.leadflow.backend.security.exception;

import java.io.Serial;

/**
 * Exception thrown when an authentication or authorization failure occurs.
 *
 * Typical scenarios:
 * - Missing or invalid JWT token
 * - Expired token
 * - Unauthorized access to protected resources
 * - Invalid authentication credentials
 *
 * This exception is expected to be translated by the global
 * exception handler into an HTTP 401 response.
 */
public class UnauthorizedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_MESSAGE = "Unauthorized access";

    public UnauthorizedException() {
        super(DEFAULT_MESSAGE);
    }

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnauthorizedException(Throwable cause) {
        super(DEFAULT_MESSAGE, cause);
    }
}