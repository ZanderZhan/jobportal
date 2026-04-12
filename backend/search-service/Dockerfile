# Build stage
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src src

RUN ./gradlew build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl && addgroup -S search && adduser -S search -G search

COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown -R search:search /app

USER search

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=docker"]
