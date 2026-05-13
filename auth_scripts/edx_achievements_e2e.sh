#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

COURSE_KEY="${1:?Usage: $0 <course-key> <block-id> [staff-username] [staff-password] [student-username] [student-password]}"
BLOCK_ID="${2:?Usage: $0 <course-key> <block-id> [staff-username] [staff-password] [student-username] [student-password]}"
STAFF_USERNAME="${3:-${KEYCLOAK_ADMIN:-admin}}"
STAFF_PASSWORD="${4:-${KEYCLOAK_ADMIN_PASSWORD:-admin}}"
STUDENT_USERNAME="${5:-${EDX_API_TEST_USERNAME:-}}"
STUDENT_PASSWORD="${6:-${EDX_API_TEST_PASSWORD:-}}"

if [[ -z "${STUDENT_USERNAME}" || -z "${STUDENT_PASSWORD}" ]]; then
  echo "Student credentials are required. Set EDX_API_TEST_USERNAME and EDX_API_TEST_PASSWORD in .env or pass them as args." >&2
  exit 1
fi

require_tools curl jq

echo "=== Login as staff (${STAFF_USERNAME}) ==="
staff_login_response="$(login_staff "${STAFF_USERNAME}" "${STAFF_PASSWORD}")"
staff_token="$(extract_token "${staff_login_response}")"
if [[ -z "${staff_token}" ]]; then
  echo "Staff login failed:"
  echo "${staff_login_response}" | jq .
  exit 1
fi
echo "Staff login OK"

echo ""
echo "=== Login as Open edX user (${STUDENT_USERNAME}) ==="
student_login_response="$(login_openedx_user "${STUDENT_USERNAME}" "${STUDENT_PASSWORD}")"
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
before_balance_json="$(json_request GET "${BASE_URL}/api/student/balance" \
  -H "Authorization: Bearer ${student_token}")"
before_achievements_json="$(json_request GET "${BASE_URL}/api/student/achievements" \
  -H "Authorization: Bearer ${student_token}")"
before_balance="$(echo "${before_balance_json}" | jq -r '.currentBalance // .balance')"
before_ach_count="$(echo "${before_achievements_json}" | jq 'length')"
echo "balance_before=${before_balance}"
echo "achievements_before=${before_ach_count}"

echo ""
echo "=== Open edX progress snapshot BEFORE actions ==="
json_request GET "${BASE_URL}/api/edx/courses/${encoded_course_key}/progress" \
  -H "Authorization: Bearer ${student_token}" | jq '{username, user_has_passing_grade, completion_summary, final_grades}'

echo ""
echo "=== Open edX subsection completion BEFORE actions ==="
json_request GET "${BASE_URL}/api/edx/users/${encoded_student_username}/courses/${encoded_course_key}/subsections/${encoded_block_id}/completion" \
  -H "Authorization: Bearer ${student_token}" | jq .

echo ""
echo "=== Trigger course_enrolled event ==="
enroll_response="$(json_request POST "${BASE_URL}/api/student/courses/${encoded_course_key}/enroll" \
  -H "Authorization: Bearer ${student_token}")"
echo "${enroll_response}" | jq .

echo ""
echo "=== Trigger lesson_complete (+ derived events) ==="
complete_response="$(json_request POST "${BASE_URL}/api/student/lessons/${encoded_course_key}/${encoded_block_id}/complete" \
  -H "Authorization: Bearer ${student_token}")"
echo "${complete_response}" | jq .

echo ""
echo "=== Open edX progress snapshot AFTER actions ==="
json_request GET "${BASE_URL}/api/edx/courses/${encoded_course_key}/progress" \
  -H "Authorization: Bearer ${student_token}" | jq '{username, user_has_passing_grade, completion_summary, final_grades}'

echo ""
echo "=== Open edX subsection completion AFTER actions ==="
json_request GET "${BASE_URL}/api/edx/users/${encoded_student_username}/courses/${encoded_course_key}/subsections/${encoded_block_id}/completion" \
  -H "Authorization: Bearer ${student_token}" | jq .

echo ""
echo "=== Student state AFTER actions ==="
after_balance_json="$(json_request GET "${BASE_URL}/api/student/balance" \
  -H "Authorization: Bearer ${student_token}")"
after_achievements_json="$(json_request GET "${BASE_URL}/api/student/achievements" \
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
