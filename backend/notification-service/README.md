# Notification Service

This service turns business events into user notifications.

## Main responsibilities

- consume asynchronous events from RabbitMQ
- create in-app notifications first
- send email when the recipient email is ready
- keep delivery history
- retry failed email work
- keep a local recipient cache for later recovery

## Main event inputs

- `application.submitted`
- `application.status-changed`
- `application.withdrawn`
- `job.posted`
- `auth.employer-verified`

## How email works

The service uses two steps for email delivery.

1. It saves the notification in the database and stores the in-app message.
2. It tries email delivery.

If the service already knows the user's email, it sends the email at once.

If the email is not ready yet:

- the notification stays in-app
- the message goes to a short RabbitMQ retry queue
- the service waits a few seconds and tries again
- if the email is still missing, the notification moves to `PENDING_RECIPIENT`
- a background scheduler checks again later

This design keeps the notification database isolated from other services.

## Recipient cache

The service has a small `RecipientIdentity` table.

- When the frontend calls notification endpoints after login, the service reads the JWT.
- It saves `userId`, `email`, and `name` to the local cache.
- Later async events can reuse this data for email delivery.

## Useful endpoints

- `GET /api/notifications/bootstrap`
- `GET /api/notifications/summary`
- `GET /api/notifications/me`
- `PATCH /api/notifications/{id}/read`
- `PATCH /api/notifications/read-all`
- `GET /api/notification-preferences/me`
- `PUT /api/notification-preferences/me/{eventType}`
- `GET /api/admin/notifications/failed`
- `POST /api/admin/notifications/{id}/retry`

## Local run

```bash
./gradlew bootRun
```

Default local port is `8086`.
