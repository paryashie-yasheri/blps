#!/bin/bash
source ../.env
response=$(curl -s -X POST "${SPRING_HOST}/api/auth/staff/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$1\",\"password\":\"$2\"}")

echo "${response}" | jq .
token=$(echo "${response}" | jq -r '.accessToken')
echo "${token}" > .token.txt
