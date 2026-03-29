#!/usr/bin/env bash

source ../.env

curl -i -b cookies.txt -X POST "${SPRING_HOST}/api/auth/register-admin" \
  -H "Content-Type: application/json" \
  -d '{"username": "admin2", "password": "admin", "email": "admin@admin.com"}'
