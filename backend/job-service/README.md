# Job Service

A microservice for managing job postings in the Job Portal application.

## Technology Stack

- **Java 25**
- **Spring Boot 4.0.2**
- **Spring Data JPA**
- **PostgreSQL**
- **OpenAPI/Swagger**
- **Docker**

## Prerequisites

- Java 25+
- Gradle 8.x+
- PostgreSQL 17+ (or Docker)
- Docker & Docker Compose (optional)

## Getting Started

### Option 1: Local Development

1. **Start PostgreSQL** (if not using Docker):
   ```bash
   # Ensure PostgreSQL is running on localhost:5432
   # Create database: jobportal
   ```

2. **Build and run the application**:
   ```bash
   ./gradlew bootRun
   ```

3. The service runs at `http://localhost:8081`

### Option 2: Docker Compose

From the project root directory (`jobportal/`):

```bash
docker-compose up -d
```

This starts both PostgreSQL and the Job Service.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/jobs` | Create a new job |
| GET | `/jobs/{id}` | Get job by ID |
| GET | `/jobs` | List all jobs (paginated) |
| PUT | `/jobs/{id}` | Update a job |
| DELETE | `/jobs/{id}` | Delete a job |
| GET | `/jobs/search` | Search jobs with filters |

### Search Parameters

- `title` - Filter by job title (partial match)
- `company` - Filter by company name
- `location` - Filter by location
- `employmentType` - FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP
- `salaryMin` / `salaryMax` - Filter by salary range
- `status` - DRAFT, ACTIVE, CLOSED
- `page`, `size`, `sort` - Pagination

### Example Requests

**Create a Job:**
```bash
curl -X POST http://localhost:8081/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Software Engineer",
    "description": "Build amazing products",
    "company": "Tech Corp",
    "location": "San Francisco, CA",
    "employmentType": "FULL_TIME",
    "salaryMin": 80000,
    "salaryMax": 120000,
    "salaryCurrency": "USD",
    "requirements": ["Java", "Spring Boot"],
    "status": "ACTIVE"
  }'
```

**Search Jobs:**
```bash
curl "http://localhost:8081/jobs/search?title=Engineer&location=San Francisco"
```

**Get All Jobs (Paginated):**
```bash
curl "http://localhost:8081/jobs?page=0&size=10&sort=createdAt,desc"
```

## API Documentation

- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8081/api-docs

## Configuration

### application.yml

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8081 | Service port |
| `spring.datasource.url` | jdbc:postgresql://localhost:5432/jobportal | Database URL |
| `spring.jpa.hibernate.ddl-auto` | update | Schema management |

### Environment Variables (Docker)

- `SPRING_PROFILES_ACTIVE` - Set to `docker` for containerized deployment
- `SPRING_DATASOURCE_URL` - Override database URL
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password

## Seeding Search Demo Data

Use the curated demo seed to populate the database with a realistic search dataset that is large enough for UI, ranking, and filter validation.

```bash
# Make executable (first time only)
chmod +x scripts/seed-jobs.sh

# Append the curated dataset
./scripts/seed-jobs.sh

# Replace existing jobs first
./scripts/seed-jobs.sh --replace
```

What the demo dataset covers:
- overlapping frontend, backend, platform, security, data, product, QA, and support titles
- repeated companies and locations for company, location, and relevance checks
- `ACTIVE`, `DRAFT`, and `CLOSED` statuses
- `FULL_TIME`, `PART_TIME`, `CONTRACT`, and `INTERNSHIP` employment types
- `EUR`, `GBP`, and `USD` salary currencies
- remote, hybrid, and on-site style locations encoded in the `location` field
- salary ranges from internship-level to senior and staff-level compensation

Files:
- `scripts/search-demo-jobs.json` - the curated dataset
- `scripts/search-demo-qa-checklist.md` - a lightweight manual QA guide for search validation

> **Note:** The Job Service must be running before executing the seed script.
>
> **Team setup:** After pulling these files, each teammate should run `./scripts/seed-jobs.sh --replace` in `backend/job-service/` on their own machine if they want the same demo dataset locally. The seeded jobs are not stored in Git, only the seed assets and documentation are.

## Project Structure

```
backend/job-service/
├── scripts/
│   ├── seed-jobs.sh                  # Demo data seeding script
│   ├── search-demo-jobs.json         # Curated search demo dataset
│   └── search-demo-qa-checklist.md   # Manual search QA checklist
├── src/
│   ├── main/
│   │   ├── java/com/jobportal/jobservice/
│   │   │   ├── config/          # OpenAPI configuration
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/             # Data transfer objects
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── exception/       # Exception handling
│   │   │   ├── repository/      # Data access
│   │   │   └── service/         # Business logic
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-docker.yml
│   └── test/
├── build.gradle.kts
├── Dockerfile
└── README.md
```

## Testing

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport
```

## Building

```bash
# Build JAR
./gradlew build

# Build Docker image
docker build -t job-service .
```
