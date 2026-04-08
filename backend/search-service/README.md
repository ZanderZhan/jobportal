# Search Service

A dedicated backend microservice for search-related APIs in the Job Portal application.

## Current Scope

The service exposes a search-focused API at `/api/search/jobs` and currently proxies job discovery requests to `job-service`. This gives the backend a stable search boundary now, while leaving room for a later move to indexing, ranking, caching, or full-text search without changing external clients again.

## Technology Stack

- Java 25
- Spring Boot 4.0.2
- Spring Web
- Spring Validation
- Spring Actuator
- OpenAPI/Swagger

## Run Locally

```bash
./gradlew bootRun
```

The service runs at `http://localhost:8083`.

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8083` | Service port |
| `services.job-service.url` | `http://localhost:8081` | Upstream job-service base URL |

## API

- `GET /api/search/jobs`

Supported query parameters:

- `title`
- `company`
- `location`
- `employmentType`
- `salaryMin`
- `salaryMax`
- `status`
- `page`
- `size`
- `sort`

Public contract for Milestone 1:

- Frontend and other clients should use `/api/search/jobs` as the public job discovery endpoint.
- `status` defaults to `ACTIVE` when omitted, so public search stays focused on active jobs unless a caller explicitly requests another status.
- `page` defaults to `0`.
- `size` defaults to `20`.
- `sort` defaults to `createdAt,desc`.
- `page`, `size`, and `sort` are passed through to the upstream search query as part of the stable boundary contract for this milestone.
- Upstream communication failures are returned as `502 Bad Gateway` with a structured error body.

Milestone 2 behavior:

- Text filters are normalized by trimming leading and trailing whitespace, collapsing repeated internal whitespace, and dropping empty values.
- `employmentType` and `status` are normalized case-insensitively before validation and forwarding.
- Public search still defaults `status` to `ACTIVE` when it is omitted or blank.
- `page` must be greater than or equal to `0`.
- `size` must be greater than or equal to `1` and is capped at `100` by default.
- `sort` must follow `field,direction`, where direction is `asc` or `desc`.
- Supported sort fields are `title`, `company`, `location`, `employmentType`, `salaryMin`, `salaryMax`, `status`, `createdAt`, and `updatedAt`.
- `salaryMin` and `salaryMax` must be greater than or equal to `0`, and `salaryMin` cannot be greater than `salaryMax`.
- Invalid search input is returned as `400 Bad Request` with a structured error body.
- Metrics are emitted for request count, request latency, and upstream error count through Micrometer/Actuator.

Milestone 3 behavior:

- Default search ordering is now search-aware instead of simple passthrough ordering.
- Exact and partial text matches are ranked ahead of weaker matches, with recency used as a secondary signal.
- Ranking is applied on the default search sort using an upstream candidate window before the final page slice is returned.
- Frequent normalized search requests are cached in-memory for a short TTL to reduce repeated upstream calls.
- Search facets are available at `GET /api/search/jobs/facets` for `location`, `company`, and `employmentType`.
- Upstream calls use explicit connect/read timeouts.
- Search retries upstream failures a limited number of times before failing.
- A simple circuit-breaker opens after repeated upstream failures and returns `503 Service Unavailable` while the upstream is cooling down.

## Docker

```bash
docker build -t search-service .
```

## Testing

```bash
./gradlew test
```
