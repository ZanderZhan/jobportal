package com.jobportal.authservice.service;

import com.jobportal.authservice.dto.*;
import com.jobportal.authservice.entity.Role;
import com.jobportal.authservice.entity.User;
import com.jobportal.authservice.exception.AuthException;
import com.jobportal.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${allowed-email-domain:}")
    private String allowedEmailDomain;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Validate email domain if restriction is set
        if (allowedEmailDomain != null && !allowedEmailDomain.isEmpty()) {
            String emailDomain = extractEmailDomain(request.email());
            if (!allowedEmailDomain.equalsIgnoreCase(emailDomain)) {
                throw new AuthException("AUTH_EMAIL_DOMAIN_NOT_ALLOWED", "Email must be from " + allowedEmailDomain + " domain", 403);
            }
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new AuthException("AUTH_EMAIL_EXISTS", "Email already registered", 409);
        }

        // Validate password strength
        if (!STRONG_PASSWORD_PATTERN.matcher(request.password()).matches()) {
            throw new AuthException("AUTH_WEAK_PASSWORD", "Password must be at least 8 characters with at least one letter and one number", 400);
        }

        // Create user
        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        user.setRole(Role.JOB_SEEKER);
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        return UserResponse.fromEntity(savedUser);
    }

    @Transactional
    public TokenResponse login(LoginRequest request, String userAgent, String ip) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new AuthException("AUTH_INVALID_CREDENTIALS", "Invalid email or password", 401));

        if (!user.getEnabled()) {
            throw new AuthException("AUTH_ACCOUNT_DISABLED", "Account is disabled", 403);
        }

        if (user.getPasswordHash() == null) {
            throw new AuthException("AUTH_INVALID_CREDENTIALS", "This account uses OAuth login", 401);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException("AUTH_INVALID_CREDENTIALS", "Invalid email or password", 401);
        }

        return createTokenResponse(user, userAgent, ip);
    }

    @Transactional
    public TokenResponse refreshToken(String refreshToken, String userAgent, String ip) {
        RefreshTokenService.TokenData tokenData = refreshTokenService.validateAndGetData(refreshToken);

        if (tokenData == null) {
            throw new AuthException("AUTH_TOKEN_NOT_FOUND", "Refresh token not found or expired", 401);
        }

        User user = userRepository.findById(tokenData.userId())
            .orElseThrow(() -> new AuthException("AUTH_USER_NOT_FOUND", "User not found", 404));

        if (!user.getEnabled()) {
            throw new AuthException("AUTH_ACCOUNT_DISABLED", "Account is disabled", 403);
        }

        // Revoke old refresh token (token rotation)
        refreshTokenService.revokeRefreshToken(refreshToken);

        return createTokenResponse(user, userAgent, ip);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            refreshTokenService.revokeRefreshToken(refreshToken);
        }
    }

    public UserResponse getCurrentUser(String token) {
        var claims = jwtService.validateAndGetClaims(token);
        UUID userId = UUID.fromString(claims.getSubject());

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("AUTH_USER_NOT_FOUND", "User not found", 404));

        return UserResponse.fromEntity(user);
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

    private String extractEmailDomain(String email) {
        int atIndex = email.lastIndexOf('@');
        if (atIndex == -1) return "";
        return email.substring(atIndex + 1);
    }
}
