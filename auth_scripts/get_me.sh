#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

require_tools curl jq

token="$(load_token)"
json_request GET "${BASE_URL}/api/auth/me" \
  -H "Authorization: Bearer ${token}" | jq .
