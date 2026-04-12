# Profile Service

A planned backend microservice for profile-related APIs in the Job Portal application.

## Current State

Milestone 3 is now in place:

- the Spring Boot module exists
- Gradle, Docker, and environment configuration follow the same conventions as the other backend services
- PostgreSQL and JPA wiring are ready
- Actuator health is exposed
- the gateway can route `/api/profiles/**` to this service
- student profile bootstrap, read, update, and completeness flows are implemented

Employer profile flows, public profile reads, and resume management remain for later milestones.

## Planned Scope

`profile-service` will own profile data only:

- student profile details beyond authentication identity
- employer profile details beyond authentication identity
- profile completeness state
- resume / CV references
- education, experience, skills, and portfolio links
- public vs private profile visibility rules

It will not own:

- authentication, JWT issuance, sessions, or OAuth flows
- role assignment or account verification rules
- job-posting lifecycle management
- application submission or application status workflow
- search indexing or search analytics
- binary file storage infrastructure

## Technology Stack

The service uses the same backend stack and conventions already present in this repo:

- Java 25
- Spring Boot 4.0.2
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Actuator
- PostgreSQL
- OpenAPI/Swagger
- Docker

## Boundary Contract

- The future gateway route is `/api/profiles/**`.
- The service should trust the same forwarded auth context already used by `application-service`:
  - `X-User-Id`
  - `X-User-Role`
- Role values must stay aligned with `auth-service`:
  - `STUDENT`
  - `HIRING`
  - `ADMIN`
- `auth-service` remains the source of truth for identity, email, display name, role, verification state, and all auth flows.
- `profile-service` stores profile data keyed by the authenticated auth user id only.
- Self-service profile writes must derive ownership from headers, not from request bodies.

## Locked MVP Fields

### Student Profile

Editable fields:

- `headline`
- `bio`
- `location`
- `phone`
- `visibility`
- `jobSearchStatus`
- `skills`
- `education`
- `experience`
- `portfolioLinks`
- `resumeReference`

Read-only or derived fields:

- `userId`
- `email`
- `name`
- `role`
- `profileCompleteness`
- `createdAt`
- `updatedAt`

### Employer Profile

Editable fields:

- `companyName`
- `companyDescription`
- `websiteUrl`
- `logoReference`
- `location`
- `contactName`
- `contactEmail`
- `visibility`

Read-only or derived fields:

- `userId`
- `email`
- `name`
- `role`
- `createdAt`
- `updatedAt`

## Visibility Model

Milestone 1 locks a minimal model to avoid conflict with the rest of the backend:

- `PRIVATE`
- `PUBLIC`

Rules:

- Default visibility should be `PRIVATE`.
- Self endpoints remain owner-only regardless of visibility.
- Public endpoints may return only a limited projection when visibility is `PUBLIC`.
- Public responses must not expose `phone`, `contactEmail`, auth fields, or `resumeReference`.
- Employer review access is a separate future policy and should not depend on public visibility alone.

## Planned API Surface

- `GET /api/profiles/me`
- `PUT /api/profiles/me`
- `GET /api/profiles/me/completeness`
- `PUT /api/profiles/me/resume`
- `DELETE /api/profiles/me/resume`
- `GET /api/profiles/students/{userId}`
- `GET /api/profiles/employers/{userId}`
- `GET /api/profiles/students/{userId}/review-summary`

## Resume Policy

- Resume handling stays reference-only.
- No binary file data should be stored inside `profile-service`.
- `application-service` keeps its own `resumeReference` handling until a deliberate migration is designed.

## Milestone Status

- Milestone 1: complete
- Milestone 2: complete
- Milestone 3: complete
- Milestone 4-7: not started

## Next Step

Milestone 4 should add the employer profile MVP and public employer profile reads without changing the locked service boundaries.
