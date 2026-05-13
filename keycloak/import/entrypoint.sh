#!/bin/bash
set -e

CONFIG_FILE="/opt/keycloak/data/import/realm-config.json"

echo "Substituting environment variables in realm-config.json..."
sed -i "s|\${KEYCLOAK_ADMIN}|${KEYCLOAK_ADMIN}|g" "$CONFIG_FILE"
sed -i "s|\${KEYCLOAK_ADMIN_EMAIL}|${KEYCLOAK_ADMIN_EMAIL}|g" "$CONFIG_FILE"
sed -i "s|\${KEYCLOAK_ADMIN_PASSWORD}|${KEYCLOAK_ADMIN_PASSWORD}|g" "$CONFIG_FILE"
sed -i "s|\${KEYCLOAK_MASTER_REALM}|${KEYCLOAK_MASTER_REALM}|g" "$CONFIG_FILE"
sed -i "s|\${KEYCLOAK_MASTER_CLIENT_ID}|${KEYCLOAK_MASTER_CLIENT_ID}|g" "$CONFIG_FILE"
sed -i "s|\${KEYCLOAK_MASTER_CLIENT_SECRET}|${KEYCLOAK_MASTER_CLIENT_SECRET}|g" "$CONFIG_FILE"
sed -i "s|\${KEYCLOAK_SERVER_URL}|${KEYCLOAK_SERVER_URL}|g" "$CONFIG_FILE"

echo "Environment variables substituted successfully"

exec /opt/keycloak/bin/kc.sh "$@"
