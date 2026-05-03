#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

STAFF_TOKEN="${1:?Usage: $0 <staff-jwt-token> <course-key> <block-id> [code-prefix]}"
COURSE_KEY="${2:?Usage: $0 <staff-jwt-token> <course-key> <block-id> [code-prefix]}"
BLOCK_ID="${3:?Usage: $0 <staff-jwt-token> <course-key> <block-id> [code-prefix]}"
CODE_PREFIX="${4:-seed_ach_$(date +%s)}"

require_tools curl jq

events=(
  "lesson_complete|${SCRIPT_DIR}/complete_lesson.png|Lesson Complete|25|1.0|${BLOCK_ID}"
  "subsection_complete|${SCRIPT_DIR}/subsection_complete.png|Subsection Complete|35|1.0|${BLOCK_ID}"
  "course_progress_updated|${SCRIPT_DIR}/course_progress_updated.png|Course Progress Updated|45|0.0|"
  "course_complete|${SCRIPT_DIR}/complete_course.png|Course Complete|60|0.0|"
  "course_enrolled|${SCRIPT_DIR}/course_enrolled.png|Course Enrolled|15|null|"
)

list_response="$(json_request GET "${BASE_URL}/api/achievements" \
  -H "Authorization: Bearer ${STAFF_TOKEN}")"

echo "=== Upsert achievements with icons from auth_scripts ==="
echo "courseKey=${COURSE_KEY}"
echo "blockId=${BLOCK_ID}"
echo "prefix=${CODE_PREFIX}"

for descriptor in "${events[@]}"; do
  IFS='|' read -r event image_file name_suffix points min_completion block_pattern <<< "${descriptor}"

  if [[ ! -f "${image_file}" ]]; then
    echo "Image file not found: ${image_file}" >&2
    exit 1
  fi

  code="${CODE_PREFIX}_${event}"
  name="${CODE_PREFIX} ${name_suffix}"
  description="E2E achievement for ${event}"

  request_json="$(jq -cn \
    --arg code "${code}" \
    --arg name "${name}" \
    --arg description "${description}" \
    --arg eventType "${event}" \
    --arg coursePattern "${COURSE_KEY}" \
    --arg blockPattern "${block_pattern}" \
    --argjson minCompletion "${min_completion}" \
    --argjson pointsAwarded "${points}" \
    '{
      code: $code,
      name: $name,
      description: $description,
      pointsAwarded: $pointsAwarded,
      isRepeatable: false,
      isActive: true,
      conditions: [
        {
          eventType: $eventType,
          coursePattern: $coursePattern,
          blockPattern: (if $blockPattern == "" then null else $blockPattern end),
          minCompletion: $minCompletion
        }
      ]
    }')"

  existing_id="$(echo "${list_response}" | jq -r --arg code "${code}" '.[] | select(.code == $code) | .achievementTypeId' | head -n1)"

  if [[ -n "${existing_id}" && "${existing_id}" != "null" ]]; then
    echo ""
    echo "Updating ${code} (${existing_id})"
    response="$(json_request PUT "${BASE_URL}/api/achievements/${existing_id}" \
      -H "Authorization: Bearer ${STAFF_TOKEN}" \
      -F "request=${request_json};type=application/json" \
      -F "image=@${image_file};type=image/png")"
  else
    echo ""
    echo "Creating ${code}"
    response="$(json_request POST "${BASE_URL}/api/achievements" \
      -H "Authorization: Bearer ${STAFF_TOKEN}" \
      -F "request=${request_json};type=application/json" \
      -F "image=@${image_file};type=image/png")"
  fi

  echo "${response}" | jq '{achievementTypeId, code, iconKey, iconUrl, eventType: .conditions[0].eventType}'
done

echo ""
echo "Done. Created/updated achievements are prefixed with: ${CODE_PREFIX}_"
