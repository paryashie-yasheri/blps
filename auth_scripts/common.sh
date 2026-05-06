#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

source "${REPO_DIR}/.env"

BASE_URL="${SPRING_HOST%/}"
TOKEN_FILE="${SCRIPT_DIR}/.token.txt"

require_tool() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required tool: $1" >&2
    exit 1
  }
}

require_tools() {
  for tool in "$@"; do
    require_tool "${tool}"
  done
}

json_request() {
  local method="$1"
  local url="$2"
  shift 2

  local response
  response="$(mktemp)"
  local status
  status="$(curl -sS -o "${response}" -w "%{http_code}" -X "${method}" "${url}" "$@")"

  if [[ "${status}" -lt 200 || "${status}" -ge 300 ]]; then
    echo "HTTP ${status}: ${method} ${url}" >&2
    cat "${response}" >&2
    echo >&2
    rm -f "${response}"
    exit 1
  fi

  if jq -e 'type == "object" and has("status") and (.status >= 400)' "${response}" >/dev/null 2>&1; then
    echo "Application error: ${method} ${url}" >&2
    cat "${response}" >&2
    echo >&2
    rm -f "${response}"
    exit 1
  fi

  cat "${response}"
  rm -f "${response}"
}

extract_token() {
  if [[ "$#" -gt 0 ]]; then
    printf "%s" "$1" | jq -r '.access_token // empty'
  else
    jq -r '.access_token // empty'
  fi
}

save_token() {
  local token="$1"
  if [[ -z "${token}" || "${token}" == "null" ]]; then
    echo "Response does not contain access_token" >&2
    exit 1
  fi
  printf "%s" "${token}" > "${TOKEN_FILE}"
}

load_token() {
  if [[ ! -s "${TOKEN_FILE}" ]]; then
    echo "Token file not found: ${TOKEN_FILE}. Run a login script first." >&2
    exit 1
  fi
  cat "${TOKEN_FILE}"
}

login_staff() {
  local username="$1"
  local password="$2"
  json_request POST "${BASE_URL}/api/auth/staff/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"${username}\",\"password\":\"${password}\"}"
}

login_openedx_user() {
  local username="$1"
  local password="$2"
  json_request POST "${BASE_URL}/api/auth/openedx_user/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"${username}\",\"password\":\"${password}\"}"
}

uri_encode() {
  jq -rn --arg value "$1" '$value|@uri'
}
