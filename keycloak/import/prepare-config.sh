#!/bin/bash
set -e

CONFIG_FILE="/opt/keycloak/data/import/realm-config.json"

if [ -f "$CONFIG_FILE" ]; then
    echo "Substituting environment variables in realm-config.json..."

    # Replace placeholders with actual env values
    sed -i "s|\${KEYCLOAK_MASTER_CLIENT_SECRET}|${KEYCLOAK_MASTER_CLIENT_SECRET}|g" "$CONFIG_FILE"
    sed -i "s|\${KEYCLOAK_SERVER_URL}|${KEYCLOAK_SERVER_URL}|g" "$CONFIG_FILE"

    echo "Done. Client secret is now: ${KEYCLOAK_MASTER_CLIENT_SECRET}"
else
    echo "Config file not found: $CONFIG_FILE"
    exit 1
fi