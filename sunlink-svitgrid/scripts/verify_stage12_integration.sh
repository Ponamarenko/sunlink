#!/usr/bin/env bash
# SunLink этап 12 — воспроизводимая проверка интеграции Backend ↔ Android.
#
# Запускает ИЗОЛИРОВАННЫЙ DEMO backend на dev-порту 8801 с тестовой SQLite,
# выполняет реальные HTTP-запросы, проверяет offline/recovery и сохранение
# истории после перезапуска. НЕ трогает production, Redis, HA, Ebyte, инвертор.
set -euo pipefail

PORT="${PORT:-8801}"
DB="${DB:-/tmp/sunlink-stage12-demo.db}"
BASE="http://127.0.0.1:${PORT}"
SERVER_DIR="$(cd "$(dirname "$0")/../sunlink-svitgrid/server" && pwd)"
PID=""

cleanup() {
  if [[ -n "$PID" ]] && kill -0 "$PID" 2>/dev/null; then
    kill "$PID" 2>/dev/null || true
    wait "$PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

start_backend() {
  local extra="$1"
  ( cd "$SERVER_DIR" && exec python3 demo_backend.py --port "$PORT" --db "$DB" $extra ) \
    >/tmp/sunlink-stage12-verify.log 2>&1 &
  PID=$!
  for _ in $(seq 1 30); do
    if curl -sf "$BASE/" >/dev/null 2>&1; then return 0; fi
    sleep 0.5
  done
  echo "FAIL: backend did not start"; tail -20 /tmp/sunlink-stage12-verify.log; exit 1
}

stop_backend() {
  kill "$PID" 2>/dev/null || true
  wait "$PID" 2>/dev/null || true
  PID=""
}

check() {
  local name="$1"; shift
  if "$@" >/dev/null 2>&1; then echo "PASS: $name"; else echo "FAIL: $name"; exit 1; fi
}

echo "=== SunLink stage 12 integration verification ==="
echo "BASE=$BASE DB=$DB (DEMO source, isolated dev port)"

rm -f "$DB"
start_backend ""   # первичный старт с seed DEMO-истории

check "health /" curl -sf "$BASE/"
check "status /status" curl -sf "$BASE/status"
check "inverters /api/inverters" curl -sf "$BASE/api/inverters"
check "live /api/live/raw" curl -sf "$BASE/api/live/raw"
check "registers /registers" curl -sf "$BASE/registers"
check "latest /api/measurements/latest" curl -sf "$BASE/api/measurements/latest"
check "latest by param" curl -sf "$BASE/api/measurements/latest/battery_soc"
check "history /api/measurements/history" curl -sf "$BASE/api/measurements/history?parameter=battery_soc&start=2026-11-18T00:00:00Z&end=2026-11-19T00:00:00Z"
check "empty history" curl -sf "$BASE/api/measurements/history?parameter=missing&start=2026-11-18T00:00:00Z&end=2026-11-19T00:00:00Z"

# DEMO-маркировка должна присутствовать в ответе
curl -sf "$BASE/api/live/raw" | grep -q '"source":"demo_simulator"'
echo "PASS: DEMO source is marked in /api/live/raw"

# Отсутствующие параметры не подменяются нулями
curl -sf "$BASE/api/live/raw" | grep -q '"p_grid":null'
echo "PASS: missing p_grid is null (not 0.0)"

# Offline: останавливаем backend и убеждаемся, что соединение недоступно
stop_backend
if curl -sf --max-time 2 "$BASE/status" >/dev/null 2>&1; then
  echo "FAIL: backend still reachable after stop"; exit 1
fi
echo "PASS: offline detected (connection refused)"

# Recovery + история после перезапуска (без повторного seed)
start_backend "--no-seed"
check "recovery /status" curl -sf "$BASE/status"
COUNT=$(curl -sf "$BASE/api/measurements/history?parameter=battery_soc&start=2026-11-18T00:00:00Z&end=2026-11-19T00:00:00Z" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['count'])")
if [[ "$COUNT" -gt 0 ]]; then
  echo "PASS: history survived backend restart (count=$COUNT)"
else
  echo "FAIL: history lost after restart"; exit 1
fi

echo "=== ALL INTEGRATION CHECKS PASSED ==="
