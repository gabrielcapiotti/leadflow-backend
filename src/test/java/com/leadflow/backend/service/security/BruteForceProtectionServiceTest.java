package com.leadflow.backend.service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.leadflow.backend.service.auth.BruteForceProtectionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BruteForceProtectionServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private BruteForceProtectionService service;

    @BeforeEach
    void setUp() {

        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        service = new BruteForceProtectionService(redisTemplate);
    }

    @Test
    void shouldBlockWhenAttemptsExceeded() {

        when(valueOps.get("key")).thenReturn("5");

        boolean blocked = service.isBlocked("key", 5);

        assertThat(blocked).isTrue();
    }
}