#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/../.env"

BASE_URL="${SPRING_HOST%/}"
COURSE_KEY="${1:?Usage: $0 <course-key> <block-id> [staff-username] [staff-password] [student-username] [student-password]}"
BLOCK_ID="${2:?Usage: $0 <course-key> <block-id> [staff-username] [staff-password] [student-username] [student-password]}"
STAFF_USERNAME="${3:-${ADMIN_USERNAME:-admin}}"
STAFF_PASSWORD="${4:-${ADMIN_PASSWORD:-admin}}"
STUDENT_USERNAME="${5:-${EDX_API_TEST_USERNAME:-}}"
STUDENT_PASSWORD="${6:-${EDX_API_TEST_PASSWORD:-}}"

if [[ -z "${STUDENT_USERNAME}" || -z "${STUDENT_PASSWORD}" ]]; then
  echo "Student credentials are required. Set EDX_API_TEST_USERNAME and EDX_API_TEST_PASSWORD in .env or pass them as args." >&2
  exit 1
fi

for tool in curl jq; do
  command -v "${tool}" >/dev/null 2>&1 || {
    echo "Missing required tool: ${tool}" >&2
    exit 1
  }
done

uri_encode() {
  jq -rn --arg value "$1" '$value|@uri'
}

extract_token() {
  local response_json="$1"
  echo "${response_json}" | jq -r '.accessToken // empty'
}

login_staff() {
  curl -sS -X POST "${BASE_URL}/api/auth/staff/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"${STAFF_USERNAME}\",\"password\":\"${STAFF_PASSWORD}\"}"
}

login_student() {
  curl -sS -X POST "${BASE_URL}/api/auth/openedx_user/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"${STUDENT_USERNAME}\",\"password\":\"${STUDENT_PASSWORD}\"}"
}

echo "=== Login as staff (${STAFF_USERNAME}) ==="
staff_login_response="$(login_staff)"
staff_token="$(extract_token "${staff_login_response}")"
if [[ -z "${staff_token}" ]]; then
  echo "Staff login failed:"
  echo "${staff_login_response}" | jq .
  exit 1
fi
echo "Staff login OK"

echo ""
echo "=== Login as Open edX user (${STUDENT_USERNAME}) ==="
student_login_response="$(login_student)"
student_token="$(extract_token "${student_login_response}")"
if [[ -z "${student_token}" ]]; then
  echo "Student login failed:"
  echo "${student_login_response}" | jq .
  exit 1
fi
echo "Student login OK"

encoded_course_key="$(uri_encode "${COURSE_KEY}")"
encoded_block_id="$(uri_encode "${BLOCK_ID}")"
encoded_student_username="$(uri_encode "${STUDENT_USERNAME}")"
prefix="e2e_$(date +%s)"

echo ""
echo "=== Seed achievement types with real png icons ==="
"${SCRIPT_DIR}/staff_seed_achievement_images.sh" "${staff_token}" "${COURSE_KEY}" "${BLOCK_ID}" "${prefix}"

echo ""
echo "=== Baseline student state ==="
before_balance_json="$(curl -sS -X GET "${BASE_URL}/api/student/balance" \
  -H "Authorization: Bearer ${student_token}")"
before_achievements_json="$(curl -sS -X GET "${BASE_URL}/api/student/achievements" \
  -H "Authorization: Bearer ${student_token}")"
before_balance="$(echo "${before_balance_json}" | jq -r '.currentBalance // .balance')"
before_ach_count="$(echo "${before_achievements_json}" | jq 'length')"
echo "balance_before=${before_balance}"
echo "achievements_before=${before_ach_count}"

echo ""
echo "=== Open edX progress snapshot BEFORE actions ==="
curl -sS -X GET "${BASE_URL}/api/edx/courses/${encoded_course_key}/progress" \
  -H "Authorization: Bearer ${student_token}" | jq '{username, user_has_passing_grade, completion_summary, final_grades}'

echo ""
echo "=== Open edX subsection completion BEFORE actions ==="
curl -sS -X GET "${BASE_URL}/api/edx/users/${encoded_student_username}/courses/${encoded_course_key}/subsections/${encoded_block_id}/completion" \
  -H "Authorization: Bearer ${student_token}" | jq .

echo ""
echo "=== Trigger course_enrolled event ==="
enroll_response="$(curl -sS -X POST "${BASE_URL}/api/student/courses/${encoded_course_key}/enroll" \
  -H "Authorization: Bearer ${student_token}")"
echo "${enroll_response}" | jq .

echo ""
echo "=== Trigger lesson_complete (+ derived events) ==="
complete_response="$(curl -sS -X POST "${BASE_URL}/api/student/lessons/${encoded_course_key}/${encoded_block_id}/complete" \
  -H "Authorization: Bearer ${student_token}")"
echo "${complete_response}" | jq .

echo ""
echo "=== Open edX progress snapshot AFTER actions ==="
curl -sS -X GET "${BASE_URL}/api/edx/courses/${encoded_course_key}/progress" \
  -H "Authorization: Bearer ${student_token}" | jq '{username, user_has_passing_grade, completion_summary, final_grades}'

echo ""
echo "=== Open edX subsection completion AFTER actions ==="
curl -sS -X GET "${BASE_URL}/api/edx/users/${encoded_student_username}/courses/${encoded_course_key}/subsections/${encoded_block_id}/completion" \
  -H "Authorization: Bearer ${student_token}" | jq .

echo ""
echo "=== Student state AFTER actions ==="
after_balance_json="$(curl -sS -X GET "${BASE_URL}/api/student/balance" \
  -H "Authorization: Bearer ${student_token}")"
after_achievements_json="$(curl -sS -X GET "${BASE_URL}/api/student/achievements" \
  -H "Authorization: Bearer ${student_token}")"
after_balance="$(echo "${after_balance_json}" | jq -r '.currentBalance // .balance')"
after_ach_count="$(echo "${after_achievements_json}" | jq 'length')"
echo "balance_after=${after_balance}"
echo "achievements_after=${after_ach_count}"
echo "${after_achievements_json}" | jq '[.[] | {achievementCode, achievementName, pointsAwarded, iconKey, iconUrl, earnedAt}]'

echo ""
echo "=== Assertions ==="
mandatory_codes=(
  "${prefix}_course_enrolled"
  "${prefix}_lesson_complete"
)

for code in "${mandatory_codes[@]}"; do
  issued_count="$(echo "${after_achievements_json}" | jq --arg code "${code}" '[.[] | select(.achievementCode == $code)] | length')"
  if [[ "${issued_count}" -gt 0 ]]; then
    echo "[OK] ${code} issued (${issued_count})"
  else
    echo "[FAIL] ${code} was not issued" >&2
    exit 1
  fi
done

optional_codes=(
  "${prefix}_subsection_complete"
  "${prefix}_course_progress_updated"
  "${prefix}_course_complete"
)

for code in "${optional_codes[@]}"; do
  issued_count="$(echo "${after_achievements_json}" | jq --arg code "${code}" '[.[] | select(.achievementCode == $code)] | length')"
  if [[ "${issued_count}" -gt 0 ]]; then
    echo "[OK] ${code} issued (${issued_count})"
  else
    echo "[WARN] ${code} not issued in this run (depends on actual Open edX progress state)"
  fi
done

echo ""
echo "Workflow finished successfully."
