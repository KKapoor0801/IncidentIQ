package com.incidentiq.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds CORS configuration properties from the {@code cors.*} namespace.
 */
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(
        String allowedOrigins
) {
}
