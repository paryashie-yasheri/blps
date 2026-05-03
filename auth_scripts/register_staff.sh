#!/usr/bin/env bash

source ../.env

token=$(cat .token.txt)
echo "$token"

echo "=== Registering staff user: $1 ==="
response=$(curl -s -v -X POST "${SPRING_HOST}/api/auth/register-staff" \
  -H "Authorization: Bearer ${token}" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$1\",\"password\":\"$2\",\"email\":\"$3\"}")

echo "Registration response:"
echo "$response" | jq .

if echo "$response" | jq -e '.error' > /dev/null 2>&1; then
  echo "Registration failed with error"
  exit 1
fi

echo "=== Login as staff user ==="
login_response=$(curl -s -X POST "${SPRING_HOST}/api/auth/staff/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$1\",\"password\":\"$2\"}")

echo "Login response:"
echo "$login_response" | jq .

token=$(echo "$login_response" | jq -r '.access_token // empty')

if [ -z "$token" ] || [ "$token" == "null" ]; then
  echo "Failed to get token"
  exit 1
fi

echo "Token: $token"

echo -e "\n=== Get Me ==="
curl -s -X GET "${SPRING_HOST}/api/auth/me" \
  -H "Authorization: Bearer ${token}" | jq .
