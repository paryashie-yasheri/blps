#!/usr/bin/env bash
set -e

source .env
BASE_URL="${SPRING_HOST}"
#USERNAME="testuser_$(date +%s)"
#PASSWORD="secret123"
USERNAME="huji_5"
PASSWORD="pass1234"

echo "=== User Workflow ==="
echo "Logging in via OpenEdx: $USERNAME"

LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/auth/openedx_user/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")

echo "Login response: $LOGIN_RESPONSE"

TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*' | sed 's/"accessToken":"//')
echo "Token: $TOKEN"

echo -e "\n\n=== Get Balance ==="
curl -s -X GET "${BASE_URL}/api/student/balance" \
  -H "Authorization: Bearer ${TOKEN}"

echo -e "\n\n=== Get Courses ==="
curl -s -X GET "${BASE_URL}/api/student/courses" \
  -H "Authorization: Bearer ${TOKEN}"

echo -e "\n\n=== Get Achievements ==="
curl -s -X GET "${BASE_URL}/api/student/achievements" \
  -H "Authorization: Bearer ${TOKEN}"

echo -e "\n\n=== Get Available Rewards ==="
curl -s -X GET "${BASE_URL}/api/student/rewards" \
  -H "Authorization: Bearer ${TOKEN}"

echo -e "\n\n=== Complete Lesson ==="
echo "(Skipping - need valid block key from course)"
echo "{}"

echo -e "\n\n=== Redeem Reward ==="
REWARD_ID=$(curl -s -X GET "${BASE_URL}/api/student/rewards" \
  -H "Authorization: Bearer ${TOKEN}" | grep -o '"rewardId":"[^"]*' | head -1 | sed 's/"rewardId":"//')
echo "Using reward: $REWARD_ID"
curl -s -X POST "${BASE_URL}/api/student/rewards/${REWARD_ID}/redeem" \
  -H "Authorization: Bearer ${TOKEN}"

echo -e "\n\n=== Get Balance After Lesson ==="
curl -s -X GET "${BASE_URL}/api/student/balance" \
  -H "Authorization: Bearer ${TOKEN}"

echo -e "\n\n=== Get Achievements After Lesson ==="
curl -s -X GET "${BASE_URL}/api/student/achievements" \
  -H "Authorization: Bearer ${TOKEN}"

echo -e "\n\n=== Get My Rewards ==="
curl -s -X GET "${BASE_URL}/api/student/rewards/my" \
  -H "Authorization: Bearer ${TOKEN}"

echo -e "\n\n=== Workflow Complete ==="
