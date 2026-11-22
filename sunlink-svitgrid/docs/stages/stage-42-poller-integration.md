# Stage 42 — Full Telemetry Integration Through Existing Modbus Poller

**Статус: PASS** — программная интеграция и тесты; production не_changed, расширенное чтение по умолчанию выключено.

## A. Git
Initial HEAD `41cd464b402950cfd858acaeaafc74e604a5c023` (= `origin/main`). Final HEAD — в финальном ответе. Working tree до начала — только untracked мастер-ТЗ.

## B. Poller architecture
- Изменён существующий `DeyePollingService` (единственный владелец `ReadOnlyModbusClient`/`ModbusTcpTransport`).
- Добавлен `services/extended_queue.py`: `ReadJob`, `ExtendedJobQueue`, round-robin, приоритеты, валидация FC03/production allowlist, декодирование.
- Очередь выполняется **внутри существующего `_run_cycle`**, под уже удерживаемым `_cycle_lock`, в том же потоке — второго poller'а, transport'а и соединения нет.
- `attach_extended_queue()` подключает очередь к уже созданному poller'у; `build_extended_poller()` теперь требует `existing_poller` и при `enabled=False` возвращает `None`.
- `deye_gateway.py` при физическом source читает `ExtendedPollingConfig.from_env()` и при явном включении вызывает `build_extended_poller(..., existing_poller=poller)`.
- Dry-run (`SUNLINK_EXTENDED_DRY_RUN=1`) не выполняет ICMP/Modbus запросы.

## C. Register coverage
| Группа | Адреса (описания) | Тип | Статус | Production eligible |
|---|---|---|---|---|
| Battery | 183/184/190/191/182/189/70/71/72–75 | U/S WORD, U_DWORD | 184 PHYSICALLY_VERIFIED; остальные MODEL_COMPATIBLE/PROVISIONAL | только 184 |
| PV | 109/110/111/112/113/114/186/187/188/108/96–97 | U_WORD/U_DWORD | MODEL_COMPATIBLE (186–188 исторический FC03) | только 184 |
| Grid | 150/151/164/165/169/167/168/170/171/192/76/77/78,80/81,82/194 | U/S WORD/U_DWORD | MODEL_COMPATIBLE/PROVISIONAL | только 184 |
| Load | 175/173/174 | S_WORD | MODEL_COMPATIBLE | только 184 |
| Inverter | 90/91/166/103/104 | S/U WORD | PROVISIONAL/UNKNOWN | только 184 |
| Energy | 96–97/108/70–75/76–82 | U_DWORD | MODEL_COMPATIBLE | только 184 |

VERIFIED: 184. DOCUMENTED/MODEL_COMPATIBLE: 48. PROVISIONAL: 190, 90/91, 169. UNAVAILABLE: поля без данных. Physically verified: 1.

## D. Backend
- `services/extended_polling.py`, `services/extended_queue.py`, `services/deye_polling_service.py`, `deye_gateway.py`.
- `/api/telemetry/full`, `/api/telemetry/quality`, `/api/stats/summary`, `/status`, `/api/live/raw`, `/api/measurements/*`, `/ws` — сохранены; quality теперь возвращает `extended_polling` (enabled/jobs/dry_run/executed).

## E. Android
- `TelemetryQualityDto` расширен `ExtendedPollingStatusDto`; Diagnostics показывает ON/OFF расширенного polling.
- Все 80 полей по-прежнему доступны через All Parameters; DTO/mapper без изменений. uk/en паритет сохранён.

## F. Тесты
| Проверка | Статус |
|---|---|
| Backend `unittest discover` | **363 PASS** (11 новых Stage 42) |
| Android `testDebugUnitTest` | **147 PASS** (3 новых) |
| lint / assembleDebug / assembleDebugAndroidTest | PASS |
| connectedDebugAndroidTest | NOT RUN (нет устройства) |
| Физические FC03 | NOT RUN (запрещено Stage 42) |

Проверено: один transport, последовательность чтений, round-robin, dry-run, timeout/errors, SOC сохраняется при ошибке дополнительных чтений, никаких FC06/FC16, произвольные адреса блокируются.

## G. APK
Package `com.sunlink.svitgrid`, versionName **2.42.0**, versionCode **63**, debug, без production signing.

## H/Remain
Production не изменялся; active map `{184}`, feature flag выключен, deployment NOT RUN. Для получения реальных дополнительных значений требуется отдельное разрешение на maintenance window + FC03-проверка и последующая активация `production_allowed`.
