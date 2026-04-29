#!/usr/bin/env bash

source ../.env

token=$(cat .token.txt)

curl -sS -X POST "${SPRING_HOST}/api/user-balance/donate" \
  -H "Authorization: Bearer ${token}" \
  -H "Content-Type: application/json" \
  -d '{"amount": 100}' | jq .
