# Job Portal

A full-stack job portal platform for students and employers. Users can browse and search jobs, apply for roles, manage applications, and maintain profiles.

## Features

- Browse and search job listings
- Employer job posting and management
- Student application submission and tracking
- Profile management
- Gateway-based microservice backend

## Architecture

| Component | Stack | Notes |
| --- | --- | --- |
| Frontend | React 19, TypeScript, Vite | SPA UI |
| Backend | Spring Boot (Java 25) microservices | Gateway + domain services |
| Data & Infra | PostgreSQL, Redis, RabbitMQ, Docker Compose | Local dev stack |

### Backend services

- `gateway` (port 8080)
- `job-service` (port 8081)
- `auth-service` (port 8082)
- `message-broker` (port 8083)
- `application-service` (port 8084)
- `profile-service` (port 8085)
- `notification-service` (port 8086)

## Requirements

- Java 25
- Node.js 20+ and npm 10+
- Docker + Docker Compose (recommended for full stack)

## Getting Started

### Option 1: Run full stack with Docker

From repository root:

```bash
docker compose up -d --build
```

Key URLs:

- Frontend: `http://localhost`
- API Gateway: `http://localhost:8080`
- RabbitMQ UI: `http://localhost:15672`

### Option 2: Frontend only (against existing backend)

```bash
cd frontend
npm install
npm run dev
```

Frontend dev server: `http://localhost:5173`

## Development

### Backend testing

```bash
cd backend/job-service && ./gradlew test --no-daemon --console=plain
cd backend/gateway && ./gradlew test --no-daemon --console=plain
```

### Frontend checks

```bash
cd frontend && npm run lint
cd frontend && npm run build
```

## Project Structure

```text
jobportal/
├── backend/
│   ├── gateway/
│   ├── job-service/
│   ├── auth-service/
│   ├── application-service/
│   ├── profile-service/
│   ├── message-broker/
│   └── notification-service/
├── frontend/
└── docker-compose.yml
```
