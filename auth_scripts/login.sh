#!/usr/bin/env bash

source ../.env

CLIENT_ID=blps-client

#
#JWT=$(curl -s -X POST http://localhost:8080/oauth2/token \
#  -H "Content-Type: application/x-www-form-urlencoded" \
#  -d "grant_type=client_credentials" \
#  -d "client_id=$CLIENT_ID" \
#  -d "client_secret=$AUTH_SERVER_CLIENT_SECRET" \
#  -d "scope=read write" | jq -r .access_token)

curl -v -s -X POST "${SPRING_HOST}"/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$AUTH_SERVER_CLIENT_SECRET" \
  -d "scope=read write"

#echo $JWT
#export $JWT
