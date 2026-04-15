#!/bin/bash
source ../.env
token=$(curl -s -X POST "${SPRING_HOST}/api/auth/staff/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$1\",\"password\":\"$2\"}")
echo "$token" | jq .
export token=$(echo "$token" | jq -r '.accessToken')
