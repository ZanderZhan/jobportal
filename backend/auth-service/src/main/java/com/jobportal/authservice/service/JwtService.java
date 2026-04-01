package com.jobportal.authservice.service;

import com.jobportal.authservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
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

    @Value("${jwt.access-token-expiry:3600}")
    private long accessTokenExpirySeconds;

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private static final String KEY_ID = "1";

    @PostConstruct
    public void init() {
        if (privateKeyPem != null && !privateKeyPem.isEmpty()) {
            try {
                String privateKeyContent = privateKeyPem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
                byte[] decoded = Base64.getDecoder().decode(privateKeyContent);
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                privateKey = keyFactory.generatePrivate(keySpec);

                String publicKeyContent = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
                byte[] publicDecoded = Base64.getDecoder().decode(publicKeyContent);
                X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicDecoded);
                publicKey = keyFactory.generatePublic(publicKeySpec);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                Logger logger = LoggerFactory.getLogger(JwtService.class);
                logger.warn("Failed to load JWT keys: {}", e.getMessage());
                privateKey = null;
                publicKey = null;
            }
        }
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirySeconds * 1000);

        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("name", user.getName())
            .claim("role", user.getRole().name())
            .issuedAt(now)
            .expiration(expiry)
            .id(KEY_ID)
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

    public long getAccessTokenExpiry() {
        return accessTokenExpirySeconds;
    }

    public boolean hasValidKeys() {
        return privateKey != null && publicKey != null;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
