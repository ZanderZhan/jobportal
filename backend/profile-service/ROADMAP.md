# Profile Service Roadmap

## Purpose

`profile-service` should implement the **Profile Context** for the Job Portal backend.

It should own profile data only:

- student profile details beyond authentication identity
- employer profile details beyond authentication identity
- profile completeness state
- resume / CV references
- education, experience, skills, and portfolio links
- public vs private profile visibility rules

It does **not** own:

- authentication, JWT issuance, sessions, or OAuth flows
- role assignment or account verification rules
- job-posting lifecycle management
- application submission or application status workflow
- search indexing or search analytics
- file storage infrastructure itself

Those remain in:

- `auth-service`
- `job-service`
- `application-service`

## Current State

Milestone 3 is now in place:

- the service boundary is defined
- the MVP field sets are defined
- the visibility model is defined
- the Spring Boot module, Dockerfile, and environment configs are present
- Actuator health is available through the standard service setup
- gateway routing can point `/api/profiles/**` at `profile-service`
- student profile bootstrap, read, update, nested sections, and completeness flows are implemented
- employer and public profile behavior is intentionally deferred to Milestone 4 so the current backend stays stable

## Bounded Context Alignment

### Aggregate Roots

- `StudentProfile`
- `EmployerProfile`

### Supporting Concepts

- `ProfileVisibility`
- `ProfileCompleteness`
- `ResumeReference`
- `EducationEntry`
- `ExperienceEntry`
- `Skill`
- `PortfolioLink`
- `EmployerBranding`
- `ContactPreferences`

### Repositories

- `StudentProfileRepository`
- `EmployerProfileRepository`

### Domain Services

- `ProfileBootstrapService`
- `ProfileCompletionService`
- `ProfileVisibilityPolicyService`
- `ResumeReferenceService`

### Domain Events

- `StudentProfileCreated`
- `StudentProfileUpdated`
- `EmployerProfileCreated`
- `EmployerProfileUpdated`

## Service Boundaries

### Reads from Other Services

- `auth-service`
  - trust gateway-forwarded authenticated identity and role claims
  - use auth user id as the profile owner id
  - optionally read minimal identity data during bootstrap only
- `job-service`
  - optionally read minimal employer job metadata for future public-company views only
- `application-service`
  - no hard synchronous dependency in the first release

### Publishes to Other Services

- `application-service`
  - may consume limited applicant profile projections later during employer review flows
- `notification-service`
  - may consume profile completion or profile update events later

## Milestone 1 Decisions

### Auth And Ownership Contract

- `profile-service` must trust the same gateway-forwarded headers already used by `application-service` and `job-service`:
  - `X-User-Id`
  - `X-User-Role`
- accepted role values should stay aligned with `auth-service`:
  - `STUDENT`
  - `HIRING`
  - `ADMIN`
- `auth-service` remains the only source of truth for:
  - identity id
  - email
  - display name
  - role
  - account verification state
  - credentials, JWT issuance, refresh tokens, and OAuth state
- `profile-service` owns only profile data keyed by the authenticated auth user id
- `profile-service` must not create its own role model, credential storage, or parallel account verification rules
- self-service profile writes must always resolve ownership from `X-User-Id`, never from request body fields

### Locked MVP Fields

#### Student Profile

Editable MVP fields:

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

Read-only / derived at the boundary:

- `userId`
- `email`
- `name`
- `role`
- `profileCompleteness`
- `createdAt`
- `updatedAt`

#### Employer Profile

Editable MVP fields:

- `companyName`
- `companyDescription`
- `websiteUrl`
- `logoReference`
- `location`
- `contactName`
- `contactEmail`
- `visibility`

Read-only / derived at the boundary:

- `userId`
- `email`
- `name`
- `role`
- `createdAt`
- `updatedAt`

Fields explicitly deferred from Milestone 1:

- binary resume upload or file storage implementation
- employer verification flags
- profile-driven recommendations
- social graph or networking data
- advanced branding assets beyond a single `logoReference`

### Locked Visibility Model

- use a minimal visibility model in the first release:
  - `PRIVATE`
  - `PUBLIC`
