package com.jobportal.authservice.service;

import com.jobportal.authservice.exception.AuthException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OAuthStateService {

    private static final String STATE_PREFIX = "oauth_state:";
    private static final long STATE_TTL_SECONDS = 600; // 10 minutes
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
            String codeVerifier = extractJsonField(value, "codeVerifier");
            String redirectUri = extractJsonField(value, "redirectUri");

            if (codeVerifier.isEmpty()) {
                return null;
            }

            return new StateData(state, codeVerifier, null, redirectUri);
        } catch (AuthException e) {
            throw e;
        } catch (RuntimeException _) {
            // Invalid JSON format or parsing error
            return null;
        }
    }

    private String generateCodeVerifier() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException _) {
            throw new AuthException("AUTH_INTERNAL_ERROR", "SHA-256 algorithm not available", 500);
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
