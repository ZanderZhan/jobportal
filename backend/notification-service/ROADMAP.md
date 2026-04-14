# Notification Service Roadmap

## Current shape

The service already supports:

- async event consumption
- in-app notifications
- email delivery with JavaMailSender
- short RabbitMQ retry for cold-start cases
- long recovery for `PENDING_RECIPIENT`
- user notification preferences
- simple frontend notification center

## Why this design was chosen

We wanted to keep the notification database independent.

Because of that:

- notification-service does not read another service database
- it keeps its own local recipient cache
- the frontend helps warm this cache after login

This gives a cleaner boundary, but it still supports async delivery.

## Next steps

Short-term next steps:

- add more notification templates
- show more delivery details in the admin view
- add better paging and search in the frontend page

Longer-term next steps:

- move more recipient data into domain events
- add stronger email provider settings
- add analytics for open rates and delivery trends
