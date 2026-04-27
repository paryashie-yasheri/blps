#!/bin/bash
source ../.env

token=$(cat .token.txt)
echo "$token"
curl -H "Authorization: Bearer ${token}" "${SPRING_HOST}/api/auth/me"
