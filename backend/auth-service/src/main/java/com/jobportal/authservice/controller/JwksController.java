package com.jobportal.authservice.controller;

import com.jobportal.authservice.service.JwtService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.Map;

@RestController
public class JwksController {

    private final JwtService jwtService;

    public JwksController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getJwks() {
        if (!jwtService.hasValidKeys()) {
            return ResponseEntity.notFound().build();
        }

        // Get the public key modulus and exponent
        var key = jwtService.getPublicKey();

        try {
            var rsaKey = (java.security.interfaces.RSAPublicKey) key;
            byte[] modulusBytes = rsaKey.getModulus().toByteArray();
            byte[] exponentBytes = rsaKey.getPublicExponent().toByteArray();

            // Remove leading zero byte if present (for unsigned representation)
            byte[] modulus = modulusBytes.length > 1 && modulusBytes[0] == 0
                ? java.util.Arrays.copyOfRange(modulusBytes, 1, modulusBytes.length)
                : modulusBytes;

            String n = Base64.getUrlEncoder().withoutPadding().encodeToString(modulus);
            String e = Base64.getUrlEncoder().withoutPadding().encodeToString(exponentBytes);

            Map<String, Object> jwk = Map.of(
                "kty", "RSA",
                "use", "sig",
                "alg", "RS256",
                "kid", "1",
                "n", n,
                "e", e
            );

            return ResponseEntity.ok(Map.of("keys", new Object[]{jwk}));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
