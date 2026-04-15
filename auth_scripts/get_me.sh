#!/bin/bash
source ../.env
curl -s -H "Authorization: Bearer $1" "${SPRING_HOST}/api/auth/me"
