# Job Portal

A full-stack job portal application that connects job seekers with employers. The platform enables users to browse job listings, apply for positions, and manage their career profiles.

## Features

- 🔍 Browse and search job listings
- 📝 Apply for positions
- 👤 User profile management
- 💼 Employer job posting management

## Architecture

This monorepo contains two main components:

| Component | Tech Stack | Description |
|-----------|------------|-------------|
| **Frontend** | React 19, TypeScript, Vite | Modern SPA for user interface |
| **Backend** | Spring Boot 4.0.2, Java 25 | RESTful API server |

## Requirements

- **Backend**: Java 25, Gradle 8.x+
- **Frontend**: Node.js 20+, npm 10+

## Getting Started

### Backend

```bash
cd backend
./gradlew bootRun
```

The API server runs at `http://localhost:8080`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The development server runs at `http://localhost:5173`

## Project Structure

```
jobportal/
├── backend/           # Spring Boot API
│   ├── src/
│   └── build.gradle.kts
└── frontend/          # React SPA
    ├── src/
    └── package.json
```

## Development

### Running Both Services

In separate terminals:

```bash
# Terminal 1 - Backend
cd backend && ./gradlew bootRun

# Terminal 2 - Frontend
cd frontend && npm run dev
```

### Building for Production

```bash
# Backend
cd backend && ./gradlew build

# Frontend
cd frontend && npm run build
```

