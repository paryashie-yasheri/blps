#!/usr/bin/env bash
set -euo pipefail

CAMUNDA_URL="${CAMUNDA_URL:-http://localhost:1234/engine-rest}"
KEY="${1:-Process_student_workflow}"

echo "Deleting all process instances for key: $KEY"

IDS=$(curl -s "$CAMUNDA_URL/process-instance?processDefinitionKey=$KEY&maxResults=200" \
  | jq '[.[].id]')

COUNT=$(echo "$IDS" | jq 'length')
if [ "$COUNT" -eq 0 ]; then
  echo "No process instances found."
  exit 0
fi

echo "Found $COUNT instance(s). Deleting…"

curl -s -X POST "$CAMUNDA_URL/process-instance/delete" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --arg ids "$IDS" '{processInstanceIds: ($ids|fromjson), deleteReason: "cleanup"}')" \
  | jq .

echo "Deletion submitted. Instances will be cleaned up shortly."
