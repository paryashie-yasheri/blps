#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

require_tools curl jq

response="$(login_staff "${ADMIN_USERNAME}" "${ADMIN_PASSWORD}")"
save_token "$(echo "${response}" | extract_token)"
echo "${response}" | jq '{username, role, tokenType, expiresIn}'
