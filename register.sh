#!/usr/bin/env bash

source .env

curl -i -X POST "${SPRING_HOST}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username": "john", "password": "secret123", "email": "bigbadjohn@machine.com"}'
