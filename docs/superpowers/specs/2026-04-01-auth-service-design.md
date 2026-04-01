# Auth Service Design

**Date**: 2026-04-01
**Status**: Approved

## 1. Overview

Auth service handles user authentication and authorization for the job portal. It issues JWTs for authenticated users, manages refresh tokens in Redis, and supports email/password login plus Google OAuth.

## 2. Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌──────────────┐
│    Frontend     │────▶│   API Gateway   │────▶│ auth-service │
│    (SPA)       │     │   (port 8080)   │     │  (port 8082) │
└─────────────────┘     └─────────────────┘     └──────┬───────┘
                                                       │
                              ┌─────────────────────────┼─────────────────────────┐
                              │                         │                         │
                              ▼                         ▼                         ▼
                       ┌──────────────┐         ┌──────────────┐         ┌──────────────┐
                       │  PostgreSQL  │         │    Redis     │         │     Google   │
                       │   (users)    │         │(refresh tokens)│        │   OAuth2    │
                       └──────────────┘         └──────────────┘         └──────────────┘
```

## 3. Tech Stack

- **Spring Boot**: 4.0.2
- **Java**: 25
- **Spring Security**: 7.x with OAuth2 Resource Server
- **Database**: PostgreSQL (Spring Data JPA)
- **Cache**: Redis (Spring Data Redis)
- **JWT**: jjwt library
- **Password**: BCrypt

## 4. API Endpoints

| Endpoint | Method | Auth | Description |
|---|---|---|---|
| `/api/auth/register` | POST | No | User registration |
| `/api/auth/login` | POST | No | Email/password login |
| `/api/auth/google` | POST | No | Google OAuth login |
| `/api/auth/refresh` | POST | No | Refresh access token |
| `/api/auth/logout` | POST | JWT | Invalidate refresh token |
| `/api/auth/me` | GET | JWT | Get current user profile |
| `/.well-known/jwks.json` | GET | No | Public keys for token verification |

## 5. Token Configuration

- **Access Token**: JWT, 1 hour expiry, contains userId, email, role
- **Refresh Token**: UUID stored in Redis, 30 day TTL
- **Signing**: RSA 2048-bit keys

## 6. Data Model

### User Entity (PostgreSQL)

| Field | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| email | VARCHAR(255) | unique, indexed |
| passwordHash | VARCHAR(255) | nullable |
| role | ENUM | JOB_SEEKER, EMPLOYER, ADMIN |
| name | VARCHAR(255) | |
| googleId | VARCHAR(255) | nullable |
| emailVerified | BOOLEAN | default false |
| createdAt | TIMESTAMP | |
| updatedAt | TIMESTAMP | |

### Refresh Token (Redis)

- **Key**: `refresh:{token_uuid}`
- **Value**: JSON `{ userId, issuedAt, expiresAt }`
- **TTL**: 30 days

## 7. Security

### Password Requirements
- Minimum 8 characters
- BCrypt hashing with strength 12

### JWT Claims
```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "role": "JOB_SEEKER",
  "iat": 1234567890,
  "exp": 1234571490
}
```

## 8. Component Structure

```
backend/auth-service/
├── build.gradle.kts
├── Dockerfile
├── settings.gradle.kts
├── src/main/java/com/jobportal/authservice/
│   ├── AuthServiceApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── RedisConfig.java
│   │   └── GoogleOAuthConfig.java
│   ├── controller/
│   │   └── AuthController.java
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── JwtService.java
│   │   └── RefreshTokenService.java
│   ├── repository/
│   │   └── UserRepository.java
│   ├── entity/
│   │   └── User.java
│   ├── dto/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── TokenResponse.java
│   │   └── UserResponse.java
│   └── exception/
│       ├── AuthException.java
│       └── GlobalExceptionHandler.java
```

## 9. Docker Integration

```yaml
auth-service:
  build: ./backend/auth-service
  ports:
    - "8082:8082"
  environment:
    SPRING_PROFILES_ACTIVE: docker
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/jobportal
    SPRING_DATASOURCE_USERNAME: postgres
    SPRING_DATASOURCE_PASSWORD: postgres
    SPRING_DATA_REDIS_HOST: redis
    SPRING_DATA_REDIS_PORT: 6379
    GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
    GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET}
  depends_on:
    postgres:
      condition: service_healthy
    redis:
      condition: service_healthy

redis:
  image: redis:7-alpine
  ports:
    - "6379:6379"
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 10s
    timeout: 5s
    retries: 5
```

## 10. Environment Variables

| Variable | Description | Example |
|---|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL | `jdbc:postgresql://postgres:5432/jobportal` |
| `SPRING_DATASOURCE_USERNAME` | DB username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `postgres` |
| `SPRING_DATA_REDIS_HOST` | Redis host | `redis` |
| `SPRING_DATA_REDIS_PORT` | Redis port | `6379` |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID | `xxx.apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | Google OAuth client secret | `GOCSPX-xxx` |
| `JWT_PRIVATE_KEY` | RSA private key (PEM) | `-----BEGIN RSA PRIVATE KEY-----...` |
| `JWT_PUBLIC_KEY` | RSA public key (PEM) | `-----BEGIN PUBLIC KEY-----...` |

## 11. Implementation Order

1. Project scaffold (Gradle, Spring Boot 4.0.2, Docker)
2. User entity and repository
3. JWT service (signing/verification)
4. Refresh token service (Redis)
5. Registration endpoint
6. Login endpoint
7. Token refresh endpoint
8. Google OAuth integration
9. Logout and /me endpoints
10. JWKS endpoint
11. Integration tests
