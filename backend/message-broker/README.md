# Message Broker

This module prepares the RabbitMQ topology used by the Job Portal event flows.

## Supported Event Contracts

- `application.submitted` -> `ApplicationSubmittedEvent`
- `application.status-changed` -> `ApplicationStatusChangedEvent`
- `application.withdrawn` -> `ApplicationWithdrawnEvent`
- `job.posted` -> `JobPostedEvent`
- `auth.employer-verified` -> `EmployerVerifiedEvent`

Each event has:

- one main notification queue
- one short retry queue
- one dead-letter queue

## Why the retry queue exists

Notification email may need a short delay.

Example:

- a user logs in
- the frontend warms up the notification recipient cache
- an async event reaches RabbitMQ almost at the same time

The retry queue gives the system a few extra seconds before the notification is moved to long-term recovery.

The retry queue uses normal RabbitMQ TTL and dead-letter routing. No extra plugin is needed.

## Local Run

```bash
./gradlew bootRun
```

The service expects RabbitMQ at `localhost:5672` by default and exposes health at `http://localhost:8083/actuator/health`.

## Docker Compose

The repo root `docker-compose.yml` starts both `rabbitmq` and this topology service.

## Optional topology endpoint

`GET /api/broker/topology` is disabled by default.

If you need it in local debug:

```bash
export BROKER_TOPOLOGY_ENDPOINT_ENABLED=true
```
