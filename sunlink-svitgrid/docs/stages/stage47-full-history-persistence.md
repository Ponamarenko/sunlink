# Stage 47 — Full Real Telemetry History Persistence

Статус: PASS (код/тесты/APK). Production deployment НЕ выполнялся.

## Причина пустой истории
Extended-значения создавались с `confidence="model_compatible"/"physically_verified"`, которых нет в `ConfidenceLevel`. `save_measurements_batch()` получал `ValueError` на всей пачке, `_persist` ловил исключение — в SQLite попадал только SOC из базового цикла.

## Исправлено
- `TelemetryValue.verification` — отдельный provenance-статус; `confidence` приводится к корректному enum (`physically_verified→corroborated`, `model_compatible→provisional`).
- `_persist()`: пропускает None/NaN/Inf и errored quality; сохраняет все валидные параметры; добавляет CALCULATED `pv_total_power` только из трёх реальных PV-мощностей.
- `save_measurements_batch()`: дедупликация по (inverter_id, parameter_name, timestamp), возвращает число реально записанных строк.
- `history_service.KNOWN_PARAMETERS` + `pv_total_power`.
- Android History: `pv_total_power` в списке и локализация `hist_pv_total`.

## Тесты
Backend 383 PASS (8 новых), Android 165 PASS (5 новых), lint/build PASS. UI на устройстве NOT RUN.

## Production deployment plan (НЕ выполнялся)
1. Backup backend + env + SQLite (sqlite3 .backup).
2. Скопировать `domain/telemetry.py`, `services/deye_polling_service.py`, `storage/sqlite_repository.py`, `services/history_service.py`.
3. Миграции НЕ нужны; `prune_before` не активировать.
4. Перезапустить только `sunlink-https.service` после отдельного разрешения.
5. Проверить записи по категориям через `/api/history/series`, WS и SOC.
6. Rollback из backup при проблеме.

APK: 2.47.0 (68), debug/prerelease.
