#!/bin/bash
set -e

CONFIG_FILE="/opt/keycloak/data/import/realm-config.json"

if [ -f "$CONFIG_FILE" ]; then
    echo "Substituting environment variables in realm-config.json..."

    # Replace placeholders with actual env values
    sed -i "s|\${KEYCLOAK_ADMIN}|${KEYCLOAK_ADMIN}|g" "$CONFIG_FILE"
    sed -i "s|\${KEYCLOAK_ADMIN_EMAIL}|${KEYCLOAK_ADMIN_EMAIL}|g" "$CONFIG_FILE"
    sed -i "s|\${KEYCLOAK_ADMIN_PASSWORD}|${KEYCLOAK_ADMIN_PASSWORD}|g" "$CONFIG_FILE"
    sed -i "s|\${KEYCLOAK_MASTER_REALM}|${KEYCLOAK_MASTER_REALM}|g" "$CONFIG_FILE"
    sed -i "s|\${KEYCLOAK_MASTER_CLIENT_ID}|${KEYCLOAK_MASTER_CLIENT_ID}|g" "$CONFIG_FILE"
    sed -i "s|\${KEYCLOAK_MASTER_CLIENT_SECRET}|${KEYCLOAK_MASTER_CLIENT_SECRET}|g" "$CONFIG_FILE"
    sed -i "s|\${KEYCLOAK_SERVER_URL}|${KEYCLOAK_SERVER_URL}|g" "$CONFIG_FILE"

    echo "Done. Admin user: ${KEYCLOAK_ADMIN}, Realm: ${KEYCLOAK_MASTER_REALM}, Client: ${KEYCLOAK_MASTER_CLIENT_ID}"
else
    echo "Config file not found: $CONFIG_FILE"
    exit 1
fi
