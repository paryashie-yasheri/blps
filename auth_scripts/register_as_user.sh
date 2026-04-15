#!/bin/bash
source ../.env
curl -s -X POST "${SPRING_HOST}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$1\",\"password\":\"$2\",\"email\":\"$3\"}" | jq .
