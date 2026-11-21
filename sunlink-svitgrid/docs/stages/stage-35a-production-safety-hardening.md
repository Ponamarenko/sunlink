# Stage 35A — Production Safety Hardening

**Статус: PASS.** Четыре проблемы Stage 34 исправлены в исходниках; физических запросов и изменений production не было.

## 1. Исходные проблемы и причины

| # | Проблема Stage 34 | Причина |
|---:|---|---|
| 1 | Регистры 183, 186–188, 191 имели `physical_status="PHYSICALLY_READ"` | В одно поле смешаны «был FC03-ответ», «параметр идентифицирован» и «сверено с дисплеем». FC03-ответ не доказывает назначение |
| 2 | При `SUNLINK_DATA_SOURCE=demo` gateway без `telemetry_source` всё равно мог вызвать физический `ModbusTcpClient` | В `DeyeGateway.read_all_registers()` был физический fallback при отсутствии источника |
| 3 | `ReadOnlyModbusClient` не проверял `isError()`/`registers` | Пустой/error-ответ превращался в `IndexError` вместо понятной ошибки |
| 4 | `/status` показывал `available_registers=9`, хотя активен только 184 | Количество известных метаданных выдавалось за количество активных регистров |

## 2. Исправление №1 — раздельная семантика статусов

Новый `RegisterStatus` (`domain/register_metadata.py`) —простая шкала доказательности, совместимая со старыми строками:

- `unknown` — нет доказательств;
- `fc03_read` — был реальный FC03-ответ (новое имя legacy `PHYSICALLY_READ`);
- `corroborated` — несколько независимых источников;
- `provisional` — есть основания, доказательств мало;
- `physically_confirmed` — реальный источник и стабильная история (только 184);
- `display_verified` — сверено с дисплеем (пока ни один регистр).

Добавлены ортогональные флаги `RegisterMetadata.display_verified` и `production_allowed`, а также `normalize_physical_status()` для обратной совместимости со старыми метками. `ConfidenceLevel` остался без изменений: он описывает достоверность **назначения**, а не факт чтения.

Итог:

- 184 — `physically_confirmed`, `production_allowed=True`, `display_verified=False`;
- 183, 186, 187, 188, 191 — `fc03_read`, `production_allowed=False`;
- 190, 90, 91 — `provisional`, `production_allowed=False`;
- активная карта физического polling по-прежнему `{184}`.

Обновлены ожидания Stage 24 тестов: `PHYSICALLY_READ → fc03_read`, SOC `→ physically_confirmed` (утверждения не ослаблены, а уточнены).

## 3. Исправление №2 — DEMO/PRODUCTION isolation

- `DeyeGateway.__init__` получил параметр `physical_modbus_allowed=True` (обратная совместимость). При `False` метод `connect()` не создаёт `ModbusTcpClient`, а `read_all_registers()` сразу возвращает пустой словарь.
- В `__main__` при физическом source: `physical_modbus_allowed=True`, подключается `build_production_poller()`/`attach_to_gateway()` — один poller.
- В `__main__` при `demo`/`mock`: явно подключается `DemoTelemetrySource()` или `MockTelemetrySource(MockFc03Transport({}))`, `physical_modbus_allowed=False`, `source` берётся из telemetry-источника.
- Нет автоматического переключения demo↔production, нет fallback на физический источник при ошибке demo и нет fallback на simulator при ошибке physical.
- Ошибка конфигурации (`SUNLINK_DATA_SOURCE=bogus`, physical без `SUNLINK_ENABLE_MODBUS=1`) остаётся понятным исключением запуска.

Тесты используют `ForbiddenPhysicalClient`, который возбуждает `AssertionError` при любой попытке создать Modbus-клиент.

## 4. Исправление №3 — обработка ответов Modbus

`ReadOnlyModbusClient` теперь проверяет:

1. ответ не `None`;
2. `isError()` (если метод есть) — `Modbus exception` детализируется;
3. наличие `registers`;
4. корректный тип `list`/`tuple`;
5. непустой список;
6. точное соответствие ожидаемому `count`;
7. Each value — `int` в диапазоне 0…65535 (bool отвергается).

Любое нарушение → `ModbusReadError`. Публичный API (`read_register`, `read_holding_registers`, `read_with_details`) не изменён. Poller ловит ошибку, пишет диагностику и не создаёт значение/не портит остальные корректные measurements.

## 5. Исправление №4 — честные поля `/status`

Legacy-поля сохранены: `available_registers` и `registers` по-прежнему означают **известные метаданные** (9). Добавлены однозначные поля:

