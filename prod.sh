#!/usr/bin/env bash

./gradlew jibDockerBuild && \
docker compose -f docker-compose.prod.yml up --build
