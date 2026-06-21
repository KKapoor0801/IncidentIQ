package com.incidentiq.core.security;

public final class SecurityConstants {

    private SecurityConstants() {}

    public static final String TOKEN_TYPE = "Bearer";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_TYPE = "typ";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    public static final String REDIS_BLACKLIST_PREFIX = "auth:jwt:blacklist:";
    public static final String REDIS_REFRESH_PREFIX = "auth:refresh:";
}
