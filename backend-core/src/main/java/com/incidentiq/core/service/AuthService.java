package com.incidentiq.core.service;

import com.incidentiq.core.domain.entity.User;
import com.incidentiq.core.domain.enums.Role;
import com.incidentiq.core.dto.request.LoginRequest;
import com.incidentiq.core.dto.request.RefreshRequest;
import com.incidentiq.core.dto.request.RegisterRequest;
import com.incidentiq.core.dto.response.AuthResponse;
import com.incidentiq.core.exception.ConflictException;
import com.incidentiq.core.exception.UnauthorizedAccessException;
import com.incidentiq.core.repository.jpa.UserRepository;
import com.incidentiq.core.security.JwtTokenProvider;
import com.incidentiq.core.security.SecurityConstants;
import io.jsonwebtoken.Claims;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider, RedisTemplate<String, Object> redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("A user with email " + request.email() + " already exists");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(Role.ENGINEER)
                .build();

        user = userRepository.save(user);
        return issueTokenPair(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return issueTokenPair(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
        if (!jwtTokenProvider.validateRefreshToken(request.refreshToken())) {
            throw new UnauthorizedAccessException("Invalid or expired refresh token");
        }

        Claims claims = jwtTokenProvider.parseRefreshToken(request.refreshToken());
        String userId = claims.getSubject();
        String jti = claims.getId();

        String redisKey = SecurityConstants.REDIS_REFRESH_PREFIX + userId;
        Object storedJti = redisTemplate.opsForValue().get(redisKey);

        if (storedJti == null || !jti.equals(storedJti.toString())) {
            throw new UnauthorizedAccessException("Refresh token has been revoked");
        }

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new UnauthorizedAccessException("User not found"));

        redisTemplate.delete(redisKey);

        return issueTokenPair(user);
    }

    public void logout(String accessToken) {
        if (accessToken == null || !jwtTokenProvider.validateAccessToken(accessToken)) {
            return;
        }

        Claims claims = jwtTokenProvider.parseAccessToken(accessToken);
        String jti = claims.getId();
        String userId = claims.getSubject();

        var remaining = jwtTokenProvider.getRemainingExpiry(claims);
        if (!remaining.isZero()) {
            redisTemplate.opsForValue().set(
                    SecurityConstants.REDIS_BLACKLIST_PREFIX + jti,
                    "blacklisted",
                    remaining.toSeconds(),
                    TimeUnit.SECONDS);
        }

        redisTemplate.delete(SecurityConstants.REDIS_REFRESH_PREFIX + userId);
    }

    private AuthResponse issueTokenPair(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        Claims refreshClaims = jwtTokenProvider.parseRefreshToken(refreshToken);
        var refreshExpiry = jwtTokenProvider.getRefreshTokenExpiry();
        redisTemplate.opsForValue().set(
                SecurityConstants.REDIS_REFRESH_PREFIX + user.getId(),
                refreshClaims.getId(),
                refreshExpiry.toSeconds(),
                TimeUnit.SECONDS);

        return new AuthResponse(accessToken, refreshToken, SecurityConstants.TOKEN_TYPE, 15 * 60);
    }
}
