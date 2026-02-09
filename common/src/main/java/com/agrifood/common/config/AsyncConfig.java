package com.agrifood.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables asynchronous processing across all services
 * Supports concurrent operations for multi-user scalability
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring Boot's default async executor is sufficient for demonstration
    // In production: configure custom ThreadPoolTaskExecutor
}