| Поле | Production (184) | DEMO |
|---|---:|---:|
| `known_registers_count` | 9 | 9 |
| `active_registers_count` | 1 | 0 |
| `active_register_addresses` | `[184]` | `[]` |
| `physically_confirmed_registers_count` | 1 | 1 |

`source` остаётся `deye_modbus` / `demo_simulator` / `mock`. Формат SOC в `/api/live/raw` и кадре `/ws` не менялся.

## 6. REST / WebSocket совместимость

Тестами подтверждено: `/status`, `/api/inverters`, `/api/live/raw`, `/api/measurements/latest` и `/ws` отвечают 200 и сохраняют format. Важно: тест REST использует временную SQLite и восстанавливает глобальный репозиторий — в предыдущем наборе нашёлся тест-изоляционный дефект: shutdown `TestClient` закрывает глобальный `_repository`, из-за чего следующий тест получал 500. Это не production-дефект, а особенность тестов; в Stage 35A тест устойчив к нему.

Android v2.35.0 не изменялся; DTO/парсер networked кадров совместимы (новые поля `/status` игнорируются старым клиентом безопасно).

## 7. Тесты

| Набор | Результат |
|---|---|
| Backend `python3 -m unittest discover -s tests` | **321 PASS** (299 + 22 Stage 35A) |
| Новый `tests/test_stage35a_safety_hardening.py` | **22 PASS** |
| DEMO isolation (5 тестов) | PASS |
| PRODUCTION isolation / explicit opt-in | PASS |
| Modbus error handling (8 тестов) | PASS |
| Register status transitions (4 теста) | PASS |
| `/status` и REST/WS compatibility (5 тестов) | PASS |
| Android unit tests | PASS (115, Stage 34; код не менялся) |
| Физические Modbus-запросы | **NOT RUN (запрещено)** |
| Modbus write | **NOT RUN (запрещено)** |

## 8. Ограничения и нерешённое

- `display_verified` остаётся `False` для всех регистров: синхронной сверки с дисплеем нет.
- Конфликт масштаба 190 (×1 vs ×0.1) и offset 90/91 не разрешён — это требует физического теста Stage 35B.
- `available_registers` всё ещё может вводить в заблуждение старых клиентов; новые поля рекомендуется использовать в будущем UI.
- Глобальный `_repository` закрывается в shutdown-событии `TestClient` — изоляция тестов улучшена только в новом тесте.
- Production не проверялся после изменений: деплой запрещён.

## 9. План Stage 35B

После отдельного разрешения владельца: первая физическая read-only сессия — FC03 по адресам `183`, `191`, `190` (по одному запросу на соединение, Unit ID 1, интервал ≥30 с), с синхронной фотографией дисплея батареи. Только после сверки обновить `physical_status`, `display_verified`, scale/offset и — отдельным решением — расширить `ACTIVE_POLL_REGISTERS`.

## 10. Production safety

Не изменялись: `.env`, systemd, SQLite, сертификаты, DNS/NAT/firewall/MikroTik, Ebyte, инвертор, endpoint, poller. Сервисы не перезапускались. Физических Modbus-запросов и Modbus write не выполнялось. Мастер-ТЗ `мобильное приложение.txt` не трогался.

## STAGE 35A — FINAL REPORT

1. **Статус:** **PASS**.
2. **HEAD:** initial `33afe83`; final — см. финальный ответ; branch `main`.
3. **Commit:** `stage35a: harden production source isolation and Modbus errors`.
4. **Branch/origin:** `main`, `HEAD = origin/main` после push.
5. **Working tree:** чистое, кроме untracked мастер-ТЗ.
6. **Изменённые файлы:** `domain/register_metadata.py`, `modbus/read_only_modbus_client.py`, `deye_gateway.py`, `tests/test_stage24_telemetry_expansion.py`, `tests/test_stage35a_safety_hardening.py`, этот отчёт, README/CHANGELOG (при наличии изменений).
7. **Четыре исправления:** см. §2–§5.
8. **DEMO/PROD isolation:** PASS (5+2 теста).
9. **Modbus error handling:** PASS (8 тестов).
10. **`/status`:** PASS (2 теста + REST compatibility).
11. **REST/WS:** PASS.
12. **Backend:** 321 PASS.
13. **Android:** 115 PASS (без изменений).
14. **Нерешённое:** §8.
15. **Production:** не изменялся.
16. **Физические запросы:** не выполнялись.
17. **Modbus write:** не выполнялся.
18. **Stage 35B:** см. §9.
