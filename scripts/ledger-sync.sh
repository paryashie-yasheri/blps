#!/usr/bin/env bash
set -e

BASE_URL="${DATALAKE_SYNC_URL:-http://localhost:8090}"

echo "=== Ledger Sync ==="
echo "Calling: ${BASE_URL}/ledger/sync"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/ledger/sync")
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | head -n -1)

if [ "$HTTP_CODE" = "200" ]; then
    echo "Sync completed successfully:"
    echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
else
    echo "Sync failed (HTTP $HTTP_CODE):"
    echo "$BODY"
    exit 1
fi
