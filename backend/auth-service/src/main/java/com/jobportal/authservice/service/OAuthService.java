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
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class OAuthService {

    private final UserRepository userRepository;
    private final OAuthStateService oAuthStateService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

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
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.oAuthStateService = oAuthStateService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
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
        // Validate state
        OAuthStateService.StateData stateData = oAuthStateService.validateAndConsumeState(request.getState());
        if (stateData == null) {
            throw new AuthException("AUTH_OAUTH_STATE_INVALID", "Invalid or expired OAuth state", 400);
        }

        // Exchange code for tokens
        Map<String, String> googleTokens = exchangeGoogleCode(request.getCode(), request.getRedirectUri());

        // Validate idToken and get user info
        Map<String, String> userInfo = getGoogleUserInfo(googleTokens.get("access_token"));

        String googleId = userInfo.get("sub");
        String email = userInfo.get("email");
        String name = userInfo.get("name");

        // Validate email domain
        if (allowedEmailDomain != null && !allowedEmailDomain.isEmpty()) {
            String emailDomain = email.substring(email.lastIndexOf('@') + 1);
            if (!allowedEmailDomain.equalsIgnoreCase(emailDomain)) {
                throw new AuthException("AUTH_EMAIL_DOMAIN_NOT_ALLOWED", "Email must be from " + allowedEmailDomain + " domain", 403);
            }
        }

        // Find or create user
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
        // Validate state
        OAuthStateService.StateData stateData = oAuthStateService.validateAndConsumeState(request.getState());
        if (stateData == null) {
            throw new AuthException("AUTH_OAUTH_STATE_INVALID", "Invalid or expired OAuth state", 400);
        }

        // Exchange code for tokens
        Map<String, String> microsoftTokens = exchangeMicrosoftCode(request.getCode(), request.getRedirectUri());

        // Get user info
        Map<String, String> userInfo = getMicrosoftUserInfo(microsoftTokens.get("access_token"));

        String microsoftId = userInfo.get("oid");
        String email = userInfo.get("preferred_username");
        String name = userInfo.get("name");

        // Validate email domain
        if (allowedEmailDomain != null && !allowedEmailDomain.isEmpty()) {
            String emailDomain = email.substring(email.lastIndexOf('@') + 1);
            if (!allowedEmailDomain.equalsIgnoreCase(emailDomain)) {
                throw new AuthException("AUTH_EMAIL_DOMAIN_NOT_ALLOWED", "Email must be from " + allowedEmailDomain + " domain", 403);
            }
        }

        // Find or create user
        User user = findOrCreateOAuthUser(email, name, null, microsoftId);

        return createTokenResponse(user, userAgent, ip);
    }

    private Map<String, String> exchangeGoogleCode(String code, String redirectUri) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("redirect_uri", redirectUri != null ? redirectUri : googleRedirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://oauth2.googleapis.com/token",
                entity,
                Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || responseBody.containsKey("error")) {
                throw new AuthException("AUTH_OAUTH_EXCHANGE_FAILED", "Failed to exchange code with Google", 502);
            }

            return Map.of(
                "access_token", (String) responseBody.get("access_token"),
                "id_token", (String) responseBody.get("id_token")
            );
        } catch (Exception e) {
            throw new AuthException("AUTH_OAUTH_EXCHANGE_FAILED", "Failed to exchange code with Google: " + e.getMessage(), 502);
        }
    }

    private Map<String, String> getGoogleUserInfo(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v2/userinfo",
                org.springframework.http.HttpMethod.GET,
                entity,
                Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new AuthException("AUTH_OAUTH_EXCHANGE_FAILED", "Failed to get user info from Google", 502);
            }

            return Map.of(
                "sub", (String) body.get("id"),
                "email", (String) body.get("email"),
                "name", (String) body.get("name")
            );
        } catch (Exception e) {
            throw new AuthException("AUTH_OAUTH_EXCHANGE_FAILED", "Failed to get user info from Google: " + e.getMessage(), 502);
        }
    }

    private Map<String, String> exchangeMicrosoftCode(String code, String redirectUri) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", microsoftClientId);
        body.add("client_secret", microsoftClientSecret);
        body.add("redirect_uri", redirectUri != null ? redirectUri : microsoftRedirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                String.format("https://login.microsoftonline.com/%s/oauth2/v2.0/token", microsoftTenant),
                entity,
                Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || responseBody.containsKey("error")) {
                throw new AuthException("AUTH_OAUTH_EXCHANGE_FAILED", "Failed to exchange code with Microsoft", 502);
            }

            return Map.of(
                "access_token", (String) responseBody.get("access_token")
            );
        } catch (Exception e) {
            throw new AuthException("AUTH_OAUTH_EXCHANGE_FAILED", "Failed to exchange code with Microsoft: " + e.getMessage(), 502);
        }
    }

    private Map<String, String> getMicrosoftUserInfo(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                "https://graph.microsoft.com/oidc/userinfo",
                org.springframework.http.HttpMethod.GET,
                entity,
                Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new AuthException("AUTH_OAUTH_EXCHANGE_FAILED", "Failed to get user info from Microsoft", 502);
            }

            return Map.of(
                "oid", (String) body.get("oid"),
                "preferred_username", (String) body.get("preferred_username"),
                "name", (String) body.get("name")
            );
        } catch (Exception e) {
            throw new AuthException("AUTH_OAUTH_EXCHANGE_FAILED", "Failed to get user info from Microsoft: " + e.getMessage(), 502);
        }
    }

    private User findOrCreateOAuthUser(String email, String name, String googleId, String microsoftId) {
        // Try to find existing user by email
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            // Link OAuth identity if not already linked
            if (googleId != null && user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                user = userRepository.save(user);
            } else if (microsoftId != null && user.getMicrosoftId() == null) {
                user.setMicrosoftId(microsoftId);
                user = userRepository.save(user);
            }
            return user;
        }

        // Create new user
        user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setGoogleId(googleId);
        user.setMicrosoftId(microsoftId);
        user.setRole(Role.JOB_SEEKER);
        user.setEmailVerified(true); // OAuth users are email verified
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
