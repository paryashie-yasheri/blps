curl -v -c cookies.txt -X POST http://localhost:1234/login \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=john" \
  -d "password=secret123"