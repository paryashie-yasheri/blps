#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

set -a
source "$SCRIPT_DIR/../../.env"
set +a

envsubst < "$SCRIPT_DIR/realm-config.json"
