#!/usr/bin/env bash

source ../.env

echo "=== Testing Email validation ==="
echo '{"username":"testuser","password":"testpass","email":"test@mail.com"}' | jq .

echo "=== Testing admin login ==="
admin_response=$(curl -s -X POST "${SPRING_HOST}/api/auth/staff/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin\"}")

echo "$admin_response" | jq .

admin_token=$(echo "$admin_response" | jq -r '.accessToken // empty')

if [ -z "$admin_token" ] || [ "$admin_token" == "null" ]; then
  echo "Failed to get admin token"
  exit 1
fi

echo "=== Testing staff registration with simple email ==="
response=$(curl -s -X POST "${SPRING_HOST}/api/auth/register-staff" \
  -H "Authorization: Bearer ${admin_token}" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"testuser\",\"password\":\"testpass\",\"email\":\"test@mail.com\"}")

echo "Registration response:"
echo "$response" | jq .

# Check response code
http_code=$(echo "$response" | jq -r '.status // empty' | head -c 3)
if [ "$http_code" = "500" ]; then
  echo "ERROR: 500 Internal Server Error"
  exit 1
fi

echo "=== Testing staff registration with admin email ==="
response=$(curl -s -X POST "${SPRING_HOST}/api/auth/register-staff" \
  -H "Authorization: Bearer ${admin_token}" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"adminuser\",\"password\":\"testpass\",\"email\":\"adminuser@test.com\"}")

echo "Registration response:"
echo "$response" | jq .