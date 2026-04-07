# Notification Service

This service consumes business events and turns them into user notifications.

## Main responsibilities

- consume asynchronous events from RabbitMQ
- create in-app notifications
- prepare email delivery attempts
- store delivery history
- allow retry for failed deliveries

## Main event inputs

- `application.submitted`
- `application.status-changed`
- `job.posted`
- `auth.employer-verified`

## Local run

```bash
./gradlew bootRun
```

Default local port is `8084`.
