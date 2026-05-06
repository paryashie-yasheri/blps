#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

echo "There is no local OPENEDX_USER registration endpoint." >&2
echo "Use ./login_as_user.sh <openedx-username> <openedx-password>; the service creates the local user after Open edX login." >&2
exit 1
