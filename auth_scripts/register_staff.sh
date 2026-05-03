#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

require_tools curl jq

token="$(load_token)"

json_request POST "${BASE_URL}/api/auth/register-staff" \
  -H "Authorization: Bearer ${token}" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${1:?Usage: $0 <username> <password> <email>}\",\"password\":\"${2:?Usage: $0 <username> <password> <email>}\",\"email\":\"${3:?Usage: $0 <username> <password> <email>}\"}" | jq .
