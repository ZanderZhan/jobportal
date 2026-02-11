# Job Portal Backend

A Spring Boot backend application for the Job Portal project.

## Requirements

- Java 25
- Gradle 8.x+

## Getting Started

### Build the project

```bash
./gradlew build
```

### Run the application

```bash
./gradlew bootRun
```

### Run tests

```bash
./gradlew test
```

## Configuration

Configuration is managed via `src/main/resources/application.properties`.

### Default Properties

| Property | Default | Description |
|----------|---------|-------------|
| `spring.application.name` | `backend` | Application name |
| `server.port` | `8080` | HTTP server port |

### Environment Variables

You can override properties using environment variables:

```bash
export SERVER_PORT=9000
./gradlew bootRun
```

### Profiles

Run with a specific profile:

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## Project Structure

```
src/
├── main/
│   ├── java/          # Application source code
│   └── resources/     # Configuration files
└── test/              # Test source code
```

## Tech Stack

- Spring Boot 4.0.2
- Java 25
- Gradle (Kotlin DSL)
- JUnit 5