#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BASE_URL="${BASE_URL:-http://localhost:1234}"
COOKIES_FILE="${SCRIPT_DIR}/cookies.txt"

USERNAME="testuser_$(date +%s)"
PASSWORD="secret123"
EMAIL="${USERNAME}@test.com"

echo "=== User Workflow ==="
echo "Registering user: $USERNAME"

curl -s -X POST "${BASE_URL}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\",\"email\":\"${EMAIL}\"}"

echo -e "\n\n=== Logging in ==="
curl -s -c "${COOKIES_FILE}" -X POST "${BASE_URL}/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${USERNAME}&password=${PASSWORD}"

echo -e "\n\n=== Get Balance ==="
curl -s -b "${COOKIES_FILE}" -X GET "${BASE_URL}/api/student/balance"

echo -e "\n\n=== Get Courses ==="
curl -s -b "${COOKIES_FILE}" -X GET "${BASE_URL}/api/student/courses"

echo -e "\n\n=== Get Achievements ==="
curl -s -b "${COOKIES_FILE}" -X GET "${BASE_URL}/api/student/achievements"

echo -e "\n\n=== Get Available Rewards ==="
curl -s -b "${COOKIES_FILE}" -X GET "${BASE_URL}/api/student/rewards"

echo -e "\n\n=== Complete Lesson (simulated) ==="
curl -s -b "${COOKIES_FILE}" -X POST "${BASE_URL}/api/student/lessons/course-v1:Test+Course/block-v1:Test+type@sequential+block1/complete"

echo -e "\n\n=== Get Balance After Lesson ==="
curl -s -b "${COOKIES_FILE}" -X GET "${BASE_URL}/api/student/balance"

echo -e "\n\n=== Get Achievements After Lesson ==="
curl -s -b "${COOKIES_FILE}" -X GET "${BASE_URL}/api/student/achievements"

echo -e "\n\n=== Get My Rewards ==="
curl -s -b "${COOKIES_FILE}" -X GET "${BASE_URL}/api/student/rewards/my"

echo -e "\n\n=== Workflow Complete ==="
echo "Cookies saved to: ${COOKIES_FILE}"