#!/usr/bin/env bash
set -euo pipefail

CAMUNDA_URL="${CAMUNDA_URL:-http://localhost:1234/engine-rest}"
USER_ID="${2:-c8061358-6798-4c0c-b60d-234ed17e09e0}"
REWARD_ID="${1:-}"

if [ -z "$REWARD_ID" ]; then
  REWARD_ID=$(python3 -c "
import psycopg2, sys
conn = psycopg2.connect(host='localhost', port=5434, dbname='studs_rewards', user='postgres', password='postgres')
cur = conn.cursor()
cur.execute('SELECT reward_id, name, points_cost, quantity FROM public.rewards WHERE quantity IS NULL ORDER BY name LIMIT 1')
r = cur.fetchone()
if not r:
    print('No unlimited reward found', file=sys.stderr)
    sys.exit(1)
print(r[0])
conn.close()
")
fi

echo "=== Test Redeem v2 (Process_1hxi6lx) ==="

echo ""
echo "--- Reward ---"
python3 -c "
import psycopg2, sys
conn = psycopg2.connect(host='localhost', port=5434, dbname='studs_rewards', user='postgres', password='postgres')
cur = conn.cursor()
cur.execute('SELECT reward_id, name, points_cost, quantity FROM public.rewards WHERE reward_id = %s', ['$REWARD_ID'])
r = cur.fetchone()
if not r:
    print('ERROR: Reward not found')
    sys.exit(1)
print(f'  ID:   {r[0]}')
print(f'  Name: {r[1]}')
print(f'  Cost: {r[2]} баллов')
print(f'  Qty:  {\"∞\" if r[3] is None else r[3]}')
conn.close()
"

POINTS_COST=$(python3 -c "
import psycopg2
conn = psycopg2.connect(host='localhost', port=5434, dbname='studs_rewards', user='postgres', password='postgres')
cur = conn.cursor()
cur.execute('SELECT points_cost FROM public.rewards WHERE reward_id = %s', ['$REWARD_ID'])
print(cur.fetchone()[0])
conn.close()
")

echo ""
echo "--- Balance before ---"
BALANCE_BEFORE=$(python3 -c "
import psycopg2, sys
conn = psycopg2.connect(host='localhost', port=5432, dbname='studs', user='postgres', password='postgres')
cur = conn.cursor()
cur.execute('SELECT current_balance, total_spent FROM public.user_balances WHERE user_id = %s', ['$USER_ID'])
r = cur.fetchone()
if not r:
    print('ERROR: User balance not found')
    sys.exit(1)
print(f'Balance: {r[0]}')
conn.close()
")
echo "  Balance: $(echo "$BALANCE_BEFORE" | grep "Balance:" | sed 's/.*: //')"

CURRENT_BALANCE=$(python3 -c "
import psycopg2
conn = psycopg2.connect(host='localhost', port=5432, dbname='studs', user='postgres', password='postgres')
cur = conn.cursor()
cur.execute('SELECT current_balance FROM public.user_balances WHERE user_id = %s', ['$USER_ID'])
print(cur.fetchone()[0])
conn.close()
")

EXPECTED_BALANCE=$(( CURRENT_BALANCE - POINTS_COST ))
echo "  Expected after: $EXPECTED_BALANCE"

if [ "$EXPECTED_BALANCE" -lt 0 ]; then
  echo "ERROR: Insufficient balance ($CURRENT_BALANCE) for cost $POINTS_COST"
  exit 1
fi

echo ""
echo "--- Triggering msg_redeem on Process_1hxi6lx ---"
OPERATION_ID=$(python3 -c "import uuid; print(uuid.uuid4())")
BUSINESS_KEY="test-v2-$(date +%s)"

curl -s -X POST "$CAMUNDA_URL/message" \
  -H "Content-Type: application/json" \
  -d "{
  \"messageName\": \"msg_redeem_v2\",
  \"processDefinitionKey\": \"Process_1hxi6lx\",
  \"businessKey\": \"${BUSINESS_KEY}\",
  \"processVariables\": {
    \"userId\": {\"value\": \"${USER_ID}\", \"type\": \"String\"},
    \"operationId\": {\"value\": \"${OPERATION_ID}\", \"type\": \"String\"},
    \"occurredAt\": {\"value\": \"$(date -u +"%Y-%m-%dT%H:%M:%S+00:00")\", \"type\": \"String\"}
  },
  \"resultEnabled\": true
}" > /dev/null

echo ""
echo "  ✅ Process started!"
echo "  📋 Open Tasklist: http://localhost:1234/camunda/app/tasklist"
echo "  👤 Login: demo / demo"
echo "  🎯 Complete the task 'Выбрать награду' with reward 'Bug Collector Badge'"
echo ""
echo "  Waiting for you to complete the task in Tasklist (timeout: 60s)..."

WAIT_SECONDS=0
while [ "$WAIT_SECONDS" -lt 60 ]; do
  sleep 3
  WAIT_SECONDS=$(( WAIT_SECONDS + 3 ))
  TASK_JSON=$(curl -s "$CAMUNDA_URL/task?processDefinitionKey=Process_1hxi6lx&active=true") || true
  USER_TASK_ID=$(echo "$TASK_JSON" | python3 -c "import sys,json; tasks=json.load(sys.stdin); print(tasks[0]['id'] if tasks else '')" 2>/dev/null || true)
  if [ -z "$USER_TASK_ID" ]; then
    echo ""
    echo "  Task completed! Checking balance..."
    break
  fi
  echo -n "."
done

if [ "$WAIT_SECONDS" -ge 60 ]; then
  echo ""
  echo "  ⏰ Timeout waiting for task completion!"
  echo "  You can still complete the task in Tasklist, then re-run to verify balance."
  exit 0
fi

echo ""
echo "--- Balance after ---"
BALANCE_AFTER=$(python3 -c "
import psycopg2
conn = psycopg2.connect(host='localhost', port=5432, dbname='studs', user='postgres', password='postgres')
cur = conn.cursor()
cur.execute('SELECT current_balance, total_spent FROM public.user_balances WHERE user_id = %s', ['$USER_ID'])
r = cur.fetchone()
print(f'Balance: {r[0]}')
print(f'Spent:   {r[1]}')
conn.close()
")
echo "  $BALANCE_AFTER" | sed 's/^/  /'

NEW_BALANCE=$(python3 -c "
import psycopg2
conn = psycopg2.connect(host='localhost', port=5432, dbname='studs', user='postgres', password='postgres')
cur = conn.cursor()
cur.execute('SELECT current_balance FROM public.user_balances WHERE user_id = %s', ['$USER_ID'])
print(cur.fetchone()[0])
conn.close()
")
NEW_SPENT=$(python3 -c "
import psycopg2
conn = psycopg2.connect(host='localhost', port=5432, dbname='studs', user='postgres', password='postgres')
cur = conn.cursor()
cur.execute('SELECT total_spent FROM public.user_balances WHERE user_id = %s', ['$USER_ID'])
print(cur.fetchone()[0])
conn.close()
")

echo ""
echo "=== Result ==="
DIFF=$(( CURRENT_BALANCE - NEW_BALANCE ))
echo "  Balance: $CURRENT_BALANCE -> $NEW_BALANCE"
echo "  Diff:    -$DIFF"
echo "  Expect:  -$POINTS_COST (reward cost)"

if [ "$DIFF" -eq "$POINTS_COST" ]; then
  echo "  VERDICT: PASS (balance decreased by exactly $POINTS_COST)"
  exit 0
elif [ "$DIFF" -gt 0 ]; then
  echo "  VERDICT: WARN (balance decreased by $DIFF, expected $POINTS_COST)"
  exit 1
else
  echo "  VERDICT: FAIL (balance did not decrease)"
  exit 2
fi
