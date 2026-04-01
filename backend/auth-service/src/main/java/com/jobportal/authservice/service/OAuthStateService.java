package com.jobportal.authservice.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OAuthStateService {

    private static final String STATE_PREFIX = "oauth_state:";
    private static final long STATE_TTL_SECONDS = 600; // 10 minutes

    private final StringRedisTemplate redisTemplate;

    public OAuthStateService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public StateData generateState(String provider, String redirectUri) {
        String state = UUID.randomUUID().toString();
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);

        String key = STATE_PREFIX + state;
        String value = String.format(
            "{\"provider\":\"%s\",\"codeVerifier\":\"%s\",\"redirectUri\":\"%s\",\"createdAt\":\"%s\"}",
            provider,
            codeVerifier,
            redirectUri != null ? redirectUri : "",
            Instant.now().toString()
        );

        redisTemplate.opsForValue().set(key, value, STATE_TTL_SECONDS, TimeUnit.SECONDS);

        return new StateData(state, codeVerifier, codeChallenge, redirectUri);
    }

    public StateData validateAndConsumeState(String state) {
        String key = STATE_PREFIX + state;
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null;
        }

        // Delete the state (one-time use)
        redisTemplate.delete(key);

        try {
            String provider = extractJsonField(value, "provider");
            String codeVerifier = extractJsonField(value, "codeVerifier");
            String redirectUri = extractJsonField(value, "redirectUri");
            String createdAt = extractJsonField(value, "createdAt");

            return new StateData(state, codeVerifier, null, redirectUri);
        } catch (Exception e) {
            return null;
        }
    }

    private String generateCodeVerifier() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateCodeChallenge(String codeVerifier) throws RuntimeException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate code challenge", e);
        }
    }

    private String extractJsonField(String json, String fieldName) {
        String search = "\"" + fieldName + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }

    public record StateData(String state, String codeVerifier, String codeChallenge, String redirectUri) {}
}
