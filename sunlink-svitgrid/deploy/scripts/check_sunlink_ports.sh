#!/usr/bin/env bash
# SunLink — проверка listener 8800/9443 (Stage 25B.1). Read-only.
# НЕ открывает порты и НЕ меняет firewall/NAT.
set -euo pipefail

echo "=== SunLink port check ==="
echo "-- Listening sockets --"
ss -ltnp 2>/dev/null | grep -E ':9443|:8800|:502' || echo "(no matching listeners)"

echo "-- Local HTTP 8800 --"
curl -sS -m 5 -o /dev/null -w 'http://127.0.0.1:8800/status -> %{http_code}\n' \
  http://127.0.0.1:8800/status || true

echo "-- Local HTTPS 9443 (через SNI, с проверкой имени) --"
curl -sS -m 5 -o /dev/null -w 'https://sunlink.pp.ua:9443/status -> %{http_code}\n' \
  --resolve sunlink.pp.ua:9443:127.0.0.1 https://sunlink.pp.ua:9443/status || true

echo "-- Expected state --"
cat <<'EOF'
8800 : HTTP listener, ТОЛЬКО LAN/локальная диагностика; НЕ должен быть в WAN
9443 : HTTPS listener (TLS в Uvicorn), NAT 91.226.253.235:9443 -> 192.168.25.20:9443
502  : Modbus/Ebyte — НЕ должен быть доступен из Интернета
8443 : больше не используется SunLink
EOF
