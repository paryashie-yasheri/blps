#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

source .env

echo "============================================"
echo " BLPS Dev Environment Starter"
echo "============================================"

# 1. Rebuild the app
echo ""
echo "[1/4] Building the app..."
./gradlew bootJar --quiet

# 2. Start ALL infra at once (so compose doesn't recreate containers)
echo ""
echo "[2/4] Starting infrastructure (PostgreSQL ×3, Keycloak, Redis)..."
docker compose -f docker-compose.dev.yml up -d pg pg-rewards pg-journal redis keycloak

echo "  Waiting for PostgreSQL..."
for db in pg pg-rewards pg-journal; do
  until docker compose -f docker-compose.dev.yml exec -T "$db" pg_isready -U postgres 2>/dev/null; do sleep 1; done
  echo "    $db ready"
done

echo "  Waiting for Keycloak..."
for i in $(seq 1 60); do
  if curl -sf "http://localhost:8081/realms/${KEYCLOAK_MASTER_REALM}/.well-known/openid-configuration" >/dev/null 2>&1; then
    echo "  Keycloak ready."
    break
  fi
  if [ "$i" -eq 60 ]; then
    echo "  ERROR: Keycloak did not start in time. Check: docker compose -f docker-compose.dev.yml logs keycloak"
    exit 1
  fi
  sleep 2
done

# 3. Start Pulsar (optional)
echo ""
echo "[3/4] Starting Pulsar..."
docker compose -f docker-compose.dev.yml up -d pulsar 2>/dev/null || true
sleep 3

# 4. Start Spring Boot app
echo ""
echo "[4/4] Starting Spring Boot app on port ${SPRING_PORT}..."
echo ""
java -jar build/libs/demo-0.0.1-SNAPSHOT.jar
