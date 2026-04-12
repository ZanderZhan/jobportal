#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DATA_FILE="${DATA_FILE:-$SCRIPT_DIR/search-demo-jobs.json}"
JOB_SERVICE_URL="${JOB_SERVICE_URL:-http://localhost:8081}"
REPLACE_DATA=false

usage() {
  cat <<'EOF'
Usage: ./seed-jobs.sh [options]

Seed the curated search demo dataset through the job-service API.

Options:
  --job-service-url URL      Base URL for job-service (default: http://localhost:8081)
  --data-file PATH           Override the demo dataset file
  --replace                  Delete existing jobs through the API before seeding
  -h, --help                 Show this help message

Examples:
  ./seed-jobs.sh
  ./seed-jobs.sh --replace
  ./seed-jobs.sh --job-service-url http://localhost:8081 --replace
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --job-service-url)
      JOB_SERVICE_URL="$2"
      shift 2
      ;;
    --data-file)
      DATA_FILE="$2"
      shift 2
      ;;
    --replace)
      REPLACE_DATA=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ ! -f "$DATA_FILE" ]]; then
  echo "Demo dataset not found: $DATA_FILE" >&2
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required to run this seed script." >&2
  exit 1
fi

echo "Seeding curated search demo jobs"
echo "Job Service:   $JOB_SERVICE_URL"
echo "Data File:     $DATA_FILE"
echo "Replace Data:  $REPLACE_DATA"
echo "================================"

python3 - "$JOB_SERVICE_URL" "$DATA_FILE" "$REPLACE_DATA" <<'PY'
import json
import sys
import urllib.error
import urllib.request

base_url = sys.argv[1].rstrip("/")
data_file = sys.argv[2]
replace_existing = sys.argv[3].lower() == "true"


def request(method, url, payload=None, expect_json=True):
    headers = {"Content-Type": "application/json"}
    body = None
    if payload is not None:
        body = json.dumps(payload).encode("utf-8")

    req = urllib.request.Request(url, data=body, headers=headers, method=method)

    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            response_body = response.read().decode("utf-8")
            if expect_json and response_body:
                return json.loads(response_body)
            return response_body
    except urllib.error.HTTPError as exc:
        error_body = exc.read().decode("utf-8")
        raise SystemExit(f"{method} {url} failed with HTTP {exc.code}: {error_body}")
    except urllib.error.URLError as exc:
        raise SystemExit(f"{method} {url} failed: {exc.reason}")


def list_existing_job_ids():
    job_ids = []
    page = 0

    while True:
        response = request(
            "GET",
            f"{base_url}/api/jobs?page={page}&size=200&sort=id,asc",
        )
        job_ids.extend(job["id"] for job in response.get("content", []))
        if response.get("last", True):
            break
        page += 1

    return job_ids


with open(data_file, "r", encoding="utf-8") as dataset_file:
    jobs = json.load(dataset_file)

if not isinstance(jobs, list) or not jobs:
    raise SystemExit("The demo dataset must be a non-empty JSON array.")

if replace_existing:
    existing_job_ids = list_existing_job_ids()
    for index, job_id in enumerate(existing_job_ids, start=1):
        request("DELETE", f"{base_url}/api/jobs/{job_id}", expect_json=False)
        print(f"[reset {index}/{len(existing_job_ids)}] deleted job {job_id}")

status_counts = {}
employment_type_counts = {}
currency_counts = {}

for index, job in enumerate(jobs, start=1):
    request("POST", f"{base_url}/api/jobs", payload=job, expect_json=False)
    status_counts[job["status"]] = status_counts.get(job["status"], 0) + 1
    employment_type_counts[job["employmentType"]] = employment_type_counts.get(job["employmentType"], 0) + 1
    currency_counts[job["salaryCurrency"]] = currency_counts.get(job["salaryCurrency"], 0) + 1
    print(f"[seed {index}/{len(jobs)}] {job['title']} at {job['company']}")

print("================================")
print(f"Seeded {len(jobs)} curated jobs")
print("By status:")
for key in sorted(status_counts):
    print(f"  - {key}: {status_counts[key]}")

print("By employment type:")
for key in sorted(employment_type_counts):
    print(f"  - {key}: {employment_type_counts[key]}")

print("By currency:")
for key in sorted(currency_counts):
    print(f"  - {key}: {currency_counts[key]}")
PY

echo "Done."
