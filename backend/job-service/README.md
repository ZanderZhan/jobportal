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

> **Note:** If your project path contains spaces, use `./run-gradle.sh` instead of `./gradlew`, or run the JAR directly with `java -jar build/libs/job-service-0.0.1-SNAPSHOT.jar`.

## Getting Started

### Option 1: Local Development

1. **Start PostgreSQL** (if not using Docker):
   ```bash
   # Ensure PostgreSQL is running on localhost:5432
   # Create database: jobportal
   ```

2. **Build and run the application**:
   ```bash
   # Build the JAR
   ./run-gradle.sh build -x test
   
   # Run the application
   java -jar build/libs/job-service-0.0.1-SNAPSHOT.jar
   ```

3. The service runs at `http://localhost:8081`

### Option 2: Docker Compose

From the `jobportal` root directory:

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

## Project Structure

```
job-service/
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
