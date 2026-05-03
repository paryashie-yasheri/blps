#!/bin/bash
source ../.env
response=$(curl -s -X POST "${SPRING_HOST}/api/auth/openedx_user/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$1\",\"password\":\"$2\"}" | jq .)

echo "${response}" | jq .
token=$(echo "${response}" | jq -r '.access_token')
echo "${token}" > .token.txt
