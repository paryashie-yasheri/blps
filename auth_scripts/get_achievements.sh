#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

COURSE_KEY="${1:-}"
USERNAME="${EDX_API_TEST_USERNAME}"
PASSWORD="${EDX_API_TEST_PASSWORD}"

require_tools curl jq

if [[ -z "${USERNAME}" || -z "${PASSWORD}" ]]; then
  echo "Error: EDX_API_TEST_USERNAME and EDX_API_TEST_PASSWORD must be set in .env" >&2
  exit 1
fi

echo "=== Login as ${USERNAME} ==="
login_response="$(login_openedx_user "${USERNAME}" "${PASSWORD}")"

token=$(echo "${login_response}" | jq -r '.accessToken // empty')
if [[ -z "${token}" ]]; then
  echo "Login failed: ${login_response}" >&2
  exit 1
fi
echo "Token acquired"

echo ""
echo "=== Balance ==="
json_request GET "${BASE_URL}/api/student/balance" \
  -H "Authorization: Bearer ${token}" | jq .

echo ""
echo "=== All Achievements ==="
achievements_json="$(json_request GET "${BASE_URL}/api/student/achievements" \
  -H "Authorization: Bearer ${token}")"

if [[ -n "${COURSE_KEY}" ]]; then
  echo "Filtering by course: ${COURSE_KEY}"
  filtered=$(echo "${achievements_json}" | jq --arg course "${COURSE_KEY}" \
    '[.[] | select(.achievementCode | contains($course))]')
  echo "${filtered}" | jq '[.[] | {code: .achievementCode, name: .achievementName, points: .pointsAwarded, earnedAt: .earnedAt}]'
else
  echo "${achievements_json}" | jq '[.[] | {code: .achievementCode, name: .achievementName, points: .pointsAwarded, earnedAt: .earnedAt}]'
fi
