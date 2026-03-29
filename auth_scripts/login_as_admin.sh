#!/usr/bin/env bash

source ../.env

curl -v -c cookies.txt -X POST "${SPRING_HOST}/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USERNAME}" \
  -d "password=${ADMIN_PASSWORD}"
