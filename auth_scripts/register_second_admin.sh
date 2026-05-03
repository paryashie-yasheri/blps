#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

require_tools curl jq

token="$(load_token)"

json_request POST "${BASE_URL}/api/auth/register-admin" \
  -H "Authorization: Bearer ${token}" \
  -H "Content-Type: application/json" \
  -d '{"username": "admin2", "password": "admin", "email": "admin2@admin.com"}' | jq .
