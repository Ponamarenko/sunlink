# Этап 12 — SunLink: интеграция Backend и Android

**Статус:** выполнено. Backend и Android проверены как единая система на изолированном DEMO-окружении.
**Ветка:** `main` (origin/main). **HEAD на начало:** `cdfb34a`.
**Физические подключения:** не выполнялись (инвертор, Ebyte, HA, Redis, production URL не трогались).

## 1. Цель

Объединить `deye_gateway.py` и Android-приложение в работающую систему мониторинга и подтвердить:
совместимость API/DTO, получение инверторов/телеметрии/истории, корректную обработку
offline/error/stale, настройку адреса backend, рабочий APK.

## 2. Фактическое состояние на старте

- HEAD `cdfb34a` = `origin/main`, рабочая копия чистых исходников (незакоммиченные build-артефакты сохранены).
- Backend — FastAPI `deye_gateway.py`, 9 регистров этапа 7/10, read-only (FC 03).
- Android — Kotlin/Compose, Repository + Result + ViewModel/StateFlow, 32 теста.

## 3. Реальные API-контракты (проверены HTTP-запросами к DEMO backend)

| Endpoint | Статус | Что возвращает | Используется Android |
|---|---|---|---|
| `/` | 200 | `read_only: true`, version | health |
| `/status` | 200 | `connection_status`, `last_update`, `inverter_id`, `available_registers`, `registers`, `source` | проверка связи |
| `/api/inverters` | 200 | `inverters[]` (один реальный инвертор), `count`, `source` | список инверторов |
| `/api/live/raw` | 200 | плоские поля + `inverter_id`, `available_registers`, `source` | Dashboard |
| `/live` | 200 | `data`, `status`, плоские поля, `source` | fallback |
| `/registers` | 200 | 9 регистров с confidence/unit/scale | диагностика |
| `/api/measurements/latest` | 200 | `count=9`, `measurements[]` с provenance | диагностика |
| `/api/measurements/latest/{parameter}` | 200 / 404 | одно измерение / `detail` | — |
| `/api/measurements/history` | 200 | `count`, `measurements[]`, пустая история = `count:0` | History |
| `/ws` | WebSocket | `live_data` каждые 5 с | не используется (HTTP-опрос) |

### Формат timestamps
ISO-8601 UTC с суффиксом `+00:00` (например `2026-11-18T07:45:44.831317+00:00`).
Android парсит через `Instant.parse` после нормализации `+00:00` → `Z`.

### Единицы и достоверность
`battery_voltage` V, `battery_soc` %, `pv1/2/3_power` W, `battery_current` A,
`battery_power` W (**provisional** — знак не подтверждён), `dc/ac_temperature` °C (**provisional** — offset −100).
Каждое измерение несёт `quality`, `confidence`, `source`. DEMO-данные помечены `source: demo_simulator`.

## 4. Найденные и исправленные реальные ошибки интеграции

1. **Подмена отсутствующих данных нулями.** `_flat_live()` возвращал `0.0` для всех отсутствующих
   регистров, а `p_load` был заглушкой `battery_power` (меняло смысл показателя). Теперь отсутствующие
   поля — `null`, `p_sun` — сумма PV, `p_grid`/`p_load` — `null` (регистров нет в карте).
2. **Нет источника в плоском ответе.** Android всегда показывал `modbus_register`/`corroborated`.
   Добавлены поля `source`, `inverter_id`, `available_registers`; DEMO явно помечается, confidence
   для DEMO — `unverified`.
3. **Хардкод в Android.** `dtoToStatus()` подставлял `inverterId="deye_sg02lp1_am3"` и `availableRegisters=9`.
   Теперь берутся из ответа backend.
4. **Непотокобезопасная SQLite.** `sqlite3.connect(check_same_thread=True)` падал при обращении из
   другого потока (TestClient/uvicorn). Добавлены `check_same_thread=False` и `RLock` на операциях.
5. **Несовместимость тестов.** Класс `Result` отсутствовал, часть тестов не компилировалась (исправлено
   на предыдущем этапе); в этом этапе добавлены contract-тесты на реальные JSON.
6. **Отсутствие `/api/inverters`.** Добавлен честный endpoint с одним реальным инвертором.
7. **ViewModel терял данные при offline.** `refresh()` и `loadData()` вели себя по-разному; при ошибке
   данные заменялись пустыми. Теперь последний успешный ответ сохраняется и показывается как stale;
   добавлены флаги `stale` в `InverterUiState` и DEMO-баннер в Dashboard.

