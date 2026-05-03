#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/../.env"

BASE_URL="${SPRING_HOST%/}"
STAFF_USERNAME="${ADMIN_USERNAME}"
STAFF_PASSWORD="${ADMIN_PASSWORD}"
STUDENT_USERNAME="${EDX_API_TEST_USERNAME}"
STUDENT_PASSWORD="${EDX_API_TEST_PASSWORD}"

if [[ -z "${STUDENT_USERNAME}" || -z "${STUDENT_PASSWORD}" ]]; then
  echo "Error: EDX_API_TEST_USERNAME and EDX_API_TEST_PASSWORD must be set in .env" >&2
  exit 1
fi

CODE_PREFIX="e2e_$(date +%s)"

extract_token() {
  echo "$1" | jq -r '.access_token // empty'
}

uri_encode() {
  jq -rn --arg value "$1" '$value|@uri'
}

# ── Course-key / block-id resolution ─────────────
if [[ -n "${1:-}" ]]; then
  COURSE_KEY="$1"
fi

if [[ -n "${2:-}" ]]; then
  BLOCK_ID="$2"
fi

if [[ -z "${COURSE_KEY:-}" || -z "${BLOCK_ID:-}" ]]; then
  echo ">>> Step 0: Resolving course and block from Open edX"

  # Student login for discovery
  student_login="$(curl -sS -X POST "${BASE_URL}/api/auth/openedx_user/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"${STUDENT_USERNAME}\",\"password\":\"${STUDENT_PASSWORD}\"}")"
  _student_token="$(extract_token "${student_login}")"
  if [[ -z "${_student_token}" ]]; then
    echo "    Student login failed: ${student_login}" >&2; exit 1
  fi

  if [[ -z "${COURSE_KEY:-}" ]]; then
    # Fetch courses
    courses_json="$(curl -sS "${BASE_URL}/api/student/courses" \
      -H "Authorization: Bearer ${_student_token}")"

    COURSE_KEY="$(echo "${courses_json}" | jq -r '(if type == "object" then .results else . end) | .[0].id // .[0].courseId // .[0].course_id // empty')"
    if [[ -z "${COURSE_KEY}" ]]; then
      echo "    No courses found for user ${STUDENT_USERNAME}" >&2; exit 1
    fi
    echo "    Found course: ${COURSE_KEY}"
  fi

  if [[ -z "${BLOCK_ID:-}" ]]; then
    # Fetch outline to get first block
    encoded_course="$(uri_encode "${COURSE_KEY}")"
    outline_json="$(curl -sS "${BASE_URL}/api/student/courses/${encoded_course}/outline" \
      -H "Authorization: Bearer ${_student_token}")"

    # Try to find a usable block id from the outline
    BLOCK_ID="$(echo "${outline_json}" | jq -r '
      .. | objects | select(.id? != null and (.type? == "sequential" or .type? == "vertical" or .type? == "problem")) | .id
    ' 2>/dev/null | head -n1)"

    if [[ -z "${BLOCK_ID}" ]]; then
      # Fallback: try to extract any block id from outline
      BLOCK_ID="$(echo "${outline_json}" | jq -r '
        .. | objects | select(.id? != null and (.id | test("block-v1"))) | .id
      ' 2>/dev/null | head -n1)"
    fi

    if [[ -z "${BLOCK_ID}" ]]; then
      echo "    Could not find a block in course outline. Falling back to course key as block." >&2
      BLOCK_ID="${COURSE_KEY}"
    fi
    echo "    Found block: ${BLOCK_ID}"
  fi

  unset _student_token courses_json encoded_course outline_json
fi

echo "============================================"
echo "  E2E: Course → Enroll → Achievements"
echo "============================================"
echo "Course:  ${COURSE_KEY}"
echo "Block:   ${BLOCK_ID}"
echo "Staff:   ${STAFF_USERNAME}"
echo "Student: ${STUDENT_USERNAME}"
echo "Prefix:  ${CODE_PREFIX}"
echo "============================================"

# ── 1. Staff login ──────────────────────────────
echo ""
echo ">>> Step 1: Login as staff (${STAFF_USERNAME})"
staff_login="$(curl -sS -X POST "${BASE_URL}/api/auth/staff/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${STAFF_USERNAME}\",\"password\":\"${STAFF_PASSWORD}\"}")"
staff_token="$(extract_token "${staff_login}")"
if [[ -z "${staff_token}" ]]; then
  echo "Staff login failed: ${staff_login}" >&2; exit 1
fi
echo "    Staff token acquired"

# ── 2. Seed achievements with icons ─────────────
echo ""
echo ">>> Step 2: Seed achievement types with icons"
"${SCRIPT_DIR}/staff_seed_achievement_images.sh" "${staff_token}" "${COURSE_KEY}" "${BLOCK_ID}" "${CODE_PREFIX}"

