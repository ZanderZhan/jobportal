# Application Service Roadmap

## Purpose

`application-service` implements the **Application Context** from the architecture report.

It owns the application lifecycle only:

- application submission
- application status tracking
- employer review actions
- student withdrawal
- application history / timeline
- publication of application domain events for notifications

It does **not** own:

- authentication or role issuance
- full job-posting management
- notification delivery

Those remain in:

- `auth-service`
- `job-service`
- `notification-service`

## Bounded Context Alignment

### Aggregate Root

- `Application`

### Supporting Concepts

- `ApplicationStatus`
- `ResumeReference`
- `ApplicantSnapshot`
- `SubmissionTimestamp`
- `ReviewDecision`
- `ApplicationTimelineEntry`

### Repositories

- `ApplicationRepository`

### Domain Services

- `ApplicationSubmissionService`
- `ApplicationStatusPolicyService`
- `ApplicationEligibilityService`

### Domain Events

- `ApplicationSubmitted`
- `ApplicationWithdrawn`
- `ApplicationStatusUpdated`

## Service Boundaries

### Reads from Other Services

- `auth-service`
  - validate user context from gateway-forwarded claims
  - optionally fetch student profile snapshot if needed
- `job-service`
  - validate job existence
  - validate that job is eligible for applications
  - fetch minimal job metadata only

### Publishes to Other Services

- `notification-service`
  - consume asynchronous application events only

## Core Invariants

- a student can apply only to an eligible published job
- duplicate applications for the same student and job are blocked
- application creation is owned only by the student actor
- status transitions must follow an explicit lifecycle policy
- a student may withdraw only when policy allows it
- employers may update only applications tied to their own jobs
- application history must be traceable over time

## Initial API Scope

### Student Endpoints

- `POST /api/applications`
  - submit an application
- `GET /api/applications`
  - list applications for the authenticated student
- `GET /api/applications/{id}`
  - get application details if authorized
- `PUT /api/applications/{id}/withdraw`
  - withdraw an application if allowed

### Employer Endpoints

- `GET /api/jobs/{jobId}/applications`
  - list applications for a job owned by the employer
- `PUT /api/applications/{id}/status`
  - update application status

## Proposed Status Model

Use the report-aligned statuses:

- `SUBMITTED`
- `UNDER_REVIEW`
- `INTERVIEW`
- `HIRED`
- `REJECTED`
- `WITHDRAWN`

Initial transition policy:

- create -> `SUBMITTED`
- `SUBMITTED` -> `UNDER_REVIEW`, `WITHDRAWN`, `REJECTED`
- `UNDER_REVIEW` -> `INTERVIEW`, `HIRED`, `REJECTED`, `WITHDRAWN`
- `INTERVIEW` -> `HIRED`, `REJECTED`
- terminal states:
  - `HIRED`
  - `REJECTED`
  - `WITHDRAWN`

## Data Model Direction

### Primary Entity

- `applications`
  - `id`
  - `student_id`
  - `job_id`
  - `resume_reference`
  - `status`
  - `submitted_at`
  - `updated_at`
  - optional snapshots:
    - `job_title_snapshot`
    - `employer_id_snapshot`
    - `student_email_snapshot`

### History Entity

- `application_timeline_entries`
  - `id`
  - `application_id`
  - `old_status`
  - `new_status`
  - `changed_by`
  - `reason`
  - `created_at`

## Integration Strategy

### Gateway

- route `"/api/applications/**"` through gateway
- trust forwarded authenticated identity and role claims

### Auth Integration

- avoid synchronous auth calls on every request when claims are already present
- use auth lookups only for missing profile snapshot data if needed

### Job Integration

- synchronous validation before application creation
- reject drafts / closed jobs at the boundary
- use a small ACL adapter to map job metadata into an internal eligibility model

### Notification Integration

- publish application events after persistence succeeds
- keep notification delivery asynchronous and outside the submission transaction

## Milestones

### Milestone 1: Service Skeleton

- create Spring Boot module
- add Dockerfile and build config
- wire PostgreSQL
- add health endpoint
- register gateway route

### Milestone 2: Domain Model And Submission Flow

- implement `Application` aggregate
- implement submission endpoint
- prevent duplicate applications
- validate job eligibility via `job-service`
- persist application and timeline entry

### Milestone 3: Student Views And Withdrawal

- list applications for current student
- fetch application details
- implement withdrawal policy
- add student-facing tests

### Milestone 4: Employer Review Workflow

- list applications by job
- implement status update endpoint
- enforce ownership and valid transitions
- record history for every change

### Milestone 5: Events, Notifications, And Hardening

- publish `ApplicationSubmitted`
- publish `ApplicationStatusUpdated`
- publish `ApplicationWithdrawn`
- add resilience around `job-service` calls
- add contract tests and integration tests

## Testing Plan

### Unit Tests

- status transition policy
- duplicate application prevention
- withdrawal rules
- employer ownership checks

### Integration Tests

- submit application end to end
- update status end to end
- withdraw application end to end

### Contract / Boundary Tests

- gateway routing
- `job-service` validation contract
- event payload contract for notification consumers

## Build / Run Direction

Planned local defaults:

- service port: `8084`
- PostgreSQL-backed persistence
- Docker Compose integration beside existing services

## Out Of Scope For Initial Delivery

- CV file upload storage service
- ranking or recommendation of applications
- in-app employer notes UI
- analytics dashboards
- notification rendering logic

## Recommended Next Step

Start with **Milestone 1** only:

- scaffold `application-service`
- define the base package structure
- set the service port and database config
- register gateway routing
- do not implement business endpoints yet
