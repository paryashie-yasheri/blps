#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

IMAGE_TAG="${IMAGE_TAG:-blps-spring-native:latest}"
DATALAKE_IMAGE="${DATALAKE_IMAGE:-blps-datalake-sync-native:latest}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"

echo "=== BLPS Production Build (Local jOOQ + GraalVM + Jib) ==="
echo "Repo:   $REPO_ROOT"
echo "Image:  $IMAGE_TAG"
echo ""

# ── Pre-flight: check Java 21 is available ───────────────────────────────────
if ! java -version 2>&1 | grep -q 'version "21'; then
    echo "ERROR: Java 21 is required. Found:"
    java -version 2>&1 | head -1
    exit 1
fi

# ── Step 1: Generate jOOQ (uses Testcontainers to spin up Postgres automatically) ─
echo "Step 1: Generating jOOQ sources (Testcontainers will start a temp Postgres)..."
./gradlew generateJooq --no-daemon 2>&1 | tail -20
if [ ${PIPESTATUS[0]} -ne 0 ]; then
    echo "ERROR: jOOQ generation failed"
    exit 1
fi

# ── Step 2: Build Docker images ───────────────────────────────────────────────
echo ""
echo "Step 2: Building Docker images (GraalVM runs inside Docker — no local GraalVM needed)..."

# Build main app (GraalVM compiled inside Docker)
docker build \
    -f Dockerfile.native \
    --target runtime \
    --tag "$IMAGE_TAG" \
    --progress=plain \
    "$REPO_ROOT" 2>&1 | tail -20

if [ ${PIPESTATUS[0]} -ne 0 ]; then
    echo "ERROR: Docker build failed"
    exit 1
fi

# Build datalake-sync (GraalVM compiled inside Docker)
cd "$REPO_ROOT/datalake-sync"
docker build \
    -f Dockerfile.graalvm \
    --target runtime \
    --tag "$DATALAKE_IMAGE" \
    --progress=plain \
    . 2>&1 | tail -20

echo ""
echo "=== All builds successful ==="
echo ""
echo "Image sizes:"
docker image inspect "$IMAGE_TAG" --format='{{.Size}}' 2>/dev/null | python3 -c "
import sys; size = int(sys.stdin.read().strip()); print(f'  spring:   {size/1024/1024:.1f} MB')
" 2>/dev/null
docker image inspect "$DATALAKE_IMAGE" --format='{{.Size}}' 2>/dev/null | python3 -c "
import sys; size = int(sys.stdin.read().strip()); print(f'  datalake-sync: {size/1024/1024:.1f} MB')
" 2>/dev/null
echo ""
echo "To start all services:"
echo "  cd $REPO_ROOT"
echo "  docker compose -f $COMPOSE_FILE up -d"
echo ""
echo "To start only app services (no build):"
echo "  docker compose -f $COMPOSE_FILE up -d spring datalake-sync"