- default visibility should be `PRIVATE` for both student and employer profiles
- self endpoints ignore visibility and remain owner-only
- public endpoints may return a limited projection only when the profile visibility is `PUBLIC`
- public profile responses must never expose:
  - auth credentials or tokens
  - private contact data not explicitly intended for public use
  - `phone`
  - `contactEmail`
  - active `resumeReference`
- employer review access is a separate future policy decision and must not be inferred from `PUBLIC`

### Locked Resume Reference Policy

- resume handling stays reference-only in `profile-service`
- no binary file content should be stored in the service database or sent through service APIs
- the persisted value may be an opaque reference plus metadata, but it must stay storage-provider-agnostic
- `application-service` keeps its own `resumeReference` behavior until a later migration is explicitly designed

### Compatibility Rules

- do not move employer ownership or job lifecycle rules out of `job-service`
- do not introduce a required synchronous dependency from `application-service` to `profile-service` in the first release
- do not add broker publishing in Milestone 1
- reserve `"/api/profiles/**"` as the future gateway route, but do not change gateway runtime config until Milestone 2
- keep the planned implementation stack aligned with the current backend services:
  - Java 25
  - Spring Boot 4.0.2
  - Spring Web
  - Spring Data JPA
  - Spring Validation
  - Spring Actuator
  - PostgreSQL
  - Springdoc OpenAPI

## Core Invariants

- every profile belongs to exactly one authenticated user id
- profile ownership is always enforced from gateway-forwarded identity
- profile data must stay separated from auth credentials and tokens
- public profile reads must respect explicit visibility rules
- employer profile data must not replace job ownership rules in `job-service`
- resume handling in `profile-service` should store references, not binary files
- profile updates must not break existing auth, job, or application flows

## Initial API Scope

### Authenticated Self Endpoints

- `GET /api/profiles/me`
  - fetch the current user's profile
- `PUT /api/profiles/me`
  - update editable profile fields
- `GET /api/profiles/me/completeness`
  - return a profile completeness summary for onboarding and dashboard prompts

### Resume Reference Endpoints

- `PUT /api/profiles/me/resume`
  - create or replace the active resume reference
- `DELETE /api/profiles/me/resume`
  - remove the active resume reference

### Public Read Endpoints

- `GET /api/profiles/students/{userId}`
  - return a limited public student profile view when allowed
- `GET /api/profiles/employers/{userId}`
  - return a limited public employer profile view when allowed

### Employer Review Read Endpoints

- `GET /api/profiles/students/{userId}/review-summary`
  - return an employer-safe applicant profile projection for application review only

## Data Model Direction

### Student Profile Entity

- `student_profiles`
  - `id`
  - `user_id`
  - `headline`
  - `bio`
  - `location`
  - `phone`
  - `resume_reference`
  - `visibility`
  - `job_search_status`
  - `created_at`
  - `updated_at`

### Student Supporting Entities

- `student_profile_skills`
  - `id`
  - `profile_id`
  - `name`
- `student_profile_education`
  - `id`
  - `profile_id`
  - `institution`
  - `degree`
  - `field_of_study`
  - `start_date`
  - `end_date`
- `student_profile_experience`
  - `id`
  - `profile_id`
  - `company`
  - `title`
  - `description`
  - `start_date`
  - `end_date`
- `student_profile_links`
  - `id`
  - `profile_id`
  - `label`
  - `url`

### Employer Profile Entity

- `employer_profiles`
  - `id`
  - `user_id`
  - `company_name`
  - `company_description`
  - `website_url`
  - `logo_reference`
  - `location`
  - `contact_name`
  - `contact_email`
  - `visibility`
  - `created_at`
  - `updated_at`

## Integration Strategy

### Gateway

- route `"/api/profiles/**"` through gateway
- follow the same authenticated request pattern already used by `application-service`

### Auth Integration

- keep `auth-service` as the source of truth for:
  - `id`
  - `email`
  - `name`
  - `role`
  - account verification state
- avoid duplicating auth ownership inside `profile-service`
- bootstrap empty profile records after registration or on first authenticated profile access

### Job Integration

- keep job posting fields and employer ownership rules inside `job-service`
- do not move `company`, `employerId`, or job lifecycle logic into `profile-service`
- allow future employer public profile enrichment without changing job write ownership

### Application Integration

- keep application submission independent from `profile-service` in the first release
- keep `resumeReference` inside `application-service` until there is a deliberate migration plan
- later decide whether applications should snapshot selected profile fields at submission time
- when employers review applicants, keep `application-service` as the main entry point and let it enrich responses with a limited profile projection from `profile-service`

