##!/bin/bash
#echo "🔐 Generating PKCE values..."
#CODE_VERIFIER=$(openssl rand -base64 32 | tr '+/' '-_' | tr -d '=')
#CODE_CHALLENGE=$(echo -n "$CODE_VERIFIER" | openssl dgst -sha256 -binary | openssl base64 | tr '+/' '-_' | tr -d '=')
#
#CLIENT_ID="blps_lab_curl"
#
##echo "📋 Open this URL in your browser:"
#curl "http://localhost:8080/oauth2/authorize
#  ?client_id=$CLIENT_ID
#  &redirect_uri=http://localhost:3000/callback
#  &response_type=code
#  &scope=read
#  &code_challenge=$CODE_CHALLENGE
#  &code_challenge_method=S256"
#
#
#echo "🔄 Exchanging code for JWT..."
#curl -s -X POST http://localhost:8080/oauth2/token \
#  -H "Content-Type: application/x-www-form-urlencoded" \
#  -d "grant_type=authorization_code" \
#  -d "code=$AUTH_CODE" \
#  -d "client_id=$CLIENT_ID" \
#  -d "redirect_uri=http://localhost:3000/callback" \
#  -d "code_verifier=$CODE_VERIFIER" | jq .
