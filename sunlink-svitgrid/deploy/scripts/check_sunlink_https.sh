#!/usr/bin/env bash
# SunLink — проверка прямого HTTPS на 9443 (Stage 25B.1). Read-only, без секретов.
# Запуск: ./check_sunlink_https.sh https://sunlink.pp.ua:9443
set -euo pipefail

URL="${1:-https://sunlink.pp.ua:9443}"

echo "=== SunLink HTTPS check: $URL ==="
echo "-- DNS (публичный resolver) --"
HOST=$(printf '%s' "$URL" | sed -E 's#^https?://##; s#:[0-9]+.*##; s#/.*##')
dig +short A "$HOST" @1.1.1.1 | head -3 || true

echo "-- TCP/TLS/HTTP --"
curl -sS -m 10 -o /dev/null \
  -w 'http_code=%{http_code} ssl_verify_result=%{ssl_verify_result} time=%{time_total}s\n' \
  "$URL/status"

echo "-- Response body --"
curl -sS -m 10 "$URL/status" | head -c 500
echo

echo "-- Certificate --"
timeout 10 openssl s_client -connect "${HOST}:9443" -servername "$HOST" </dev/null 2>/dev/null \
  | openssl x509 -noout -subject -issuer -dates -ext subjectAltName 2>/dev/null | head -12
