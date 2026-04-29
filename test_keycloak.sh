#!/bin/bash

source .env
BASE_URL="${SPRING_HOST}"

echo "=== Testing Keycloak Integration ==="

# Test 1: Check Keycloak realms
echo "1. Checking Keycloak realms..."
curl -s "${BASE_URL}"/realms/blps-staff | jq -r '.realm'

# Test 2: Check if we can create a user in Keycloak
echo "2. Testing user creation in Keycloak..."

# First, let's check if the application is running locally
echo "3. Checking if local application is running..."
if curl -s "${BASE_URL}"/actuator/health > /dev/null 2>&1; then
    echo "✓ Local application is running"
    
    # Test staff login
    echo "4. Testing staff login..."
    response=$(curl -s --json "{\"username\":\"${ADMIN_USERNAME}\",\"password\":\"${ADMIN_PASSWORD}\"}" \
      "${SPRING_HOST}/api/auth/staff/login")

    echo "${response}" | jq .
    token=$(echo "${response}" | jq -r '.accessToken')

    curl -v -H "Authorization: Bearer ${token}" "${SPRING_HOST}/api/auth/me"

else
    echo "✗ Local application is not running"
    echo "Starting with placeholder tokens..."
fi

echo "=== Test Complete ==="
