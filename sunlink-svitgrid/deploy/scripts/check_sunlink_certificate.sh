#!/usr/bin/env bash
# SunLink — проверка сертификата и прав доступа (Stage 25B). Read-only.
# Запуск: ./check_sunlink_certificate.sh /etc/letsencrypt/live/sunlink.pp.ua
set -euo pipefail

CERT_DIR="${1:-/etc/letsencrypt/live/sunlink.pp.ua}"
CERT="$CERT_DIR/fullchain.pem"
KEY="$CERT_DIR/privkey.pem"

echo "=== Certificate check: $CERT_DIR ==="
[[ -f "$CERT" ]] || { echo "MISSING: fullchain.pem"; exit 1; }
[[ -f "$KEY" ]] || { echo "MISSING: privkey.pem"; exit 1; }

echo "-- SAN / subject / dates --"
openssl x509 -in "$CERT" -noout -subject -issuer -dates -ext subjectAltName | head -12

echo "-- Key permissions (owner must be service user, mode 0600) --"
stat -c '%a %U:%G %n' "$KEY" "$CERT"

echo "-- Private key validity --"
openssl pkey -in "$KEY" -noout -check 2>&1 | head -2

echo "-- Cert/key match --"
CERT_HASH=$(openssl x509 -in "$CERT" -noout -pubkey | openssl sha256 | cut -d' ' -f2)
KEY_HASH=$(openssl pkey -in "$KEY" -noout -pubkey | openssl sha256 | cut -d' ' -f2)
if [[ "$CERT_HASH" == "$KEY_HASH" ]]; then echo "MATCH"; else echo "MISMATCH"; exit 1; fi

echo "-- Days until expiry --"
END=$(openssl x509 -in "$CERT" -noout -enddate | cut -d= -f2)
END_EPOCH=$(date -d "$END" +%s)
echo "$(( (END_EPOCH - $(date +%s)) / 86400 )) days"
