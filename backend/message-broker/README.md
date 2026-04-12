# Message Broker

This module provisions the RabbitMQ topology used by the Job Portal event-driven flows.

## Supported Event Contracts

- `application.submitted` -> `ApplicationSubmittedEvent`
- `application.status-changed` -> `ApplicationStatusChangedEvent`
- `application.withdrawn` -> `ApplicationWithdrawnEvent`
- `job.posted` -> `JobPostedEvent`
- `auth.employer-verified` -> `EmployerVerifiedEvent`

Each event is routed to a dedicated notification queue plus a dedicated dead-letter queue.

## Local Run

```bash
./gradlew bootRun
```

The service expects RabbitMQ at `localhost:5672` by default and exposes health at `http://localhost:8083/actuator/health`.

## Docker Compose

The repo root `docker-compose.yml` starts both `rabbitmq` and this topology service.
