#!/bin/bash
source ../.env
curl -s -X POST "${SPRING_HOST}/api/auth/openedx_user/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$1\",\"password\":\"$2\"}" | jq .
