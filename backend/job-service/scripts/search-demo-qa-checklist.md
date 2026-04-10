# Search Demo QA Checklist

Use this checklist after seeding `search-demo-jobs.json`.

## Seed The Data

```bash
cd backend/job-service
./scripts/seed-jobs.sh --replace --reindex
```

If you are running the full stack in Docker, the default URLs already match:

- `job-service`: `http://localhost:8081`
- `search-service`: `http://localhost:8083`

## Core Search Checks

1. Open `http://localhost:8080/api/search/jobs?page=0&size=20`
   - Expect a non-empty response with mostly `ACTIVE` jobs.
2. Open `http://localhost/jobs`
   - Expect the jobs page to render a large, varied result set.
3. Search `Frontend`
   - Expect multiple frontend-related roles across different companies and locations.
4. Search `Platform`
   - Expect backend platform, payment platform, and search relevance platform-adjacent roles.
5. Search `Data`
   - Expect analyst, analytics engineer, data engineer, and machine learning roles.

## Filtering Checks

1. Filter by `company=Northwind`
   - Expect multiple frontend and design-oriented roles.
2. Filter by `location=Remote`
   - Expect a broad mix of engineering, analytics, and support roles.
3. Filter by `employmentType=CONTRACT`
   - Expect design systems, QA, GIS, and analytics-style contract roles.
4. Filter by `employmentType=INTERNSHIP`
   - Expect intern roles from product, engineering, cloud, and QA domains.
5. Filter by `status=ACTIVE`
   - Expect only live roles.
6. Filter by `status=CLOSED`
   - Expect a smaller result set that proves default public search is excluding closed roles.
7. Filter by `salaryMin=90000&salaryMax=130000`
   - Expect senior or platform-heavy roles more often than entry-level roles.

## Ranking And Discovery Checks

1. Search `Search`
   - Expect `Search Relevance Engineer` to be a strong match.
2. Search `Security`
   - Expect security engineering and security operations roles from multiple companies.
3. Search `Product`
   - Expect product manager, product designer, AI product engineer, and clinical product roles.
4. Search `Engineer` and compare the first page to a narrower query like `Backend Engineer`
   - Expect narrower queries to surface more focused roles.
5. Search `Atlas`
   - Expect payment, fraud, mobile, and compliance roles grouped around the same company.

## Data Coverage Checks

Confirm the dataset includes:

- active, draft, and closed jobs
- full-time, part-time, contract, and internship jobs
- EUR, GBP, and USD salary currencies
- remote, hybrid, and on-site style locations
- overlapping keywords across titles and descriptions
- salary ranges from internship-level to senior/staff compensation
