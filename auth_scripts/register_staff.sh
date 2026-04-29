#!/usr/bin/env bash

source ../.env

# Get admin token
echo "=== Getting admin token ==="
admin_response=$(curl -s -X POST "${SPRING_HOST}/api/auth/staff/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin\"}")

echo "$admin_response" | jq .

admin_token=$(echo "$admin_response" | jq -r '.accessToken // empty')

if [ -z "$admin_token" ] || [ "$admin_token" == "null" ]; then
  echo "Failed to get admin token"
  exit 1
fi

echo "Admin token obtained"

# Register staff user
echo "=== Registering staff user: $1 ==="
response=$(curl -s -X POST "${SPRING_HOST}/api/auth/register-staff" \
  -H "Authorization: Bearer ${admin_token}" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$1\",\"password\":\"$2\",\"email\":\"$3\"}")

echo "Registration response:"
echo "$response" | jq .

# Check for errors
if echo "$response" | jq -e '.error' > /dev/null 2>&1; then
  echo "Registration failed with error"
  exit 1
fi

# Login with new user
echo "=== Login as staff user ==="
login_response=$(curl -s -X POST "${SPRING_HOST}/api/auth/staff/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$1\",\"password\":\"$2\"}")

echo "Login response:"
echo "$login_response" | jq .

token=$(echo "$login_response" | jq -r '.accessToken // empty')

if [ -z "$token" ] || [ "$token" == "null" ]; then
  echo "Failed to get token"
  exit 1
fi

echo "Token: $token"

# Get user info
echo -e "\n=== Get Me ==="
curl -s -X GET "${SPRING_HOST}/api/auth/me" \
  -H "Authorization: Bearer ${token}" | jq .
