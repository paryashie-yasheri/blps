#!/usr/bin/env bash

source ../.env

curl -v -b cookies.txt -X GET "${SPRING_HOST}/api/auth/me"
