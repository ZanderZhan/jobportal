package com.jobportal.authservice.service;

import com.jobportal.authservice.dto.OAuthCallbackRequest;
import com.jobportal.authservice.dto.TokenResponse;
import com.jobportal.authservice.dto.UserResponse;
import com.jobportal.authservice.entity.Role;
import com.jobportal.authservice.entity.User;
import com.jobportal.authservice.exception.AuthException;
import com.jobportal.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class OAuthService {

    private final UserRepository userRepository;
    private final OAuthStateService oAuthStateService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final RestTemplate restTemplate;

    @Value("${google.client.id:}")
    private String googleClientId;

    @Value("${google.client.secret:}")
    private String googleClientSecret;

    @Value("${google.client.redirect-uri:}")
    private String googleRedirectUri;

    @Value("${microsoft.client.id:}")
    private String microsoftClientId;

    @Value("${microsoft.client.secret:}")
    private String microsoftClientSecret;

    @Value("${microsoft.client.redirect-uri:}")
    private String microsoftRedirectUri;

    @Value("${microsoft.tenant:common}")
    private String microsoftTenant;

    @Value("${allowed-email-domain:}")
    private String allowedEmailDomain;

    public OAuthService(
            UserRepository userRepository,
            OAuthStateService oAuthStateService,
            RefreshTokenService refreshTokenService,
            JwtService jwtService,
            RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.oAuthStateService = oAuthStateService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.restTemplate = restTemplate;
    }

    public String getGoogleAuthorizationUrl(String state, String codeChallenge) {
        return String.format(
            "https://accounts.google.com/o/oauth2/v2/auth?client_id=%s&redirect_uri=%s&response_type=code&scope=openid%%20email%%20profile&state=%s&code_challenge=%s&code_challenge_method=S256",
            googleClientId,
            googleRedirectUri,
            state,
            codeChallenge
        );
    }

    @Transactional
    public TokenResponse handleGoogleCallback(OAuthCallbackRequest request, String userAgent, String ip) {
        OAuthStateService.StateData stateData = oAuthStateService.validateAndConsumeState(request.state());
        if (stateData == null) {
            throw new AuthException("AUTH_OAUTH_STATE_INVALID", "Invalid or expired OAuth state", 400);
        }

        // Use the redirect URI bound to the OAuth state and reject mismatches
        String stateRedirectUri = stateData.redirectUri();
        if (request.redirectUri() != null && !request.redirectUri().equals(stateRedirectUri)) {
            throw new AuthException("AUTH_OAUTH_REDIRECT_URI_MISMATCH", "Invalid redirect URI for OAuth state", 400);
        }

        Map<String, String> googleTokens = exchangeCode(
            request.code(),
            stateRedirectUri,
            googleClientId,
            googleClientSecret,
            googleRedirectUri,
            "https://oauth2.googleapis.com/token"
        );

        Map<String, String> userInfo = getGoogleUserInfo(googleTokens.get("access_token"));

        String googleId = userInfo.get("sub");
        String email = userInfo.get("email");
        String name = userInfo.get("name");

        validateEmailDomain(email);

        User user = findOrCreateOAuthUser(email, name, googleId, null);
        return createTokenResponse(user, userAgent, ip);
    }

    public String getMicrosoftAuthorizationUrl(String state, String codeChallenge) {
        return String.format(
            "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize?client_id=%s&redirect_uri=%s&response_type=code&scope=openid%%20email%%20profile&state=%s&code_challenge=%s&code_challenge_method=S256",
            microsoftTenant,
            microsoftClientId,
            microsoftRedirectUri,
            state,
            codeChallenge
        );
    }

    @Transactional
    public TokenResponse handleMicrosoftCallback(OAuthCallbackRequest request, String userAgent, String ip) {
        OAuthStateService.StateData stateData = oAuthStateService.validateAndConsumeState(request.state());
        if (stateData == null) {
            throw new AuthException("AUTH_OAUTH_STATE_INVALID", "Invalid or expired OAuth state", 400);
        }

        Map<String, String> microsoftTokens = exchangeCode(
            request.code(),
            request.redirectUri(),
            microsoftClientId,
            microsoftClientSecret,
            microsoftRedirectUri,
            String.format("https://login.microsoftonline.com/%s/oauth2/v2.0/token", microsoftTenant)
        );

        Map<String, String> userInfo = getMicrosoftUserInfo(microsoftTokens.get("access_token"));

        String microsoftId = userInfo.get("oid");
        String email = userInfo.get("preferred_username");
        String name = userInfo.get("name");

        validateEmailDomain(email);

        User user = findOrCreateOAuthUser(email, name, null, microsoftId);
        return createTokenResponse(user, userAgent, ip);
    }

    private Map<String, String> exchangeCode(
            String code,
            String redirectUri,
            String clientId,
            String clientSecret,
            String defaultRedirectUri,
            String tokenUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri != null ? redirectUri : defaultRedirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                tokenUrl,
                entity,
                (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || responseBody.containsKey("error")) {
                throw new AuthException("AUTH_OAUTH_EXCHANGE_FAILED", "Failed to exchange code with OAuth provider", 502);
            }

            return Map.of(
                "access_token", String.valueOf(responseBody.get("access_token"))
            );
        } catch (HttpClientErrorException e) {
            throw new AuthException("AUTH_OAUTH_EXCHANGE_FAILED",
                "Failed to exchange code with OAuth provider: " + e.getStatusCode(), 502);
        }
    }

    private Map<String, String> getGoogleUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v2/userinfo",
                org.springframework.http.HttpMethod.GET,
                entity,
                (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new AuthException("AUTH_OAUTH_EXCHANGE_FAILED", "Failed to get user info from Google", 502);
            }

            return Map.of(
                "sub", String.valueOf(body.get("id")),
                "email", String.valueOf(body.get("email")),
                "name", String.valueOf(body.get("name"))
            );
        } catch (HttpClientErrorException e) {
            throw new AuthException("AUTH_OAUTH_EXCHANGE_FAILED",
                "Failed to get user info from Google: " + e.getStatusCode(), 502);
        }
    }

    private Map<String, String> getMicrosoftUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "https://graph.microsoft.com/oidc/userinfo",
                org.springframework.http.HttpMethod.GET,
                entity,
                (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new AuthException("AUTH_OAUTH_EXCHANGE_FAILED", "Failed to get user info from Microsoft", 502);
            }

            return Map.of(
                "oid", String.valueOf(body.get("oid")),
                "preferred_username", String.valueOf(body.get("preferred_username")),
                "name", String.valueOf(body.get("name"))
            );
        } catch (HttpClientErrorException e) {
            throw new AuthException("AUTH_OAUTH_EXCHANGE_FAILED",
                "Failed to get user info from Microsoft: " + e.getStatusCode(), 502);
        }
    }

    private void validateEmailDomain(String email) {
        if (allowedEmailDomain != null && !allowedEmailDomain.isEmpty()) {
            int atIndex = email.lastIndexOf('@');
            if (atIndex == -1) {
                throw new AuthException("AUTH_EMAIL_DOMAIN_NOT_ALLOWED", "Invalid email format", 403);
            }
            String emailDomain = email.substring(atIndex + 1);
            if (!allowedEmailDomain.equalsIgnoreCase(emailDomain)) {
                throw new AuthException("AUTH_EMAIL_DOMAIN_NOT_ALLOWED",
                    "Email must be from " + allowedEmailDomain + " domain", 403);
            }
        }
    }

    private User findOrCreateOAuthUser(String email, String name, String googleId, String microsoftId) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            if (googleId != null && user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                return userRepository.save(user);
            } else if (microsoftId != null && user.getMicrosoftId() == null) {
                user.setMicrosoftId(microsoftId);
                return userRepository.save(user);
            }
            return user;
        }

        user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setGoogleId(googleId);
        user.setMicrosoftId(microsoftId);
        user.setRole(Role.JOB_SEEKER);
        user.setEmailVerified(true);
        user.setEnabled(true);

        return userRepository.save(user);
    }

    private TokenResponse createTokenResponse(User user, String userAgent, String ip) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.generateRefreshToken(user.getId(), userAgent, ip);

        return new TokenResponse(
            accessToken,
            refreshToken,
            jwtService.getAccessTokenExpiry(),
            UserResponse.fromEntity(user)
        );
    }
}
