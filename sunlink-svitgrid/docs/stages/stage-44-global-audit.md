# Stage 44 — Global Audit, Real Implementation & Production Readiness

**Статус: PASS** (аудит, исправления, тесты, APK). Production Stage 43 не повреждён; deployment/restart в этом этапе не выполнялись.

## A. Исходное состояние
Initial commit `87957548f52429651923ff17c34af0ded8321403`, `main = origin/main`, рабочее дерево — только untracked мастер-ТЗ. Резервная копия: `/tmp/sunlink-stage43-baseline.bundle` (311 MB). Production read-only: `sunlink-https.service` active, 1 gateway, 2 listener, `SUNLINK_EXTENDED_POLLING=1`; `svitgrid.service` active.

## B. Глобальный аудит
| Подсистема | Проверено | Найдено | Исправлено | Статус |
|---|---|---|---|---|
| Modbus transport | Один poller, FC03, allowlist, dry-run | `POST /read` создавал второй pymodbus client | Endpoint отдаёт 403, second master невозможен | PASS |
| Register decoder | WORD/DWORD, scale, word order | Нет новых ошибок | — | PASS |
| Telemetry mapping | `/api/live/raw` → `/api/telemetry/full` | 7 реальных полей терялись из-за рассогласования имён (`total_*_low`, `daily_grid_*`) | Маппинг исправлен, добавлены тесты | PASS |
| Android DTO/Repository | Плоский REST → InverterData | 10 реальных полей не парсились (grid_frequency, energy counters) | DTO/модель/repository/Dashboard расширены | PASS |
| History/Statistics | Сортировка, dedupe, periods | Регрессий нет | — | PASS |
| Quality/provenance | 6 статусов | Ранее возможна путаница calculated/direct | Сохранено, тесты добавлены | PASS |
| REST/WS | /status, /live/raw, /telemetry/full, /ws | Согласованы | — | PASS |
| Security | CORS, secrets, /read | `/read` unsafe | Отключён | PARTIAL (CORS `*` оставлен как legacy; без production-изменений) |
| Localization | uk/en паритет | Паритет сохранён | Добавлен `energy_summary` | PASS |

## C. Реальное внедрение
- Backend: `domain/telemetry.py` — правильные имена параметров, реальные значения теперь попадают в battery.total_charge/discharge, pv.total_energy, grid.import/export, energy totals.
- Backend: `deye_gateway.py` — `POST /read` возвращает 403 и не создаёт Modbus-клиент.
- Android: `LiveRawDto`, `InverterData`, `DataRepository` — 10 новых полей; Dashboard показывает «ЕНЕРГІЯ ЗА ДЕНЬ» (PV, заряд, разряд, импорт, экспорт).
- Локализация: uk/en ключ `energy_summary`.

## D. Телеметрия
Production (read-only, после Stage 43): SOC ~97 %, батарея 53.3 В / ~1.6 А, PV1 307 В / 18.6 А / 5619 Вт, PV2 325 В / 2.9 А / 945 Вт, PV3 280 В / 3 А / 816 Вт, сеть 233 В / ~50 Гц, нагрузка ~6.9 кВт, дневная генерация 9–10 кВт·ч. Status: PHYSICALLY_VERIFIED (184), MODEL_COMPATIBLE/DECODED (183, 186–188, 191, 109–114, 150, 192, 175, 70/71/76/77/72/74/96), CALCULATED (`pv.total_power`), UNAVAILABLE (inverter temps/faults, grid L2/total energy). Исключены: `grid_voltage_l2` (0.0), `total_grid_import_low` (32 млн kWh).

## E. Тесты
| Набор | Результат |
|---|---|
| Backend `unittest discover` | **367 PASS** (3 новых Stage 44) |
| Android `testDebugUnitTest` | **150 PASS** (3 новых) |
| lint / assembleDebug / assembleDebugAndroidTest | PASS |
| `connectedDebugAndroidTest` | NOT RUN (нет устройства) |
| Физические FC03 в Stage 44 | NOT RUN (production не менялся) |

## F/G. APK и Git
versionName **2.44.0**, versionCode **65**, debug/prerelease. Package `com.sunlink.svitgrid`. Git commit и Release — в финальном ответе.

## H. Production
Сервисы не перезапускались, `.env`, systemd, SQLite, active map, TLS/DNS/NAT/firewall, Ebyte и инвертор не изменялись. Здесь только read-only проверки и изменения в Git.

## I. Осталось
CORS `*` и отсутствие authentication — ограничение архитектуры, требует отдельного решения. Inverter temperatures/faults и grid total energy требуют физической проверки перед включением.