## 5. Изолированное DEMO-окружение

`sunlink-svitgrid/server/demo_backend.py` — отдельный dev-порт **8801**, отдельная тестовая SQLite
**`/tmp/sunlink-stage12-demo.db`**, источник `demo_simulator`. Использует тот же FastAPI `app` и Data Layer,
но DEMO-клиент вместо `ModbusTcpClient`. Не трогает production `.env`, systemd, Redis, HA, Ebyte, инвертор.

```bash
cd sunlink-svitgrid/server
python3 demo_backend.py --port 8801 --db /tmp/sunlink-stage12-demo.db
# без настройки адреса в приложении: Настройки -> 127.0.0.1 : 8801
```

Воспроизводимая проверка всей цепочки (HTTP, offline, recovery, история после рестарта):

```bash
./scripts/verify_stage12_integration.sh
```

Результат: **все 14 проверок PASS** (health, status, inverters, live, registers, latest, latest/param,
history, пустая история, DEMO-маркировка, `p_grid=null`, offline, recovery, история после рестарта).

Пример реального ответа `/api/live/raw` (DEMO):
```json
{"p_sun":449.0,"p_load":null,"p_batt":1915.0,"p_grid":null,"batt_soc":85.0,
 "battery_voltage":53.22,"battery_current":3.61,"dc_temperature":36.5,"ac_temperature":42.4,
 "pv1_power":211.0,"pv2_power":131.0,"pv3_power":107.0,
 "connection_status":true,"inverter_id":"deye_sg02lp1_am3","available_registers":9,
 "source":"demo_simulator","timestamp":"2026-11-18T07:45:44+00:00"}
```

## 6. Тесты

### Backend — 159 тестов
```bash
cd sunlink-svitgrid/server && python3 -m unittest discover -s tests
```
Из них 16 новых контрактных (`tests/test_stage12_contract.py`): HTTP-статусы, структура JSON,
названия полей, nullable, timestamp, quality/confidence/source, пустая история, 400/404,
все параметры History, сохранение истории после переоткрытия БД.

### Android — 49 тестов
```bash
./gradlew :app:testDebug
```
Новые:
- `DemoBackendContractTest` — парсит **реальные** JSON-ответы DEMO backend из
  `app/src/test/resources/demo/` (live_raw, status, inverters, latest, history, empty).
- `Stage12IntegrationTest` — полная цепочка MockWebServer → Retrofit → Repository → ViewModel → UI state,
  включая offline → сохранение последних данных → recovery, и запрос истории с правильным именем параметра.
- `SettingsRepositoryTest` — дефолтный адрес, нормализация и сохранение URL между "перезапусками".

## 7. APK

```bash
./gradlew :app:testDebug assembleDebug
```
- Файл: `app/build/outputs/apk/debug/app-debug.apk` (~13 МБ)
- applicationId: `com.sunlink.svitgrid`
- versionName: `2.30.0`, versionCode: `50`

## 8. Ограничения

- Эмулятор `emulator-5554` в статусе `offline`, бинарник `emulator` отсутствует — установка и запуск
  приложения на устройстве **не проверялись**. Проверены сборка APK и JVM-интеграционные тесты.
- WebSocket `/ws` реализован backend, но Android использует HTTP-опрос (WebSocket не подключён) —
  единый механизм обновления, без конкурирующих источников.
- `/api/measurements/*` не принимает `inverter_id` в query (gateway одноканальный, `INVERTER_ID`
  константа). "Ошибочный inverter_id" проверен через несуществующий параметр: `latest` → 404,
  `history` → `count:0`.
- p_grid/p_load остаются `null`: регистров сети/нагрузки в карте этапа 7 нет. UI показывает `—`.
- Forecast по-прежнему использует демонстрационные данные (реальный weather API не подключён).

## 9. Схема обмена

```
DEMO/Mock Registers -> DeyeGateway(read FC03) -> normalizer -> SQLite (source=demo_simulator)
        |                     |                       |
        |                     |                  /api/measurements/latest|history
        |                     |
        +---- _flat_live() -> /api/live/raw (null вместо отсутствующих)
                                   |
                          Android Retrofit (base URL из Settings)
                                   |
                    DataRepository -> Result -> MainViewModel/StateFlow
                                   |
                     Dashboard / History / Settings (DEMO-баннер, «—», stale/offline)
```
