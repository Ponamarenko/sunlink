# Stage 24 — Real Telemetry Expansion: Backend + API + Android

**Статус: PASS**
**HEAD до:** `d47ea89dc52ed030f0ddd70f90bd8e2291a5f3ae`.
**Физического Modbus в Stage 24: 0 запросов. Production не затронут.**

## 1. Архитектура

```
RegisterMetadata (PHYSICALLY_READ / PROVISIONAL + candidate_scale/offset)
    -> register_decoder.decode_register()  (raw/decoded/normalized/unit/scale/offset/confidence/status)
        -> TelemetrySnapshot (value_of accessors, provisional_parameters)
            -> SQLite MeasurementRepository (без миграции — схема уже вмещает все поля)
                -> /api/live/raw + /api/measurements/*  (+ provisional_parameters)
                    -> Android LiveRawDto -> InverterData -> ViewModel -> Dashboard "РЕАЛЬНА ТЕЛЕМЕТРІЯ"
```

Активный физический polling **не расширялся**: `ACTIVE_POLL_REGISTERS = {184}`.
Добавлен отдельный `CANDIDATE_REGISTERS = {183,186,187,188,190,191,90,91}` — только для декодирования и mock-тестов. Default profile не запускает физический опрос.

## 2. Новые регистры и статусы (из Stage 19–23)

| Регистр | Параметр | Преобразование | Статус | Физ. чтение |
|---:|---|---|---|---|
| 184 | battery_soc | U_WORD ×1 % | PHYSICALLY_READ | 99 % (Stage 19), 95 % ×3 (Stage 21) |
| 186/187/188 | pv1/2/3_power | U_WORD ×1 Вт | PHYSICALLY_READ | 1519/796/123 Вт (Stage 23) |
| 183 | battery_voltage | U_WORD ×0.01 В | PHYSICALLY_READ | 53.53 В |
| 191 | battery_current | S_WORD ×0.01 А | PHYSICALLY_READ | **−3.51 А** |
| 190 | battery_power | S_WORD ×1 Вт; candidate ×0.1 | PROVISIONAL | raw 0xF8C3 = −1853; ×0.1 → −185.3 Вт |
| 90/91 | dc/ac_temperature | S_WORD ×0.1 −100 °C | PROVISIONAL | raw 1410/1482 → 41.0/48.2 °C |

PHYSICALLY_READ ≠ VERIFIED: синхронной сверки с дисплеем инвертора документально нет.
Отрицательные значения сохранены (`0xFEA1 → −351`, `0xF8C3 → −1853`).

## 3. Backend изменения

- `domain/register_metadata.py`: поля `physical_status`, `candidate_scale`, `candidate_offset`; `CANDIDATE_REGISTERS`; updated provisional reasons со ссылкой на Stage 23.
- `domain/register_decoder.py` (новый): единая функция `decode_register(raw, meta, use_candidate)` возвращает `DecodedRegister` со всеми полями (raw, decoded, normalized, unit, scale, offset, data_type, confidence, physical_status, quality, error). Диапазоны: SOC 0–100, voltage 0–100, current ±500, power ±15000, PV 0–20000, temp −50..200. Некорректное значение → `normalized_value=None` и quality=error, никогда 0.0.
- `domain/telemetry.py`: accessors `value_of`, `pv1_power`, `pv2_power`, `pv3_power`, `battery_voltage`, `battery_current`, `battery_power`, `temperature_90/91`, `provisional_parameters`; поле `stale`.
- `services/telemetry_sources.py`: DEMO-сценарии `charging`, `discharging`, `no_pv`, `partial` + `demo_source_for()`; все с меткой `demo_simulator`.
- `deye_gateway.py`: плоский ответ получил `provisional_parameters` для UI-маркировки.
- SQLite: схемы не менялись — модель `Measurement` уже хранит register_address, raw/normalized, unit, timestamp, quality, confidence, source, description. Проверена совместимость со старой БД и повторное открытие (schema_version=1, данные сохраняются).

## 4. API

Пример `/api/live/raw` (DEMO discharge, симуляция):
```json
{"pv1_power":1519.0,"pv2_power":796.0,"pv3_power":123.0,"p_sun":2438.0,
 "battery_voltage":53.53,"battery_current":-3.51,"battery_power":-1853.0,
 "batt_soc":95.0,"dc_temperature":41.0,"ac_temperature":48.2,
 "provisional_parameters":["ac_temperature","battery_power","dc_temperature"],
 "source":"demo_simulator","connection_status":true}
```
Отсутствующие поля = `null`. `/api/measurements/latest`, `/history`, `/api/inverters`, `/status` сохранили контракт; новые параметры доступны по именам `pv1_power`, `battery_current` и т. д. Проверены пустая история, отсутствующий параметр (404/empty), offline/stale, demo/mock/deye_modbus.

## 5. Android

- `LiveRawDto`: новое поле `provisional_parameters`.
- `InverterData`: `provisionalParameters: Set<String>` + `toMap()`.
- `DataRepository`: переносит provisional-метки в доменную модель.
- `DashboardScreen`: новая карточка **«РЕАЛЬНА ТЕЛЕМЕТРІЯ»** — PV1/2/3 и ΣPV (только если все три известны), SOC, Vbat, Ibat, Pbat, T90/T91; отрицательные значения со знаком, `null` → `—`, provisional отмечены `*` с пояснением. DEMO-баннер и stale/offline сохранены.
- Новые фикстуры: `live_raw_discharge.json`, `live_raw_partial.json` (сгенерированы backend DEMO-сценариями).

## 6. Тесты

Backend: **246 PASS** (`python3 -m unittest discover -s tests`) — 20 Stage 24 тестов (декодер U/S_WORD, candidate scale/offset, отрицательные значения, null, диапазоны, SQLite новых параметров и старой БД, API provisional/negative/null, absence write endpoints).
Android: **67 PASS** (`./gradlew :app:testDebug`) — DTO parsing новых фикстур, Repository mapping, ViewModel с отрицательными значениями, все существующие тесты сохранены.

## 7. APK и Release

- versionCode `51`, versionName `2.31.0` (было 50 / 2.30.0).
- APK: `app/build/outputs/apk/debug/app-debug.apk`, 12 869 688 bytes.
- SHA-256: `253620944aa7ad2a32a6886901bd4f038fa839c51d976222fa444ff431f886b9`.
- Prerelease `android-v2.31.0-stage24`: https://github.com/Ponamarenko/sunlink/releases/tag/android-v2.31.0-stage24
- APK asset: https://github.com/Ponamarenko/sunlink/releases/download/android-v2.31.0-stage24/app-debug.apk
- Checksum: https://github.com/Ponamarenko/sunlink/releases/download/android-v2.31.0-stage24/app-debug.apk.sha256
- Скачанный asset проверен: размер и SHA-256 совпали. APK в Git не добавлялся.

## 8. Ограничения и что осталось для Stage 25

- Native modbus source по-прежнему активен только для регистра 184; новые регистры в физический опрос **не включены**.
- Масштаб 190 (×0.1) и offset 90/91 остаются PROVISIONAL — нужны синхронные показания дисплея и отдельное разрешение на уточняющие FC03.
- Тип PV power (U vs S_WORD) не разрешён для отрицательных значений.
- Grid power/load, энергетические счётчики и battery temperature остаются с прежними статусами.
- Physical polling, HA, Redis, systemd, firewall, MikroTik, Ebyte и конфигурация инвертора не изменялись; APK — debug, не production-signed.
