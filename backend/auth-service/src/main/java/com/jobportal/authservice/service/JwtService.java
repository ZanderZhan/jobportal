package com.jobportal.authservice.service;

import com.jobportal.authservice.entity.User;
import com.jobportal.authservice.exception.AuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${jwt.private-key:}")
    private String privateKeyPem;

    @Value("${jwt.public-key:}")
    private String publicKeyPem;

    @Value("${jwt.private-key-file:}")
    private String privateKeyFile;

    @Value("${jwt.public-key-file:}")
    private String publicKeyFile;

    @Value("${jwt.access-token-expiry:3600}")
    private long accessTokenExpirySeconds;

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private static final String KEY_ID = "1";

    public static final String CLAIM_JTI = "jti";

    @PostConstruct
    public void init() {
        String privateKeyContent = resolveKeyContent(privateKeyPem, privateKeyFile, "private");
        String publicKeyContent = resolveKeyContent(publicKeyPem, publicKeyFile, "public");

        if (privateKeyContent.isEmpty() || publicKeyContent.isEmpty()) {
            throw new IllegalStateException(
                "JWT keys not configured. Set jwt.private-key / jwt.private-key-file " +
                "and jwt.public-key / jwt.public-key-file properties.");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(privateKeyContent);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = keyFactory.generatePrivate(keySpec);

            byte[] publicDecoded = Base64.getDecoder().decode(publicKeyContent);
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicDecoded);
            publicKey = keyFactory.generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to parse JWT keys: " + e.getMessage(), e);
        }
    }

    private String resolveKeyContent(String directValue, String filePath, String keyType) {
        String content = directValue != null ? directValue.trim() : "";
        if (content.isEmpty() && filePath != null && !filePath.isBlank()) {
            try {
                org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(filePath.trim());
                if (resource.exists()) {
                    content = resource.getContentAsString(java.nio.charset.StandardCharsets.UTF_8).trim();
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to read JWT " + keyType + " key from file: " + e.getMessage(), e);
            }
        }
        if (content.startsWith("-----BEGIN")) {
            content = content.replace("-----BEGIN " + (keyType.equals("private") ? "PRIVATE" : "PUBLIC") + " KEY-----", "")
                .replace("-----END " + (keyType.equals("private") ? "PRIVATE" : "PUBLIC") + " KEY-----", "")
                .replaceAll("\\s", "");
        }
        return content;
    }

    public String generateAccessToken(User user) {
        if (privateKey == null) {
            throw new AuthException("TOKEN_ERROR", "JWT signing keys not available", 500);
        }
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirySeconds * 1000);

        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("name", user.getName())
            .claim("role", user.getRole().name())
            .claim(CLAIM_JTI, jti)
            .issuedAt(now)
            .expiration(expiry)
            .header().add("kid", KEY_ID).and()
            .signWith(privateKey)
            .compact();
    }

    public Claims validateAndGetClaims(String token) {
        return Jwts.parser()
            .verifyWith(publicKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = validateAndGetClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    public String getJtiFromToken(String token) {
        Claims claims = validateAndGetClaims(token);
        return claims.get(CLAIM_JTI, String.class);
    }

    public long getAccessTokenExpirySeconds() {
        return accessTokenExpirySeconds;
    }

    public boolean hasValidKeys() {
        return privateKey != null && publicKey != null;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
