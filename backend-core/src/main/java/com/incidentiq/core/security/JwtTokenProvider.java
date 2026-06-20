package com.incidentiq.core.security;

import com.incidentiq.core.config.JwtProperties;
import com.incidentiq.core.domain.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final Duration accessTokenExpiry;
    private final Duration refreshTokenExpiry;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.accessKey = Keys.hmacShaKeyFor(jwtProperties.accessTokenSecret().getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(jwtProperties.refreshTokenSecret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiry = Duration.ofMinutes(jwtProperties.accessTokenExpiryMinutes());
        this.refreshTokenExpiry = Duration.ofDays(jwtProperties.refreshTokenExpiryDays());
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(SecurityConstants.CLAIM_EMAIL, user.getEmail())
                .claim(SecurityConstants.CLAIM_ROLE, user.getRole().name())
                .claim(SecurityConstants.CLAIM_TYPE, SecurityConstants.TOKEN_TYPE_ACCESS)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenExpiry)))
                .signWith(accessKey)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(SecurityConstants.CLAIM_TYPE, SecurityConstants.TOKEN_TYPE_REFRESH)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTokenExpiry)))
                .signWith(refreshKey)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(accessKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Claims parseRefreshToken(String token) {
        return Jwts.parser()
                .verifyWith(refreshKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateAccessToken(String token) {
        try {
            parseAccessToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            parseRefreshToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Duration getRemainingExpiry(Claims claims) {
        Instant expiry = claims.getExpiration().toInstant();
        Duration remaining = Duration.between(Instant.now(), expiry);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    public Duration getRefreshTokenExpiry() {
        return refreshTokenExpiry;
    }
}