### Search Integration

- no required synchronous integration in the first release
- later evaluate using public employer profile data in search result presentation only

### Message Broker Integration

- do not add broker dependencies in Milestone 1
- introduce profile events only when a real downstream consumer exists
- keep event naming aligned with the existing `message-broker` contract style

## Milestone Status

- Milestone 1: complete in planning and boundary documentation
- Milestone 2: complete as service skeleton and infrastructure wiring
- Milestone 3: complete as the student profile MVP
- Milestone 4: not started
- Milestone 5: not started
- Milestone 6: not started
- Milestone 7: not started

## Milestones

### Milestone 1: Boundary Definition

- confirm exact ownership split between `auth-service` and `profile-service`
- confirm student and employer MVP field sets
- confirm public vs private visibility rules
- confirm resume handling stays reference-only
- lock the gateway/auth header assumptions to the same `X-User-Id` / `X-User-Role` pattern already used elsewhere
- leave runtime implementation to Milestone 2 so existing services remain unchanged

### Milestone 2: Service Skeleton

- create Spring Boot module
- use the same backend stack already used by the other services:
  - Java 25
  - Spring Boot 4.0.2
  - Spring Web
  - Spring Data JPA
  - Spring Validation
  - Spring Actuator
  - PostgreSQL
  - Springdoc OpenAPI
- add Dockerfile and build config
- wire PostgreSQL
- add health endpoint
- register gateway route

### Milestone 3: Student Profile MVP

- implement `StudentProfile` aggregate
- implement `GET /api/profiles/me`
- implement `PUT /api/profiles/me`
- support skills, education, experience, and links
- calculate and expose profile completeness

### Milestone 4: Employer Profile MVP

- implement `EmployerProfile` aggregate
- implement public employer profile reads
- support company description, website, logo reference, and contact metadata
- keep employer verification ownership in `auth-service`

### Milestone 5: Resume Reference Management

- add create, update, and delete flows for resume references
- validate allowed reference shape and metadata
- define how resume references are reused safely by later consumers

### Milestone 6: Cross-Service Hardening

- add resilience around any required auth lookups
- add contract tests for gateway and auth boundary assumptions
- decide whether to publish profile events
- decide whether applications should consume profile snapshots

### Milestone 7: Employer Applicant Profile View

- allow employers to see applicant profile information during application review without moving application ownership out of `application-service`
- add an employer-safe profile projection endpoint such as `GET /api/profiles/students/{userId}/review-summary`
- expose only review-relevant fields:
  - headline
  - bio
  - skills
  - education
  - experience
  - portfolio links
  - active resume reference when policy allows
- enforce that only authorized employer review flows can request this projection
- keep visibility and field-level access policy inside `profile-service`
- let `application-service` call `profile-service` when building employer-facing applicant detail responses
- keep the frontend employer application pages using `application-service` as the main API boundary so the current structure stays intact
- evaluate whether a small applicant snapshot should also be stored in `application-service` for resilience and historical consistency, while keeping `profile-service` as the source of truth for current profile data
- add contract and integration tests for the applicant profile projection flow

## Testing Plan

### Unit Tests

- profile ownership rules
- profile completeness calculation
- visibility policy rules
- resume reference validation

### Integration Tests

- fetch self profile end to end
- update self profile end to end
- read public employer profile end to end
- read employer applicant profile projection end to end

### Contract / Boundary Tests

- gateway routing for `/api/profiles/**`
- auth claim handling contract
- public profile response shape
- applicant review projection contract with `application-service`

## Build / Run Direction

Planned local defaults:

- service port: `8085`
- PostgreSQL-backed persistence
- Docker Compose integration beside existing backend services
- same Gradle and Spring Boot conventions used in the current backend modules

## Out Of Scope For Initial Delivery

- moving identity fields out of `auth-service`
- binary file upload or object storage implementation
- profile-driven personalization or recommendations
- advanced social / networking features
- employer verification workflow changes
- automatic migration of application resume handling

## Recommended Next Step

Start with **Milestone 4**:

- implement `EmployerProfile`
- add public employer profile reads
- support company description, website, logo reference, and contact metadata
- keep employer verification ownership in `auth-service`
