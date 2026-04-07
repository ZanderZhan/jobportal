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

If `status` is omitted, the service defaults it to `ACTIVE`.

## Docker

```bash
docker build -t search-service .
```

## Testing

```bash
./gradlew test
```
