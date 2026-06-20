package com.incidentiq.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds JWT configuration properties from the {@code jwt.*} namespace.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String accessTokenSecret,
        String refreshTokenSecret,
        long accessTokenExpiryMinutes,
        long refreshTokenExpiryDays
) {
}
