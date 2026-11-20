#!/usr/bin/env bash
# SunLink — проверка listener/портов (Stage 25B). Read-only.
# НЕ открывает порты и НЕ меняет firewall/NAT.
set -euo pipefail

echo "=== SunLink port check ==="
echo "-- Listening sockets --"
ss -ltnp 2>/dev/null | grep -E ':8443|:8800|:502' || echo "(no matching listeners)"

echo "-- Local endpoint --"
curl -sS -m 5 -o /dev/null -w 'https://127.0.0.1:8443/status -> %{http_code}\n' \
  --resolve sunlink.pp.ua:8443:127.0.0.1 https://sunlink.pp.ua:8443/status || true

echo "-- Expected state --"
cat <<'EOF'
8443 : TLS listener on SunLink backend, NAT 91.226.253.235:8443 -> 192.168.25.20:8443
8800 : must NOT be exposed to WAN; local-only if still used
502  : Modbus/Ebyte — must NOT be exposed to WAN
EOF
