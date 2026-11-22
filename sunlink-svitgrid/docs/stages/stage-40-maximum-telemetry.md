# Stage 40 — Maximum Deye Telemetry Acquisition & Android Display

**Статус: PASS (программная реализация, offline-тесты, APK). Production не изменялся.**

## 1. Результаты по критериям

| Показатель | До Stage 40 | После Stage 40 |
|---|---:|---:|
| Параметров в register map | 9 | 49 |
| Групп телеметрии | 6 (в модели) | 5 физических групп + energy |
| Разрешено для production polling | 1 (184) | 1 (184) |
| Физически проверено | 1 (184) | 1 (184) |
| Полей в `/api/telemetry/full` | ~40 | 68 |
| Android-тестов | 135 | 139 |
| Backend-тестов | 330 | 345 |

## 2. Реализовано

- `domain/extended_register_map.py`: 49 регистров с group, типом, count, FC, scale, unit, word_order, source, confidence, `physically_verified`, `production_allowed`.
- `domain/extended_decoder.py`: декодирование U/S WORD и U/S DWORD, candidate scale, ошибки вместо нулей.
- `build_read_plan()`: приоритетные группы, безопасное разбиение на блоки ≤8; по умолчанию план — только `[184]`. Явный список адресов не обходит `production_allowed`.
- `domain/telemetry.py`: категории расширены до 68 полей (battery 10, pv 13, grid 15, load 8, inverter 15, energy 7).
- Android: `InverterDetailScreen` + маршрут `inverter-status`; Battery/PV/Grid/Load экраны показывают новые поля; TelemetryScreen динамически показывает все категории.
- Локализация uk/en дополнена; паритет проверен тестом.

## 3. Что доступно и что нет
Доступно физически: только `battery_soc` (184). Остальные 48 регистров описаны как MODEL_COMPATIBLE/PHYSICALLY_VERIFIED (по историческим FC03, без HMI), но **не разрешены** для production. Все новые поля в API/UI остаются `null` до отдельного согласования.

## 4. Direct vs calculated
`battery_soc` — direct measurement `deye_modbus`. `pv.total_power` — calculated из присутствующих PV-мощностей, явно маркируется `confidence=calculated`. Энергия из мощности не рассчитывается.

## 5. Тесты
| Проверка | Статус |
|---|---|
| Backend `unittest discover` | **345 PASS** (15 новых Stage 40) |
| Android `testDebugUnitTest` | **139 PASS** (4 новых) |
| Android lint | PASS |
| `assembleDebug` | PASS |
| `assembleDebugAndroidTest` | PASS (компиляция) |
| `connectedDebugAndroidTest` | NOT VERIFIED (нет устройства) |

## 6. APK
Package `com.sunlink.svitgrid`, versionName **2.40.0**, versionCode **61**, debug, без production signing.

## 7. Production safety
Active map осталась `{184}`; poller, endpoint, `.env`, systemd, SQLite, TLS/DNS/NAT/firewall, Ebyte, инвертор — без изменений. Modbus read/write физически — NOT RUN. Deployment — NOT RUN.

## 8. Дальше
После отдельного разрешения — физическая проверка приоритетных регистров (183/191/190, затем 186–188, 109–114, 150/151/169/192), затем точечное включение в `production_allowed` с сохранением stale/confidence.
