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

echo "=== Test Redeem Workflow ==="

# Get reward info
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

# Get balance before
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

# Trigger the process
echo ""
echo "--- Triggering redeem ---"
OPERATION_ID=$(python3 -c "import uuid; print(uuid.uuid4())")

curl -s -X POST "$CAMUNDA_URL/message" \
  -H "Content-Type: application/json" \
  -d "{
  \"messageName\": \"msg_redeem\",
  \"businessKey\": \"test-$(date +%s)\",
  \"processVariables\": {
    \"userId\": {\"value\": \"${USER_ID}\", \"type\": \"String\"},
    \"rewardId\": {\"value\": \"${REWARD_ID}\", \"type\": \"String\"},
    \"operationId\": {\"value\": \"${OPERATION_ID}\", \"type\": \"String\"},
    \"occurredAt\": {\"value\": \"$(date -u +"%Y-%m-%dT%H:%M:%S+00:00")\", \"type\": \"String\"}
  },
  \"resultEnabled\": true
}" > /dev/null

echo "  Process triggered"

# Wait and check balance
sleep 2

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

# Verify
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
