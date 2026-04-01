#!/bin/bash

# Seed script to create 100 sample jobs via the Job Service API
# Usage: ./seed-jobs.sh [base_url]
# Example: ./seed-jobs.sh http://localhost:8081

BASE_URL="${1:-http://localhost:8081}"

# Arrays for generating varied job data
TITLES=(
  "Software Engineer"
  "Senior Software Engineer"
  "Frontend Developer"
  "Backend Developer"
  "Full Stack Developer"
  "DevOps Engineer"
  "Data Scientist"
  "Data Engineer"
  "Machine Learning Engineer"
  "Product Manager"
  "Engineering Manager"
  "QA Engineer"
  "Security Engineer"
  "Cloud Architect"
  "Mobile Developer"
  "iOS Developer"
  "Android Developer"
  "Site Reliability Engineer"
  "Technical Lead"
  "Solutions Architect"
)

COMPANIES=(
  "Tech Corp"
  "Innovate Inc"
  "Digital Solutions"
  "Cloud Systems"
  "Data Dynamics"
  "Future Tech"
  "Smart Software"
  "Code Masters"
  "Byte Works"
  "Cyber Systems"
)

LOCATIONS=(
  "San Francisco, CA"
  "New York, NY"
  "Seattle, WA"
  "Austin, TX"
  "Boston, MA"
  "Chicago, IL"
  "Denver, CO"
  "Los Angeles, CA"
  "Portland, OR"
  "Remote"
)

EMPLOYMENT_TYPES=("FULL_TIME" "PART_TIME" "CONTRACT" "INTERNSHIP")
STATUSES=("ACTIVE" "DRAFT")

REQUIREMENTS_POOL=(
  "Java"
  "Python"
  "JavaScript"
  "TypeScript"
  "Go"
  "Rust"
  "Spring Boot"
  "React"
  "Node.js"
  "AWS"
  "Docker"
  "Kubernetes"
  "PostgreSQL"
  "MongoDB"
  "Redis"
  "Git"
  "CI/CD"
  "Agile"
  "REST APIs"
  "GraphQL"
)

echo "Creating 100 jobs at $BASE_URL..."
echo "================================"

success_count=0
fail_count=0

for i in $(seq 1 100); do
  # Random selections
  title="${TITLES[$((RANDOM % ${#TITLES[@]}))]}"
  company="${COMPANIES[$((RANDOM % ${#COMPANIES[@]}))]}"
  location="${LOCATIONS[$((RANDOM % ${#LOCATIONS[@]}))]}"
  employment_type="${EMPLOYMENT_TYPES[$((RANDOM % ${#EMPLOYMENT_TYPES[@]}))]}"
  status="${STATUSES[$((RANDOM % ${#STATUSES[@]}))]}"
  
  # Generate salary range (50k-200k)
  salary_min=$(( (RANDOM % 15 + 5) * 10000 ))
  salary_max=$(( salary_min + (RANDOM % 5 + 1) * 20000 ))
  
  # Pick 3-5 random requirements
  num_reqs=$((RANDOM % 3 + 3))
  requirements=""
  for j in $(seq 1 $num_reqs); do
    req="${REQUIREMENTS_POOL[$((RANDOM % ${#REQUIREMENTS_POOL[@]}))]}"
    if [ -z "$requirements" ]; then
      requirements="\"$req\""
    else
      requirements="$requirements, \"$req\""
    fi
  done
  
  # Create job payload
  payload=$(cat <<EOF
{
  "title": "$title",
  "description": "We are looking for a talented $title to join our team at $company. This is an exciting opportunity to work on cutting-edge projects.",
  "company": "$company",
  "location": "$location",
  "employmentType": "$employment_type",
  "salaryMin": $salary_min,
  "salaryMax": $salary_max,
  "salaryCurrency": "USD",
  "requirements": [$requirements],
  "status": "$status"
}
EOF
)

  # Send request
  response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/jobs" \
    -H "Content-Type: application/json" \
    -d "$payload")
  
  http_code=$(echo "$response" | tail -n1)
  
  if [ "$http_code" -eq 200 ] || [ "$http_code" -eq 201 ]; then
    ((success_count++))
    echo "[$i/100] Created: $title at $company ✓"
  else
    ((fail_count++))
    echo "[$i/100] Failed: $title at $company ✗ (HTTP $http_code)"
  fi
done

echo "================================"
echo "Done! Created $success_count jobs, $fail_count failed."
