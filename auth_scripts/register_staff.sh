#!/usr/bin/env bash

source ../.env

curl -i -b cookies.txt -X POST "${SPRING_HOST}/api/auth/register-staff" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$1\",\"password\":\"$2\",\"email\":\"$3\"}"
