# Stage 46 — Real History & Graphs

Статус: PASS (код/тесты/APK); production deployment не выполнялся.

## Реализовано
- Backend `services/history_service.py`: реальные временные ряды из SQLite с агрегацией 24h=5min, 7d=1h, 30d=6h; пропуски — `value=null, gap=true`; до 1000 точек; валидация параметра/периода.
- Backend API `GET /api/history/series?parameter=...&period=24h|7d|30d[&end=...]`.
- Repository `prune_before()` для контроля роста БД (по умолчанию не активируется).
- Android: `HistorySeriesDto`, ApiService/Repository/ViewModel `loadSeries()`; HistoryScreen переписан под реальные series, периоды 24h/7d/30d, 15 параметров, график с разрывами, Slider выбора точки с timestamp и значением, состояния loading/error/empty.
- uk/en строки `history_source`, `history_points`.

## Тесты
Backend 375 PASS (8 новых), Android 160 PASS (4 новых), lint/build PASS. Compose UI на устройстве NOT RUN.

## Production deployment plan (НЕ выполнялся)
1. Backup `/opt/sunlink/backend-stage25b1` + `/etc/sunlink/sunlink-https.env` + SQLite.
2. Скопировать `services/history_service.py`, `deye_gateway.py`, `storage/repository.py`, `storage/sqlite_repository.py`.
3. Миграция схемы НЕ требуется (индексы уже есть в v1); `prune_before` не вызывается автоматически.
4. Перезапустить только `sunlink-https.service` после отдельного разрешения.
5. Проверить `/api/history/series` для 24h/7d/30d и SOC; проверить WS и отсутствие второго master.
6. Rollback из backup при ошибке.

APK: 2.46.0 (67), debug/prerelease.
