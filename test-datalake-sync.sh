#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/.env"

BASE_URL="${SPRING_HOST}"
DATALAKE_URL="http://localhost:8090"
USERNAME="${EDX_API_TEST_USERNAME}"
PASSWORD="${EDX_API_TEST_PASSWORD}"

PG_JOURNAL_CONTAINER="blps-pg-journal-prod"

echo "=== Step 1: Login ==="
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/auth/openedx_user/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")

echo "Login response: $LOGIN_RESPONSE"

TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"access_token":"[^"]*' | sed 's/"access_token":"//')
if [ -z "$TOKEN" ]; then
    echo "ERROR: Failed to get token"
    exit 1
fi
echo "Token obtained: ${TOKEN:0:20}..."

echo -e "\n=== Step 2: Donate points (create ledger entry) ==="
DONATE_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/user-balance/donate" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"amount": 100}')

echo "Donate response: $DONATE_RESPONSE"

if ! echo "$DONATE_RESPONSE" | grep -q "balanceAfter"; then
    echo "ERROR: Donate failed"
    exit 1
fi

echo -e "\n=== Step 3: Query ledger DB count (studs_journal) ==="
DB_COUNT=$(docker exec ${PG_JOURNAL_CONTAINER} psql -U postgres -d studs_journal -t -c "SELECT COUNT(*) FROM ledger_events;" 2>/dev/null | xargs)
echo "Ledger DB count: $DB_COUNT"

if [ -z "$DB_COUNT" ]; then
    echo "ERROR: Failed to get DB count"
    exit 1
fi

echo -e "\n=== Step 4: Query datalake count (before sync) ==="
DATALAKE_COUNT_BEFORE=$(curl -s -X GET "${DATALAKE_URL}/ledger/count")
echo "Datalake count (before sync): $DATALAKE_COUNT_BEFORE"

COUNT_VALUE_BEFORE=$(echo "$DATALAKE_COUNT_BEFORE" | jq -r '.stdout' 2>/dev/null | head -20 | grep -E '^[0-9]+$' | head -1)

if [ -z "$COUNT_VALUE_BEFORE" ]; then
    COUNT_VALUE_BEFORE=0
fi
echo "Parsed datalake count: $COUNT_VALUE_BEFORE"

echo -e "\n=== Step 5: Manual sync ==="
SYNC_RESPONSE=$(curl -s -X POST "${DATALAKE_URL}/ledger/sync")
echo "Sync response: $SYNC_RESPONSE"

if echo "$SYNC_RESPONSE" | grep -q '"status":"error"'; then
    echo "ERROR: Sync failed"
    exit 1
fi

echo -e "\n=== Step 6: Query datalake count (after sync) ==="
DATALAKE_COUNT_AFTER=$(curl -s -X GET "${DATALAKE_URL}/ledger/count")
echo "Datalake count (after sync): $DATALAKE_COUNT_AFTER"

COUNT_VALUE_AFTER=$(echo "$DATALAKE_COUNT_AFTER" | jq -r '.stdout' 2>/dev/null | head -20 | grep -E '^[0-9]+$' | head -1)

if [ -z "$COUNT_VALUE_AFTER" ]; then
    COUNT_VALUE_AFTER=0
fi
echo "Parsed datalake count: $COUNT_VALUE_AFTER"

echo -e "\n=== Results ==="
echo "Ledger DB count:      $DB_COUNT"
echo "Datalake (before):    $COUNT_VALUE_BEFORE"
echo "Datalake (after):     $COUNT_VALUE_AFTER"

if [ "$COUNT_VALUE_AFTER" = "$DB_COUNT" ]; then
    echo -e "\nSUCCESS: Datalake count matches ledger DB count!"
    exit 0
else
    echo -e "\nFAILURE: Datalake count ($COUNT_VALUE_AFTER) does not match ledger DB count ($DB_COUNT)"
    exit 1
fi
