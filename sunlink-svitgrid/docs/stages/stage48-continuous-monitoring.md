# Stage 48 — Continuous Monitoring & Full Telemetry

Статус: PASS (код/тесты/APK). Production deployment НЕ выполнялся.

## Реализовано
- `DeyePollingService.start()` вызывается в main один раз; повторный start — no-op; `_ACTIVE_POLLERS` блокирует второй poller.
- Фоновый цикл 30 с работает независимо от HTTP/WS клиентов; `read()` из REST/WS использует тот же `_cycle_lock`, параллельных FC03 нет.
- Статистика цикла: cycles/failed_cycles/last_cycle_ms/missed_cycles в `get_status()` и `/api/telemetry/quality`.
- `shutdown_event` останавливает poller до закрытия repository.
- `/api/telemetry/catalog` — 49 описаний, 23 production_allowed, 5 verified, без фактических значений.
- Android Diagnostics показывает polling ON/OFF, cycles, failed/missed cycles.

## Безопасность
Один transport, только FC03, интервал не изменён, write нет, `prune_before` не активирован.

## Тесты
Backend 389 PASS (6 новых), Android 168 PASS (3 новых), lint/build PASS. UI на устройстве NOT RUN.

## Production deployment plan (НЕ выполнялся)
1. Backup backend/env/SQLite через sqlite3 .backup.
2. Скопировать `deye_gateway.py`, `services/deye_polling_service.py`, `services/telemetry_catalog.py`.
3. Миграции не нужны; `.env`, systemd, БД не трогать.
4. Перезапустить только `sunlink-https.service` после отдельного разрешения.
5. Проверить `/status` (polling_running/cycles), `/api/telemetry/catalog`, историю 24h/7d/30d, WS, отсутствие второго master.
6. Rollback из backup при проблеме.

APK: 2.48.0 (69), debug/prerelease.
