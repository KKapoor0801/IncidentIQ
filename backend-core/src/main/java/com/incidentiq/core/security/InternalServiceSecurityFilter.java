package com.incidentiq.core.security;

import com.incidentiq.core.config.InternalServiceProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalServiceSecurityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InternalServiceSecurityFilter.class);
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final InternalServiceProperties properties;

    public InternalServiceSecurityFilter(InternalServiceProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = request.getHeader(INTERNAL_TOKEN_HEADER);

        if (token == null || token.isBlank()) {
            log.warn("Internal request rejected — missing token: uri={}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Missing X-Internal-Token header\"}");
            response.setContentType("application/json");
            return;
        }

        if (!token.equals(properties.aiCallbackToken())) {
            log.warn("Internal request rejected — invalid token: uri={}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\"Invalid internal service token\"}");
            response.setContentType("application/json");
            return;
        }

        log.debug("Internal request authenticated: uri={}", request.getRequestURI());
        filterChain.doFilter(request, response);
    }
}
