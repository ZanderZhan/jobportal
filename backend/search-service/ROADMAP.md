# Search Service Roadmap

This document captures the planned evolution of `search-service` from a thin proxy into a dedicated search platform boundary for the Job Portal backend.

## Current State

- Exposes `GET /api/search/jobs`
- Uses `search-service` as the public search boundary for job discovery
- Maintains a PostgreSQL-backed search index with fallback behavior
- Supports autocomplete, discovery suggestions, saved searches, and search analytics
- Provides a stable entry point for future search-specific logic and frontend product work

## Goals

- Keep external search APIs stable while internals evolve
- Separate search concerns from CRUD concerns in `job-service`
- Improve relevance, resilience, and observability over time
- Create a clear path toward indexed search if query volume or complexity grows

## Milestone 1: Stabilize The Boundary

Scope:
- Move frontend search traffic to `search-service`
- Treat `search-service` as the only public search API
- Lock down request and response contracts

Deliverables:
- Frontend uses `/api/search/jobs` instead of `/api/jobs/search`
- Contract tests for request forwarding and response shape
- Clear error mapping for upstream failures
- Documented limits for `page`, `size`, and `sort`

Success criteria:
- All job discovery traffic enters through `search-service`
- No client depends directly on `job-service` search endpoints

## Milestone 2: Add Search-Specific Application Logic

Scope:
- Add logic that belongs in search, not CRUD
- Normalize and validate incoming queries

Deliverables:
- Query normalization for whitespace, casing, and empty values
- Default filter policy for public search
- Input validation and safe paging caps
- Structured metrics for query count, latency, and upstream error rate

Success criteria:
- Search behavior is consistent and predictable
- Operational dashboards can show search health separately from job-service health

## Milestone 3: Improve Relevance And Performance

Scope:
- Make results better ordered and cheaper to serve

Deliverables:
- Ranking strategy for exact matches, partial matches, recency, and active jobs
- Response caching for frequent anonymous queries
- Timeouts, retries, and circuit-breaker behavior around upstream calls
- Optional support for facets such as location, company, and employment type

Success criteria:
- Median and tail latency improve for common queries
- Search results feel more intentional than simple database filtering

## Milestone 4: Introduce A Real Search Backend

Scope:
- Stop relying on synchronous proxying for all search execution

Options to evaluate:
- PostgreSQL full-text search as the first step
- OpenSearch or Elasticsearch if scale or ranking needs exceed database search

Deliverables:
- Search index schema and synchronization strategy
- Reindex job for backfills
- Incremental update path from job-service changes
- Fallback behavior during indexing lag or partial outages

Success criteria:
- Search can evolve independently of job-service query design
- Complex text search does not depend on transactional CRUD queries

## Milestone 5: Product Features

Scope:
- Turn the service into a user-facing search product layer

Deliverables:
- Autocomplete and typeahead
- Suggested filters and related searches
- Saved searches
- Personalized ranking when user context is available
- Analytics for zero-result queries and abandoned searches

Success criteria:
- Search becomes a measurable product capability, not only an API endpoint

## Milestone 6: Search Experience UI

Scope:
- Turn the existing jobs page into a stronger search interface
- Make search features visible, usable, and explainable in the frontend

Deliverables:
- A dedicated search layout with clear separation between query entry, filters, results, and saved searches
- Stronger autocomplete presentation with keyboard navigation and loading/empty states
- Visible related searches and suggested filters that feel connected to the current query
- Better result cards that surface why a job is relevant to the active search
- Mobile-friendly filter interactions and sticky search controls for desktop

Success criteria:
- Search features added in Milestone 5 are clearly visible in the UI
- Users can refine and recover from weak queries without guessing what to do next
- The jobs page feels like a search product, not only a list view

## Milestone 7: Advanced Filtering And Query Design

Scope:
- Expand filtering beyond the first release and make advanced search easier to evaluate
- Keep filtering semantics consistent between frontend, `search-service`, and indexed search

Deliverables:
- Filter support for salary ranges, salary currency, posted date ranges, remote or on-site mode, and multi-select employment type
- A consistent query model for combining text search with structured filters
- Clear URL state for shareable searches and back or forward navigation
- Search filter research to decide which filters deserve primary placement versus advanced placement
- Validation and analytics around filter usage, zero-result combinations, and abandoned refinements

Success criteria:
- Advanced filters can be tested confidently with realistic combinations
- Query state is predictable and visible in the URL
- Filter usage data can guide future ranking and UX decisions

## Milestone 8: Search Demo Data And Evaluation

Scope:
- Create enough realistic data to validate search UX, ranking, and advanced filtering
- Make local and demo environments easier to test repeatedly

Deliverables:
- Seed data with broad coverage across titles, companies, locations, salary ranges, statuses, and employment types
- Jobs that intentionally overlap on keywords so autocomplete, ranking, and related searches can be evaluated
- Jobs that exercise edge cases such as remote-only roles, contract work, seniority differences, and salary gaps
- A repeatable seeding flow for local development and Docker environments
- A lightweight QA checklist for validating autocomplete, discovery, filtering, saved searches, and analytics

Success criteria:
- Developers can validate advanced search behavior without manual data entry
- Search relevance and filtering regressions are easier to spot before release
- Frontend work is not blocked by missing or low-variety test data

## Milestone 9: Search Observability And Product Tuning

Scope:
- Turn the collected search analytics into operational and product feedback loops
- Improve confidence in future search changes

Deliverables:
- Dashboards for query volume, zero-result rate, click-through rate, and abandoned searches
- Instrumentation for autocomplete usage, facet selection, and saved-search adoption
- A small set of relevance and UX tuning levers that can be adjusted without redesigning the API
- Guardrails for measuring the effect of ranking or filtering changes over time

Success criteria:
- Search quality can be improved using actual behavior data
- Backend and frontend changes can be evaluated against stable product metrics

## Non-Goals For Now

- Full personalization in the first release
- Distributed event-driven indexing before there is a demonstrated need
- Expanding the service into a general reporting or recommendation backend

## Technical Risks

- Duplicating filtering semantics across `job-service` and `search-service`
- Search ranking changes becoming hard to explain without proper observability
- Tight coupling to upstream response models if contracts are not separated early
- Premature adoption of external search infrastructure before query scale justifies it

## Recommended Next Step

The next change should be Milestone 6:
- refine the jobs page into a stronger search experience
- decide which filters are primary, advanced, or suggestion-driven
- add richer demo data so advanced filter work can be evaluated quickly
