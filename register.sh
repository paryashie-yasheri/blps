curl -i -X POST "http://localhost:1234/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username": "john", "password": "secret123", "email": "bigbadjohn@machine.com"}'