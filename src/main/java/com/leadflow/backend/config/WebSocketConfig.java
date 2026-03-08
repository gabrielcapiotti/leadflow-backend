package com.leadflow.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Objects;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {

        MessageBrokerRegistry safeRegistry =
                Objects.requireNonNull(registry, "MessageBrokerRegistry must not be null");

        safeRegistry.enableSimpleBroker("/topic", "/queue");
        safeRegistry.setApplicationDestinationPrefixes("/app");
        safeRegistry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {

        StompEndpointRegistry safeRegistry =
                Objects.requireNonNull(registry, "StompEndpointRegistry must not be null");

        safeRegistry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}