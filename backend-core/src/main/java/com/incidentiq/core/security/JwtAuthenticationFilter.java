package com.incidentiq.core.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, RedisTemplate<String, Object> redisTemplate) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);
        if (token != null && jwtTokenProvider.validateAccessToken(token)) {
            Claims claims = jwtTokenProvider.parseAccessToken(token);

            String jti = claims.getId();
            if (isBlacklisted(jti)) {
                log.warn("JWT rejected — token blacklisted: jti={}", jti);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            String tokenType = claims.get(SecurityConstants.CLAIM_TYPE, String.class);
            if (!SecurityConstants.TOKEN_TYPE_ACCESS.equals(tokenType)) {
                log.warn("JWT rejected — invalid token type");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            String userId = claims.getSubject();
            String role = claims.get(SecurityConstants.CLAIM_ROLE, String.class);

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            var authentication = new UsernamePasswordAuthenticationToken(
                    UUID.fromString(userId), null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("JWT authenticated: userId={}, role={}", userId, role);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return header.substring(SecurityConstants.TOKEN_PREFIX.length());
        }
        return null;
    }

    private boolean isBlacklisted(String jti) {
        if (jti == null) return false;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(SecurityConstants.REDIS_BLACKLIST_PREFIX + jti));
        } catch (Exception e) {
            return false;
        }
    }
}
