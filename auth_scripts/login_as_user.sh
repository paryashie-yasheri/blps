#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

require_tools curl jq

response="$(login_openedx_user "${1:?Usage: $0 <username> <password>}" "${2:?Usage: $0 <username> <password>}")"
save_token "$(echo "${response}" | extract_token)"
echo "${response}" | jq '{username, role, token_type, expires_in}'
