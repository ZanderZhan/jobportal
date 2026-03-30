# Build stage
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

# Copy gradle wrapper
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Download dependencies
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build the application
RUN ./gradlew build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:25-jre

WORKDIR /app

# Create non-root user
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Copy jar from build stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
