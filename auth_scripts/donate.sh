#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

require_tools curl jq

token="$(load_token)"
amount="${1:-100}"

json_request POST "${BASE_URL}/api/user-balance/donate" \
  -H "Authorization: Bearer ${token}" \
  -H "Content-Type: application/json" \
  -d "{\"amount\": ${amount}}" | jq .