# ── 3. Student login ────────────────────────────
echo ""
echo ">>> Step 3: Login as student (${STUDENT_USERNAME})"
student_login="$(curl -sS -X POST "${BASE_URL}/api/auth/openedx_user/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${STUDENT_USERNAME}\",\"password\":\"${STUDENT_PASSWORD}\"}")"
student_token="$(extract_token "${student_login}")"
if [[ -z "${student_token}" ]]; then
  echo "Student login failed: ${student_login}" >&2; exit 1
fi
echo "    Student token acquired"

# ── 4. Baseline state ───────────────────────────
echo ""
echo ">>> Step 4: Baseline state"
before_balance="$(curl -sS "${BASE_URL}/api/student/balance" \
  -H "Authorization: Bearer ${student_token}" | jq -r '.currentBalance // .balance // 0')"
before_count="$(curl -sS "${BASE_URL}/api/student/achievements" \
  -H "Authorization: Bearer ${student_token}" | jq 'length')"
echo "    balance=${before_balance}, achievements=${before_count}"

# ── 5. Enroll ───────────────────────────────────
echo ""
echo ">>> Step 5: Enroll in course"
encoded_course="$(uri_encode "${COURSE_KEY}")"
enroll_resp="$(curl -sS -X POST "${BASE_URL}/api/student/courses/${encoded_course}/enroll" \
  -H "Authorization: Bearer ${student_token}")"
echo "${enroll_resp}" | jq .

# ── 6. Complete lesson ──────────────────────────
echo ""
echo ">>> Step 6: Complete lesson"
encoded_block="$(uri_encode "${BLOCK_ID}")"
complete_resp="$(curl -sS -X POST "${BASE_URL}/api/student/lessons/${encoded_course}/${encoded_block}/complete" \
  -H "Authorization: Bearer ${student_token}")"
echo "${complete_resp}" | jq .

# ── 7. Achievements after actions ───────────────
echo ""
echo ">>> Step 7: Achievements after actions"
after_achievements="$(curl -sS "${BASE_URL}/api/student/achievements" \
  -H "Authorization: Bearer ${student_token}")"
after_balance="$(curl -sS "${BASE_URL}/api/student/balance" \
  -H "Authorization: Bearer ${student_token}" | jq -r '.currentBalance // .balance // 0')"
after_count="$(echo "${after_achievements}" | jq 'length')"

echo ""
echo "    Balance: ${before_balance} → ${after_balance} (+$(( after_balance - before_balance )))"
echo "    Achievements: ${before_count} → ${after_count}"

echo ""
echo "    Achieved achievements with icon URLs:"
echo "${after_achievements}" | jq --arg prefix "${CODE_PREFIX}" '
  [.[] | select(.achievementCode | startswith($prefix)) |
    {
      code: .achievementCode,
      name: .achievementName,
      points: .pointsAwarded,
      iconUrl: .iconUrl,
      earnedAt: .earnedAt
    }
  ]'

# ── 8. Available rewards ────────────────────────
echo ""
echo ">>> Step 8: Available rewards"
curl -sS "${BASE_URL}/api/student/rewards" \
  -H "Authorization: Bearer ${student_token}" | jq '.[] | {name, pointsCost, iconUrl}'

# ── 9. Redeem a reward ──────────────────────────
echo ""
echo ">>> Step 9: Redeem first available reward"
reward_id="$(curl -sS "${BASE_URL}/api/student/rewards" \
  -H "Authorization: Bearer ${student_token}" | jq -r '.[0].rewardId // empty')"
if [[ -n "${reward_id}" ]]; then
  redeem_resp="$(curl -sS -X POST "${BASE_URL}/api/student/rewards/${reward_id}/redeem" \
    -H "Authorization: Bearer ${student_token}")"
  echo "${redeem_resp}" | jq '{userRewardId, rewardName: .reward.name, pointsCost: .reward.pointsCost}'
  echo ""
  echo "    Balance after redeem:"
  curl -sS "${BASE_URL}/api/student/balance" \
    -H "Authorization: Bearer ${student_token}" | jq '{currentBalance, totalSpent}'
else
  echo "    No rewards available to redeem"
fi

# ── 10. Journal entries from pg-journal ────────
echo ""
echo ">>> Step 10: Ledger events from pg-journal"
cd "${SCRIPT_DIR}/.."
source .env
docker compose exec -T pg-journal psql \
  -U "${JOURNAL_POSTGRES_USER}" \
  -d "${JOURNAL_POSTGRES_DB}" \
  -c "SELECT event_type, amount, balance_before, balance_after, description, occurred_at FROM ledger_events ORDER BY occurred_at DESC LIMIT 10;"

echo ""
echo "============================================"
echo "  E2E finished successfully"
echo "============================================"
