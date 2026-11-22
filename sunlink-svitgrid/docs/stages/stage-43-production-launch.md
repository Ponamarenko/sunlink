# Stage 43 — Production Launch & Real Deye Telemetry

**Статус: PASS** — реальные данные получены с физического инвертора через существующий poller, переданы в REST/WS и доступны Android.

## A. Production
- Версия до: Stage 42, сервис `sunlink-https.service` active.
- Deployment: код Stage 42/43 скопирован в `/opt/sunlink/backend-stage25b1`, backup `/root/sunlink-backup/stage43-2026-11-22-101223` (код, env, unit, SQLite + checksum).
- Перезапуск: только `sunlink-https.service` (RESTART_AT `2026-11-22T08:13:12Z`, затем дважды после исправлений); `svitgrid.service` не трогался.
- Откат: не требовался.

## B. Modbus
- Один transport `ModbusTcpTransport` внутри `DeyePollingService`; `ExtendedJobQueue` выполняет FC03-задания в существующем цикле под `_cycle_lock`.
- Второго master/клиента/потока нет: `gateway_procs=1`, `modbus_connections=0` между циклами.
- Явный `production_allowed` list расширен до 23 адресов; реально прочитано 20+ параметров.
- FC06/FC16 и любые write — NOT RUN.
- Некорректные `total_grid_import_low` (32 млн kWh) и `grid_voltage_l2` (0.0) исключены из allowlist после обнаружения.

## C. Реальная телеметрия
| Параметр | Адрес | Raw | Decoded | Единица | Confidence |
|---|---:|---:|---:|---|---|
| pv.daily_energy | 108 | 90 | 9.0 | kWh | model_compatible |
| pv.pv1_current | 110 | 186 | 18.6 | A | model_compatible |
| pv.pv1_power | 186 | 5619 | 5619.0 | W | physically_verified |
| pv.pv1_voltage | 109 | 3071 | 307.1 | V | model_compatible |
| pv.pv2_current | 112 | 29 | 2.9000000000000004 | A | model_compatible |
| pv.pv2_power | 187 | 945 | 945.0 | W | physically_verified |
| pv.pv2_voltage | 111 | 3253 | 325.3 | V | model_compatible |
| pv.pv3_current | 114 | 30 | 3.0 | A | model_compatible |
| pv.pv3_power | 188 | 816 | 816.0 | W | physically_verified |
| pv.pv3_voltage | 113 | 2805 | 280.5 | V | model_compatible |
| pv.total_power | — | — | 7380.0 | W | calculated |
| battery.current | 191 | 165 | 1.6500000000000001 | A | model_compatible |
| battery.daily_charge_energy | 70 | 40 | 4.0 | kWh | model_compatible |
| battery.daily_discharge_energy | 71 | 29 | 2.9000000000000004 | kWh | model_compatible |
| battery.soc | 184 | 97 | 97.0 | % | corroborated |
| battery.voltage | 183 | 5327 | 53.27 | V | physically_verified |
| grid.frequency | 192 | 4996 | 49.96 | Hz | model_compatible |
| grid.voltage_l1 | 150 | 2336 | 233.60000000000002 | V | model_compatible |
| energy.daily_generation | 108 | 90 | 9.0 | kWh | model_compatible |

- PHYSICALLY_READ: 184 (SOC). DECODED: 183/191/109–114/186–188/108/70/71/150/192/175.
- CALCULATED: `pv.total_power` — помечен `confidence=calculated`.
- UNAVAILABLE: inverter temperature/faults/warnings, grid L2, total grid energy.
- Quality: known_parameters=21, available_parameters=21, extended polling enabled, jobs=26, errors=0.

## D. Backend
REST `/api/telemetry/full`, `/api/telemetry/quality`, `/api/live/raw`, `/status`, `/api/measurements/*`, `/api/stats/summary` работают; WebSocket `/ws` отправляет `live_data` с реальными значениями (source=`deye_modbus`).

## E. Android
Экраны Dashboard/Battery/PV/Grid/Load/Inverter/Energy Flow/Telemetry/All Parameters/Diagnostics получают значения через REST и WS. Новые поля маппятся без дублирования подписок. uk/en сохранены. Нет фиктивных значений.

## F. Тесты
Backend **364 PASS**, Android **147 PASS**, lint/build PASS, UI tests NOT RUN (нет устройства). Production smoke: service active, 1 gateway, HTTPS 200, WS live_data, SOC обновляется.

## G. APK
versionName **2.43.0**, versionCode **64**, debug/prerelease; SHA256 и ссылки — в финальном ответе.

## H. Git
Initial `06d9333...` (Stage 42); final — в финальном ответе.

## I. Осталось
Inverter temps/faults и grid totals требуют отдельной физической проверки; production deployment дополнительных изменений — только после согласования.
