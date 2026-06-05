#!/usr/bin/env bash
CAMUNDA_URL="${CAMUNDA_URL:-http://localhost:1234/engine-rest}"

list_rewards() {
  python3 -c "
import psycopg2
conn = psycopg2.connect(host='localhost', port=5434, dbname='studs_rewards', user='postgres', password='postgres')
cur = conn.cursor()
cur.execute(\"SELECT reward_id, name, points_cost, quantity FROM public.rewards ORDER BY name\")
print('Available rewards:')
for r in cur.fetchall():
    stock = '∞' if r[3] is None else str(r[3])
    print(f'  {r[0]}  {r[1]} ({r[2]} баллов, в наличии: {stock})')
conn.close()
"
}

[ "${1:-}" = "--list" ] && list_rewards && exit 0

USER_ID="${2:-c8061358-6798-4c0c-b60d-234ed17e09e0}"
REWARD_ID="${1:-$(python3 -c "
import psycopg2
conn = psycopg2.connect(host='localhost', port=5434, dbname='studs_rewards', user='postgres', password='postgres')
cur = conn.cursor()
cur.execute('SELECT reward_id FROM public.rewards WHERE quantity IS NULL ORDER BY name LIMIT 1')
print(cur.fetchone()[0])
conn.close()
")}"

OPERATION_ID=$(python3 -c "import uuid; print(uuid.uuid4())")

curl -s -X POST "$CAMUNDA_URL/message" \
  -H "Content-Type: application/json" \
  -d "{
  \"messageName\": \"msg_redeem\",
  \"businessKey\": \"redeem-${USER_ID}-${REWARD_ID}\",
  \"processVariables\": {
    \"userId\": {\"value\": \"${USER_ID}\", \"type\": \"String\"},
    \"rewardId\": {\"value\": \"${REWARD_ID}\", \"type\": \"String\"},
    \"operationId\": {\"value\": \"${OPERATION_ID}\", \"type\": \"String\"},
    \"occurredAt\": {\"value\": \"$(date -u +"%Y-%m-%dT%H:%M:%S+00:00")\", \"type\": \"String\"}
  },
  \"resultEnabled\": true
}" | jq .
