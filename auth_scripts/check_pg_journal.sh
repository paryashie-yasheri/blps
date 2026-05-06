#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

require_tools curl jq docker

"${SCRIPT_DIR}/login_as_user.sh" "${EDX_API_TEST_USERNAME}" "${EDX_API_TEST_PASSWORD}"

# 3) Донат (это должно записать событие DONATION в ledger_events)
TOKEN="$(load_token)"
json_request POST "${BASE_URL}/api/user-balance/donate" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"amount": 25}' | jq .

# 4) Проверка журнала в pg-journal
cd "${REPO_DIR}"
docker compose exec -T pg-journal psql \
  -U "${JOURNAL_POSTGRES_USER}" \
  -d "${JOURNAL_POSTGRES_DB}" \
  -c "select event_type, amount, balance_before, balance_after, occurred_at from ledger_events order by occurred_at desc limit 10;"
