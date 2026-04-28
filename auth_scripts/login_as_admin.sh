#!/usr/bin/env bash

source ../.env

response=$(curl -s --json "{\"username\":\"${ADMIN_USERNAME}\",\"password\":\"${ADMIN_PASSWORD}\"}" \
  "${SPRING_HOST}/api/auth/staff/login")

echo "${response}" | jq .

token=$(echo "$response" | jq -r '.accessToken')

echo "${token}"
