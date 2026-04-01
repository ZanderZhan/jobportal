FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

COPY gradle gradle
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .

COPY src src

RUN chmod +x gradlew && ./gradlew build -x test --no-daemon

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

RUN addgroup -S auth && adduser -S auth -G auth

COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown -R auth:auth /app

USER auth

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
