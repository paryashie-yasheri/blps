#!/usr/bin/env bash

source ../.env

curl -v -b cookies.txt -X POST "${SPRING_HOST}/api/user-balance/donate" \
  -H "Content-Type: application/json" \
  -d '{"amount": 100}'

