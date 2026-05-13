#!/bin/bash
set -e

SOURCE_FILE="/opt/keycloak/data/import/realm-config.json"
WORK_FILE="/tmp/realm-config.json"

echo "Substituting environment variables in realm-config.json..."
cp "$SOURCE_FILE" "$WORK_FILE"

sed -i "s|\${KEYCLOAK_ADMIN}|${KEYCLOAK_ADMIN}|g" "$WORK_FILE"
sed -i "s|\${KEYCLOAK_ADMIN_EMAIL}|${KEYCLOAK_ADMIN_EMAIL}|g" "$WORK_FILE"
sed -i "s|\${KEYCLOAK_ADMIN_PASSWORD}|${KEYCLOAK_ADMIN_PASSWORD}|g" "$WORK_FILE"
sed -i "s|\${KEYCLOAK_MASTER_REALM}|${KEYCLOAK_MASTER_REALM}|g" "$WORK_FILE"
sed -i "s|\${KEYCLOAK_MASTER_CLIENT_ID}|${KEYCLOAK_MASTER_CLIENT_ID}|g" "$WORK_FILE"
sed -i "s|\${KEYCLOAK_MASTER_CLIENT_SECRET}|${KEYCLOAK_MASTER_CLIENT_SECRET}|g" "$WORK_FILE"
sed -i "s|\${KEYCLOAK_SERVER_URL}|${KEYCLOAK_SERVER_URL}|g" "$WORK_FILE"

echo "Environment variables substituted successfully"

exec /opt/keycloak/bin/kc.sh "$@"
