#!/bin/bash
set -e

KEYCLOAK_PASSWORD="${KC_DB_PASSWORD:-keycloak_password}"

psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" <<-EOSQL
    -- Create keycloak user
    CREATE ROLE keycloak WITH LOGIN PASSWORD '$KEYCLOAK_PASSWORD';

    -- Grant connect to the database
    GRANT CONNECT ON DATABASE studs TO keycloak;

    -- Create keycloak schema
    CREATE SCHEMA IF NOT EXISTS keycloak;

    -- Grant permissions on keycloak schema
    GRANT USAGE, CREATE ON SCHEMA keycloak TO keycloak;
    GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA keycloak TO keycloak;
    GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA keycloak TO keycloak;

    -- Grant permissions on public schema (for cross-schema FKs if needed)
    GRANT USAGE ON SCHEMA public TO keycloak;
    GRANT SELECT ON ALL TABLES IN SCHEMA public TO keycloak;

    -- Set default privileges for future objects
    ALTER DEFAULT PRIVILEGES IN SCHEMA keycloak
        GRANT ALL PRIVILEGES ON TABLES TO keycloak;

    ALTER DEFAULT PRIVILEGES IN SCHEMA keycloak
        GRANT ALL PRIVILEGES ON SEQUENCES TO keycloak;
EOSQL

echo "Keycloak user created successfully"
