package com.leadflow.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.RedisPassword;

import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Objects;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String host;

    @Value("${spring.redis.port:6379}")
    private int port;

    @Value("${spring.redis.password:}")
    private String password;

    @Value("${spring.redis.database:0}")
    private int database;

    @Value("${spring.redis.timeout:2000}")
    private long timeoutMillis;

    /* ======================================================
       CONNECTION FACTORY
       ====================================================== */

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {

        RedisStandaloneConfiguration standaloneConfig =
                new RedisStandaloneConfiguration();

        /*
         * Garante valor seguro e não-nulo para host
         */
        String safeHost = (host == null || host.isBlank())
                ? "localhost"
                : host;

        standaloneConfig.setHostName(Objects.requireNonNull(safeHost));
        standaloneConfig.setPort(port);
        standaloneConfig.setDatabase(database);

        if (password != null && !password.isBlank()) {
            standaloneConfig.setPassword(RedisPassword.of(password));
        }

        /*
         * Timeout mínimo seguro
         */
        Duration commandTimeout = Duration.ofMillis(Math.max(timeoutMillis, 100));
        Duration shutdownTimeout = Duration.ofMillis(100);

        /*
         * Garantia explícita de non-null
         */
        Duration safeCommandTimeout = Objects.requireNonNull(commandTimeout);
        Duration safeShutdownTimeout = Objects.requireNonNull(shutdownTimeout);

        LettuceClientConfiguration clientConfig =
                LettuceClientConfiguration.builder()
                        .commandTimeout(safeCommandTimeout)
                        .shutdownTimeout(safeShutdownTimeout)
                        .build();

        return new LettuceConnectionFactory(
                standaloneConfig,
                clientConfig
        );
    }

    /* ======================================================
       STRING REDIS TEMPLATE
       ====================================================== */

    @Bean
    public StringRedisTemplate stringRedisTemplate(
            RedisConnectionFactory factory
    ) {

        RedisConnectionFactory safeFactory =
                Objects.requireNonNull(
                        factory,
                        "RedisConnectionFactory must not be null"
                );

        StringRedisTemplate template =
                new StringRedisTemplate(safeFactory);

        StringRedisSerializer serializer = new StringRedisSerializer();

        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();

        return template;
    }
}