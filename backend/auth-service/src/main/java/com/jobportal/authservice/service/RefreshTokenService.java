package com.jobportal.authservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RefreshTokenService {

    private static final String REFRESH_PREFIX = "refresh:";
    private static final String USER_TOKENS_PREFIX = "user:tokens:";
    private static final String TOKEN_USER_PREFIX = "token:user:";
    private static final long REFRESH_TOKEN_TTL_DAYS = 30;

    private final StringRedisTemplate redisTemplate;
    private final long ttlSeconds;

    public RefreshTokenService(
            StringRedisTemplate redisTemplate,
            @Value("${jwt.refresh-token-ttl-days:30}") long ttlDays) {
        this.redisTemplate = redisTemplate;
        this.ttlSeconds = ttlDays * 24 * 60 * 60;
    }

    public String generateRefreshToken(UUID userId, String userAgent, String ip) {
        String token = UUID.randomUUID().toString();
        String key = REFRESH_PREFIX + token;

        String value = String.format(
            "{\"userId\":\"%s\",\"issuedAt\":\"%s\",\"userAgent\":\"%s\",\"ip\":\"%s\"}",
            userId.toString(),
            Instant.now().toString(),
            userAgent != null ? userAgent : "",
            ip != null ? ip : ""
        );

        redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
        redisTemplate.opsForSet().add(USER_TOKENS_PREFIX + userId, token);
        redisTemplate.opsForValue().set(TOKEN_USER_PREFIX + token, userId.toString(), ttlSeconds, TimeUnit.SECONDS);
        return token;
    }

    public TokenData validateAndGetData(String token) {
        String key = REFRESH_PREFIX + token;
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            // Token expired or never existed — clean up stale references
            String userId = redisTemplate.opsForValue().get(TOKEN_USER_PREFIX + token);
            if (userId != null) {
                redisTemplate.opsForSet().remove(USER_TOKENS_PREFIX + userId, token);
                redisTemplate.delete(TOKEN_USER_PREFIX + token);
            }
            return null;
        }

        // Parse JSON manually (simple parsing for this use case)
        try {
            String userId = extractJsonField(value, "userId");
            String issuedAt = extractJsonField(value, "issuedAt");
            String userAgent = extractJsonField(value, "userAgent");
            String ip = extractJsonField(value, "ip");

            return new TokenData(UUID.fromString(userId), issuedAt, userAgent, ip);
        } catch (IllegalArgumentException | NullPointerException e) {
            // Invalid UUID or null values
            return null;
        }
    }

    public boolean revokeRefreshToken(String token) {
        String key = REFRESH_PREFIX + token;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return false;
        }
        String userId = extractJsonField(value, "userId");
        if (userId != null && !userId.isEmpty()) {
            redisTemplate.opsForSet().remove(USER_TOKENS_PREFIX + userId, token);
        }
        redisTemplate.delete(TOKEN_USER_PREFIX + token);
        Boolean deleted = redisTemplate.delete(key);
        return Boolean.TRUE.equals(deleted);
    }

    public void revokeAllUserTokens(UUID userId) {
        String tokenSetKey = USER_TOKENS_PREFIX + userId;
        var tokens = redisTemplate.opsForSet().members(tokenSetKey);
        if (tokens != null && !tokens.isEmpty()) {
            for (String token : tokens) {
                redisTemplate.delete(REFRESH_PREFIX + token);
                redisTemplate.delete(TOKEN_USER_PREFIX + token);
            }
            redisTemplate.delete(tokenSetKey);
        }
    }

    private String extractJsonField(String json, String fieldName) {
        String search = "\"" + fieldName + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) {
            search = "\"" + fieldName + "\":\"";
            start = json.indexOf(search);
        }
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }

    public record TokenData(UUID userId, String issuedAt, String userAgent, String ip) {}
}
