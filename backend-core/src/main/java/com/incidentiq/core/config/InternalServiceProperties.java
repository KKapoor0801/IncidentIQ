package com.incidentiq.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds internal service configuration properties from the {@code internal-service.*} namespace.
 */
@ConfigurationProperties(prefix = "internal-service")
public record InternalServiceProperties(
        String aiCallbackToken
) {
}
