FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy gradle wrapper and config
COPY gradle gradle
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Copy source code
COPY src src

# Download dependencies and build
RUN chmod +x gradlew && ./gradlew build -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S gateway && adduser -S gateway -G gateway

# Copy built artifact
COPY --from=builder /app/build/libs/*.jar app.jar

# Set ownership
RUN chown -R gateway:gateway /app

USER gateway

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
