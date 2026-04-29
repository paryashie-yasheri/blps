#!/bin/bash
source ../.env

# Try to login as staff first (if user exists)
echo "=== Attempting staff login ==="
response=$(curl -s -X POST "${SPRING_HOST}/api/auth/staff/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$1\",\"password\":\"$2\"}")

echo "$response" | jq .

# Check if login failed - user might not exist, try creating via register-staff (needs admin)
if echo "$response" | jq -e '.error' > /dev/null 2>&1; then
  echo "Login failed, trying to register as admin first..."
  
  # Try to get admin token first
  admin_response=$(curl -s -X POST "${SPRING_HOST}/api/auth/staff/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"admin\",\"password\":\"admin\"}")
  
  admin_token=$(echo "$admin_response" | jq -r '.accessToken // empty')
  
  if [ -n "$admin_token" ] && [ "$admin_token" != "null" ]; then
    echo "Admin login success, registering staff user..."
    
    # Register the staff user
    register_response=$(curl -s -X POST "${SPRING_HOST}/api/auth/register-staff" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer ${admin_token}" \
      -d "{\"username\":\"$1\",\"password\":\"$2\",\"email\":\"$1@blps.local\"}")
    
    echo "Registered: $register_response" | jq .
    
    # Now try login again
    response=$(curl -s -X POST "${SPRING_HOST}/api/auth/staff/login" \
      -H "Content-Type: application/json" \
      -d "{\"username\":\"$1\",\"password\":\"$2\"}")
    
    echo "Login response: $response" | jq .
  fi
fi

token=$(echo "$response" | jq -r '.accessToken // empty')

if [ -z "$token" ] || [ "$token" == "null" ]; then
  echo "Failed to get token"
  exit 1
fi

echo "Token: $token"

echo -e "\n=== Get Me ==="
curl -s -X GET "${SPRING_HOST}/api/auth/me" \
  -H "Authorization: Bearer ${token}" | jq .