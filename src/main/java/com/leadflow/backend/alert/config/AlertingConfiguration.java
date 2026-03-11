package com.leadflow.backend.alert.config;

import com.leadflow.backend.alert.service.SlackAlertService;
import com.leadflow.backend.alert.service.EmailAlertService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for alerting services
 *
 * Automatically detects and configures:
 * - Slack alerting (if slack.webhook.url is set)
 * - Email alerting (if alert.email.to is set)
 */
@Slf4j
@Configuration
public class AlertingConfiguration {

    /**
     * Slack alert service bean (optional, only if webhook URL configured)
     */
    @Bean
    public SlackAlertService slackAlertService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {

        SlackAlertService service = new SlackAlertService(
                null, // injected later from properties
                null,
                null,
                restTemplate,
                objectMapper
        );

        if (service.isConfigured()) {
            log.info("Slack alerting service initialized");
        } else {
            log.debug("Slack alerting service not configured (slack.webhook.url not set)");
        }

        return service;
    }

    /**
     * Email alert service bean (optional, only if email address configured)
     */
    @Bean
    @ConditionalOnBean(JavaMailSender.class)
    public EmailAlertService emailAlertService(
            JavaMailSender mailSender,
            TemplateEngine templateEngine) {

        EmailAlertService service = new EmailAlertService(
                mailSender,
                templateEngine,
                null, // injected later from properties
                null,
                null
        );

        if (service.isConfigured()) {
            log.info("Email alerting service initialized");
        } else {
            log.debug("Email alerting service not configured (alert.email.to not set)");
        }

        return service;
    }

}