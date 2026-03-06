package com.leadflow.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableScheduling
@EnableRetry
@EnableCaching
@EnableAspectJAutoProxy
@EnableJpaRepositories(basePackages = "com.leadflow.backend.repository")
@SpringBootApplication(scanBasePackages = {
    "com.leadflow.backend.service.vendor",
    "com.leadflow.backend.service.system"
})
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}