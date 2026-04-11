package com.jobportal.authservice.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    public TokenBlacklistService(StringRedisTemplate redisTemplate, JwtService jwtService) {
        this.redisTemplate = redisTemplate;
        this.jwtService = jwtService;
    }

    /**
     * Blacklist an access token by its jti.
     * The token remains blacklisted until its natural expiration time.
     */
    public void blacklistToken(String jti) {
        String key = BLACKLIST_PREFIX + jti;
        // Store with TTL equal to access token expiry to auto-expire
        redisTemplate.opsForValue().set(key, "1", jwtService.getAccessTokenExpirySeconds(), TimeUnit.SECONDS);
    }

    /**
     * Check if a token's jti is blacklisted.
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isEmpty()) {
            return false;
        }
        String key = BLACKLIST_PREFIX + jti;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
