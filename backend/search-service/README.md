# Search Service

`backend/search-service` is currently a placeholder. The MVP search behaviour is implemented through `backend/job-service` using `/api/jobs/search`.

## Current Ownership

- `job-service` owns job data and the current search/filter endpoint.
- `search-service` does not currently contain runtime source code or a build definition.
- Search improvements should therefore be applied to `job-service` until a real extraction is planned.

## Current Search Behaviour

The current MVP search supports:

- title partial match
- company partial match
- location partial match
- employment type filtering
- salary range filtering
- status filtering
- employer/owner filtering
- stable default ordering by `createdAt` descending

## Future Extraction Triggers

Move search from `job-service` into `search-service` only when there is a clear need, such as:

- full-text search
- ranked search results
- search analytics
- recommendations
- indexing outside the relational job database
- a volume of job data that makes simple database filtering unsuitable

Until then, keeping search in `job-service` avoids unnecessary distributed-system complexity for the MVP.
