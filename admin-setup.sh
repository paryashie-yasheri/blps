#!/bin/bash
set -e

BASE_URL="${BASE_URL:-http://localhost:1234}"
STAFF_TOKEN="${STAFF_TOKEN:-}"

echo "=== Staff Login ==="
if [ -z "$STAFF_TOKEN" ]; then
    LOGIN_RESP=$(curl -s -X POST "${BASE_URL}/api/auth/staff/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"admin"}')
    STAFF_TOKEN=$(echo "$LOGIN_RESP" | grep -o '"accessToken":"[^"]*' | sed 's/"accessToken":"//')
    echo "Staff token: $STAFF_TOKEN"
fi

echo -e "\n=== Add Achievement Type 1 ==="
curl -s -X POST "${BASE_URL}/api/admin/achievement-types" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${STAFF_TOKEN}" \
    -d '{
        "code": "first_lesson",
        "name": "First Lesson Complete",
        "description": "Complete your first lesson",
        "pointsAwarded": 50,
        "icon": "trophy.png",
        "isRepeatable": false,
        "isActive": true,
        "conditions": {"lesson_complete": 1}
    }'

echo -e "\n=== Add Achievement Type 2 ==="
curl -s -X POST "${BASE_URL}/api/admin/achievement-types" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${STAFF_TOKEN}" \
    -d '{
        "code": "course_complete",
        "name": "Course Complete",
        "description": "Complete an entire course",
        "pointsAwarded": 200,
        "icon": "graduation.png",
        "isRepeatable": false,
        "isActive": true,
        "conditions": {"course_complete": 1}
    }'

echo -e "\n=== Add Achievement Type 3 ==="
curl -s -X POST "${BASE_URL}/api/admin/achievement-types" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${STAFF_TOKEN}" \
    -d '{
        "code": "top_student",
        "name": "Top Student",
        "description": "Achieve highest score in a course",
        "pointsAwarded": 500,
        "icon": "star.png",
        "isRepeatable": false,
        "isActive": true,
        "conditions": {"score": 100}
    }'

echo -e "\n=== Get All Achievement Types ==="
curl -s -X GET "${BASE_URL}/api/admin/achievement-types" \
    -H "Authorization: Bearer ${STAFF_TOKEN}"

echo -e "\n\n=== Add Reward 1 ==="
curl -s -X POST "${BASE_URL}/api/admin/rewards" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${STAFF_TOKEN}" \
    -d '{
        "name": "Extra Credit",
        "description": "Get 100 bonus points",
        "pointsCost": 300,
        "isActive": true,
        "quantity": null
    }'

echo -e "\n=== Add Reward 2 ==="
curl -s -X POST "${BASE_URL}/api/admin/rewards" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${STAFF_TOKEN}" \
    -d '{
        "name": "Certificate",
        "description": "Official course completion certificate",
        "pointsCost": 500,
        "isActive": true,
        "quantity": 10
    }'

echo -e "\n=== Add Reward 3 ==="
curl -s -X POST "${BASE_URL}/api/admin/rewards" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${STAFF_TOKEN}" \
    -d '{
        "name": "T-Shirt",
        "description": "Exclusive platform t-shirt",
        "pointsCost": 1000,
        "isActive": true,
        "quantity": 5
    }'

echo -e "\n=== Get All Rewards ==="
curl -s -X GET "${BASE_URL}/api/admin/rewards" \
    -H "Authorization: Bearer ${STAFF_TOKEN}"

echo -e "\n\n=== Setup Complete ==="
