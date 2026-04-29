./login_as_user.sh "huji_5" "pass1234"

# 3) Донат (это должно записать событие DONATION в ledger_events)
TOKEN=$(cat .token.txt)
source ../.env
curl -sS -X POST "${SPRING_HOST}/api/user-balance/donate" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"amount": 25}' | jq .

# 4) Проверка журнала в pg-journal
cd ..
source .env
docker compose exec -T pg-journal psql \
  -U "${JOURNAL_POSTGRES_USER}" \
  -d "${JOURNAL_POSTGRES_DB}" \
  -c "select event_type, amount, balance_before, balance_after, occurred_at from ledger_events order by occurred_at desc limit 10;"
