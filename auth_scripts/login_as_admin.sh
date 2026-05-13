#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

require_tools curl jq

response="$(login_staff "${KEYCLOAK_ADMIN}" "${KEYCLOAK_ADMIN_PASSWORD}")"
save_token "$(echo "${response}" | extract_token)"
echo "${response}" | jq '{username, role, token_type, expires_in}'
