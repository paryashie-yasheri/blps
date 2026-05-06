#!/bin/bash
set -e

CONFIG_FILE="/opt/keycloak/data/import/realm-config.json"

echo "Substituting environment variables in realm-config.json..."
sed -i "s|\${KEYCLOAK_MASTER_CLIENT_SECRET}|${KEYCLOAK_MASTER_CLIENT_SECRET}|g" "$CONFIG_FILE"
sed -i "s|\${KEYCLOAK_SERVER_URL}|${KEYCLOAK_SERVER_URL}|g" "$CONFIG_FILE"

echo "Environment variables substituted successfully"

exec /opt/keycloak/bin/kc.sh "$@"