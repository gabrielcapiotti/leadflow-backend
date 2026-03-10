package com.leadflow.backend.util;

import java.util.regex.Pattern;

public final class LogSanitizer {

    private static final Pattern BEARER_PATTERN =
            Pattern.compile("Bearer\\s+[A-Za-z0-9-_.]+");

    private static final Pattern PASSWORD_JSON_PATTERN =
            Pattern.compile("(\\\"password\\\"\\s*:\\s*)\\\".*?\\\"");

    private static final Pattern TOKEN_JSON_PATTERN =
            Pattern.compile("(\\\"(?:token|accessToken|refreshToken|jwt)\\\"\\s*:\\s*)\\\".*?\\\"");

    private LogSanitizer() {
    }

    public static String sanitize(String input) {

        if (input == null) {
            return null;
        }

        return TOKEN_JSON_PATTERN
                .matcher(PASSWORD_JSON_PATTERN
                        .matcher(BEARER_PATTERN.matcher(input)
                                .replaceAll("Bearer [REDACTED]"))
                        .replaceAll("$1\"[REDACTED]\""))
                .replaceAll("$1\"[REDACTED]\"");
    }
}
