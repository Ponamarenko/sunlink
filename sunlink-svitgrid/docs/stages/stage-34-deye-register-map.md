# Stage 34 — Deye Register Map, достоверность и план безопасного расширения телеметрии

**Статус: PASS (аудит, документация, offline-тесты). Физических запросов: 0. Production не изменялся.**

## 1. Исходное состояние (PHASE A)

- Репозиторий: `Ponamarenko/sunlink`, ветка `main`.
- Initial HEAD: `43817c4bdac044ac57dd80a5325387542398278f` (= `origin/main`).
- Рабочее дерево: чистое, кроме untracked пользовательского файла `мобильное приложение.txt` (мастер-ТЗ). Файл не изменялся и в коммит не включался.
- Отчёт Stage 33 прочитан: `docs/stages/stage-33-production-deployment.md`.
- Исходный коммит не перезаписывался; незакоммиченных пользовательских изменений в исходниках нет.

## 2. Изучённые файлы (PHASE B/C)

**Backend/код:** `deye_gateway.py`, `data_source_config.py`, `services/production_pipeline.py`, `services/deye_polling_service.py`, `services/telemetry_sources.py`, `services/modbus_polling_service.py`, `modbus/modbus_tcp_transport.py`, `modbus/read_only_modbus_client.py`, `modbus/mock_fc03_transport.py`, `domain/register_metadata.py`, `domain/register_decoder.py`, `domain/normalizer.py`, `domain/telemetry.py`, `domain/models.py`, `storage/sqlite_repository.py`, `tls_config.py`, `listener_config.py`.

**Документы проекта:** `docs/modbus-register-map.md`, `DEYE_SG02LP1_MODBUS_VERIFICATION.md`, `DEYE_READONLY_ADAPTER_STAGE2.md`, `docs/research/third-party-deye-mqtt-mcp-projects.md`, `docs/stages/stage-3-first-real-read.md`, `stage-4-modbus-display-verification.md`, `stage-6-ha-register-cross-check.md`, `stage-9.2-official-protocol-documentation.md`, `stage-9.3.1-evidence-reconciliation.md`, `stage-20…stage-24`, `stage-28`, `stage-30…stage-33`.

**Android (PHASE G, без изменений):** `WebSocketDto.kt`, `TelemetryWebSocketClient.kt`, `DataRepository.kt`, `MainViewModel.kt`, `DashboardScreen.kt`, design system, `SettingsRepository.kt`. Endpoint'ы `https://sunlink.pp.ua:9443/` / `wss://sunlink.pp.ua:9443/ws` не менялись.

## 3. Фактическая архитектура production после Stage 33

```
Deye SUN-12K-SG02LP1-EU-AM3 → RS485 → Ebyte NA111-E 192.168.25.15:502
→ ModbusTcpTransport (FC03, 1 запрос = 1 соединение, timeout 3 с)
→ ReadOnlyModbusClient → DeyePollingService (единый, source=deye_modbus)
→ ACTIVE_POLL_REGISTERS = {184} → normalize_register → TelemetrySnapshot
→ SQLite storage/measurements.db (аддитивные миграции)
→ REST /api/live/raw, /status, /api/measurements/*
→ WebSocket /ws (каждые 5 с)
→ Android v2.35.0 (REST + WS, null → «Немає даних»)
```

`deye_gateway.py` в `__main__` читает `DataSourceConfig.from_env()`, при физическом источнике вызывает `build_production_poller()` и `attach_to_gateway()` — ровно один poller. `PollerAlreadyRunning` защищает от второго. `TelemetrySource` не содержит write-методов; `ReadOnlyModbusClient` тоже. Slot `source` в `/status` берётся из poller'а (`deye_modbus`), а не из константы. Stale: при ошибке `DeyePollingService` возвращает последний удачный snapshot с `stale=True` и `connection_ok=False`, не выдавая старое значение за свежее. DEMO/mock в production pipeline не импортируются (`SIMULATION_MODULES`).

## 4. Источники документации (PHASE C)

| # | Источник | URL / lokal | Что подтверждает | Ограничение |
|---:|---|---|---|---|
| 1 | Официальная страница Deye SUN-7.6/8/10/12K-SG02LP1-EU-AM2/AM3 | https://www.deyeinverter.com/product/hybrid-inverter-1/sun7-6-8-10-12ksg02lp1euam2-am3-7-612kw-single-phase-2-mppt-hybrid-inverter-lv-battery-supported.html | Проверено 2026-11-21: модель существует, 12 кВт AM3, 2/3 MPPT, RS485, LCD | **Register map отсутствует** |
| 2 | Официальный User Manual (по данным Stage 9.2) | https://hqcdn.hqsmartcloud.com/deyeinverter/2026/05/19/BManualSUN-5-12K-SG02LP1-EU-AM320260519en.pdf | Подтверждена модель/интерфейсы; в мануале нет register map, FC03 и адресов | Источник: локальный отчёт Stage 9.2, PDF повторно не скачивался |
| 3 | kbialek/deye-inverter-mqtt, `docs/metric_group_deye_sg02lp1.md` | https://raw.githubusercontent.com/kbialek/deye-inverter-mqtt/main/docs/metric_group_deye_sg02lp1.md | Проверено 2026-11-21: адреса 186–188, 109–114, 108, 96–97, 169, 150/151, 167–171, 175, 192, 90/91, типы и масштабы | Сторонняя реализация (Apache-2.0), не официальная карта |
| 4 | kbialek, `docs/metric_group_deye_sg02lp1_battery.md` | https://raw.githubusercontent.com/kbialek/deye-inverter-mqtt/main/docs/metric_group_deye_sg02lp1_battery.md | Проверено 2026-11-21: 183 ×0.01 В, 184 ×1 %, 190 S_WORD ×1 Вт, 191 ×0.01 А, 182 ×0.1 °C, 72–75, 70/71, 189 | То же ограничение |
| 5 | MarkLabs, Deye Modbus Complete Register List | https://marklabs.pl/en/deye-modbus-complete-register-list/ | Проверено 2026-11-21: отдельная таблица SG02LP1-EU-AM2/AM3, 169 ×10, 190 ×1, 90/91 ×0.1, DWORD LW,HW | Уровень A/B, не официальный документ |
| 6 | Локальная карта проекта | `docs/modbus-register-map.md` | Сводка уровней B, DWORD LW,HW, исключённые адреса | Сама ссылается на #3/#5 |
| 7 | История физических тестов | `stage-19/21/23`, `stage-28`, `stage-33` | Реальные FC03-ответы: 184, 183, 186–188, 190, 191, 90/91 | Синхронной сверки с дисплеем нет |
| 8 | Stage 3.5 research | `docs/research/third-party-deye-mqtt-mcp-projects.md` | Сравнение карт, лицензии, предупреждения о write-функциях | Часть выводов устарела относительно текущей версии #3/#4 |

**Официальной карты регистров для SUN-12K-SG02LP1-EU-AM3 не найдено.** Ни один параметр не имеет статуса `OFFICIALLY_DOCUMENTED`.

## 5. Единственный физически подтверждённый регистр

**184 — Battery SOC, U_WORD ×1, %, `source=deye_modbus`, `PHYSICALLY_CONFIRMED` (конвенция проекта).**

История реальных FC03-ответов с нашего инвертора: Stage 19 `0x0063` = 99 %, Stage 21 `0x005F` = 95 % (3 чтения), Stage 28 `0x005E` = 94 % (3 чтения), Stage 33 production 76–77 %. Все ответы стабильны и физически правдоподобны, Unit ID 1, FC03, qty 1. **Синхронной сверки с дисплеем не было** — статус означает «реально прочитано с оборудования», не «сверено с HMI».

## 6. Полная таблица достоверности

Статусы: `PHYSICALLY_CONFIRMED` | `OFFICIALLY_DOCUMENTED` | `CORROBORATED` | `PROVISIONAL` | `UNKNOWN` | `REJECTED`.

| Адрес | Параметр | Тип / кол-во | Масштаб / offset | Ед. | Источник | Статус | Комментарий |
|---:|---|---|---|---|---|---|---|
| 184 | Battery SOC | U_WORD ×1 | ×1 | % | #3,#4,#5, Stage 19/21/28/33 | **PHYSICALLY_CONFIRMED** | Единственный активный регистр |
| 183 | Battery voltage | U_WORD ×1 | ×0.01 | V | #3,#4,#5, Stage 23 (`0x14E9`→53.53 В) | CORROBORATED | Ответ FC03 был, но нет синхронного дисплея |
| 191 | Battery current | S_WORD ×1 | ×0.01 | A | #3,#4,#5, Stage 23 (`0xFEA1`→−3.51 А) | CORROBORATED | S_WORD подтверждён отрицательным значением |
| 186 | PV1 power | U_WORD ×1 | ×1 | W | #3,#4,#5, Stage 23 (1519) | CORROBORATED | U_WORD vs S_WORD при отрицательных не проверен |
| 187 | PV2 power | U_WORD ×1 | ×1 | W | #3,#4,#5, Stage 23 (796) | CORROBORATED | То же |
| 188 | PV3 power | U_WORD ×1 | ×1 | W | #3,#4,#5, Stage 23 (123) | CORROBORATED | Separate register; NOT a 186–188 range as one parameter |
| 190 | Battery power | S_WORD ×1 | **×1 (conflict: local candidate ×0.1)** | W | #3,#4,#5; Stage 23 raw `0xF8C3`=−1853 | PROVISIONAL | Два независимыхmaps дают ×1; локальная гипотеза ×0.1 из V×I. Знак заряда/разряда тоже не подтверждён дисплеем |
| 90 | DC temperature | S_WORD ×1 | ×0.1; local candidate offset −100 | °C | #3,#5 (без offset), локальная карта (offset) | PROVISIONAL | Raw 1410 → 141 °C или 41 °C; конфликт источников |
| 91 | AC temperature | S_WORD ×1 | ×0.1; local candidate offset −100 | °C | #3,#5 (без offset), локальная карта (offset) | PROVISIONAL | Raw 1482 → 148 °C или 48 °C |
| 109 / 111 / 113 | PV1/2/3 voltage | U_WORD ×1 | ×0.1 | V | #3,#5 | CORROBORATED | Не читались физически |
| 110 / 112 / 114 | PV1/2/3 current | U_WORD ×1 | ×0.1 | A | #3,#5 | CORROBORATED | Не читались физически |
| 108 | Daily PV production | U_WORD ×1 | ×0.1 | kWh | #3,#4,#5 | CORROBORATED | Счётчик, не мощность |
| 96–97 | Total PV production | U_DWORD ×2, LW=96,HW=97 | ×0.1 | kWh | #3,#4,#5 | CORROBORATED | Порядок слов LW,HW требует физического подтверждения |
| 150 / 151 | Grid voltage L1/L2 | U_WORD ×1 | ×0.1 | V | #3,#5 | CORROBORATED | 151 **не** частота |
| 169 | Total grid power | S_WORD ×1 | **×10** (у #1LP1 ×1) | W | #3,#5 | PROVISIONAL | Масштаб и знак import/export требуют сверки с HMI |
| 167 / 168 / 170 / 171 | CT power L1/L2 internal/external | S_WORD ×1 | ×1 | W | #3,#5 | CORROBORATED | 167 ≠ 169 ≠ 172 (разные величины) |
| 175 | Total power (load) | S_WORD ×1 | ×1 | W | #3,#5 | PROVISIONAL | Возможная мощность нагрузки, не сверена |
| 192 | Load frequency | U_WORD ×1 | ×0.01 | Hz | #3,#5 | CORROBORATED | 5000 → 50.00 Гц |
| 182 | Battery temperature | U_WORD ×1 | ×0.1 (offset dispute) | °C | #4; локальный HA-лог 222 °C | UNKNOWN | Результат нефизичный без уточнения |
| 72–73 / 74–75 | Total battery charge/discharge | U_DWORD ×2 LW,HW | ×0.1 | kWh | #4,#5 | CORROBORATED | Счётчики |
| 70 / 71 | Daily battery charge/discharge | U_WORD ×1 | ×0.1 | kWh | #4 | CORROBORATED | Не читались |
| 76 / 77 / 78,80 / 81,82 | Grid energy bought/sold | U_WORD / U_DWORD | ×0.1 | kWh | #3,#5 | CORROBORATED | Не читались; у 78/80 необычный адрес HW=80 |
| 189 | Battery status | U_WORD ×1 | ×1 | code | #4 | UNKNOWN | Нет карты кодов |
| 103 / 104 | Fault codes | — | — | — | локальные документы | UNKNOWN | Формат не подтверждён |
| 194 | Grid connected | U_WORD ×1 | 0/1 | — | HA-лог, Stage 22 | UNKNOWN | Требует проверки |
| 151 как «grid frequency» | — | — | — | — | устаревший SunPortal-файл | **REJECTED** | 151 — Grid Voltage L2, не частота |
| 60/63/76/78/80/84 как «энергия» (старая карта) | — | — | — | — | legacy-документы | **REJECTED** | Не соответствует карте SG02LP1 |

Ни один layout не имеет статуса `OFFICIALLY_DOCUMENTED`. Все CORROBORATED-записи основаны на сторонних реализациях и/или исторических FC03-ответах без синхронного HMI-сравнения.

## 7. Типы, масштабы, order и endianness (PHASE E)

| Тип | Правило | Где реализовано | Тест |
|---|---|---|---|
| U_WORD | raw 0..65535 без знака | `domain/normalizer.py::decode_u_word` | Stage 22/34 |
| S_WORD | two's complement: `raw−65536` при raw>32767 | `decode_s_word`, `decode_register::_decode_word` | Stage 22/34 |
| U_DWORD | `low + (high<<16)`, LW по младшему адресу, затем ×scale | **только локальное правило в offline-тесте**, production decoder 32-bit не имеет | Stage 22/34 |
| S_DWORD | тот же raw, знак по `raw−2^32` при >0x7FFFFFFF | только offline-тест | Stage 34 |
| Float32 | не используется ни в одной карте;доказательств нет | — | NOT APPLICABLE |
| Word swap | LW,HW vs HW,LW — различаются; LW,HW принят #3/#4/#5 | offline-тест проверяет различие | Stage 34 |
| Scaling | 0.01 (V, A), 0.1 (V/A PV, kW→kWh, °C), 1 (W, %), 10 (169) | `apply_scale`, `decode_register` | Stage 22/34 |
| Temperature | `raw×0.1−offset`, offset 100 — **конфликт** #3/#5 (без offset) vs локальная карта | `apply_temperature_offset`, `decode_register(use_candidate=True)` | Stage 22/34 |

Известные риски неправильного декодирования: отрицательная мощность как 65000+ при U_WORD; `×0.1` vs `×1` для 190; 141 °C vs 41 °C для 90/91; Wh vs kWh; W vs kW; перепутанный порядок слов DWORD. Все они зафиксированы в тестах и не «исправляются догадкой».

## 8. Результаты offline-тестов (PHASE F/K)

Новый файл: `tests/test_stage34_register_validation.py` — **23 теста**, все PASS. Покрытие: U_WORD/S_WORD/U_DWORD/S_DWORD, scale и temperature offset, endianness/word order, `null`/invalid/out-of-range → `None` (никогда 0), timeout и Modbus exception на mock-транспорте, missing register, invalid response length, stale snapshot, timestamp UTC, отсутствие write-методов, отказ DEMO в production pipeline, метка `demo_simulator`, отсутствие demo-импортов в entrypoint, отсутствие `random` в production-исходниках, active map = `{184}`.

Полный backend suite: **299 PASS** (276 предыдущих + 23 новых).

Статусы проверок:

| Проверка | Статус |
|---|---|
| Backend unit tests | PASS |
| Modbus decoder tests | PASS |
| Offline mock FC03 failure tests | PASS |
| Simulator isolation test | PASS |
| Production source isolation test | PASS |
| API tests (существующие) | PASS |
| Android tests | PASS (код не менялся, 115 из Stage 32) |
| Новые физические FC03-запросы | **NOT RUN (запрещено)** |
| Сверка с дисплеем инвертора | **NOT RUN (нет данных от владельца)** |
| 32-bit decoder в production | **NOT APPLICABLE** (реализован только offline) |

## 9. Найденные проблемы

1. `domain/register_metadata.py` помечает 183, 186–188, 191 как `physical_status="PHYSICALLY_READ"` по значению Stage 23. По определению Stage 34 (`PHYSICALLY_CONFIRMED` = прочитано **и** сопоставлено с дисплеем) они таковыми не являются. Рекомендуется переклассифицировать в `CORROBORATED` при следующем изменении карты (в этом этапе метки не менялись).
2. Локальный `candidate_scale=0.1` для регистра 190 противоречит двум независимым SG02LP1-картам (#3, #4, #5), где указано ×1. Гипотеза V×I правдоподобна, но не подтверждена;мы не меняем scale догадкой.
3. Локальный `offset=-100` для 90/91 противоречит текущим документам #3/#5 (только ×0.1). Более старый анализ (#8) утверждал offset −100; источники конфликтуют.
4. `/status` отдаёт `available_registers=9` и список всех 9 адресов, хотя физически опрашивается только 184. Это не ложные данные, но может вводить в заблуждение; стоит показывать `active_registers` отдельно.
5. Если `SUNLINK_DATA_SOURCE=demo`, `DeyeGateway` без `telemetry_source` всё равно вызывает физический `ModbusTcpClient` в `read_all_registers()` — потенциальный скрытый физический fallback. В production сейчас source=`deye_modbus`, поэтому проблема не активна, но архитектурно её стоит устранить.
6. В `ReadOnlyModbusClient.read_holding_registers()` не проверяется `Fc03Result.isError()`; пустой список вызывает `IndexError` в `read_register()`. Poller ловит исключение и не создаёт ложное значение — тест это подтверждает, но диагностика могла бы быть точнее.
7. `docs/stages/stage-9.2…` сообщает, что web-поиск был недоступен; часть выводов о «отсутствии официальной карты» основана на анализе одного PDF. Текущая проверка (#1) это подтверждает: на странице продукта register map нет.

## 10. Архитектура будущей телеметрии (PHASE I)

- Один `DeyePollingService` на `inverter_id`; реестр `_ACTIVE_POLLERS` уже запрещает второй.
- `ACTIVE_POLL_REGISTERS` остаётся единственным списком физического опроса; расширение — только через явный, документированный список, проверенный по `CANDIDATE_REGISTERS`.
- `ReadOnlyModbusClient` + `ModbusTcpTransport`: только FC03, один запрос = одно соединение, без retry и без write-методов.
- Декодирование — чистые функции `domain/register_decoder.py`; 32-bit decoder добавить отдельно с тестами до включения счётчиков.
- Каждое значение: `TelemetryValue(raw, normalized, unit, confidence, quality, address, timestamp)`; источник `DataSource.DEYE_MODBUS` только для реальных кадров.
- SQLite: аддитивные миграции, backup до изменения схемы; историяraw сохраняется.
- REST/WS контракт не меняется; Android продолжает получает `null` как «Немає даних».
- DEMO/mock никогда не импортируются в production entrypoint; `build_production_poller()` уже отказывает для demo.
- Частота опроса фиксированная (30 с физический интервал), backoff 5→60 с.
- Никакого прямого доступа Android к Modbus/Ebyte, второго poller или параллельного процесса.

## 11. NEXT_READ_ONLY_TEST_PLAN

Все строки ниже — **кандидаты**, требующие отдельного разрешения владельца. Ни один не включён в production poller. Формат запроса: FC03, Unit ID 1, один адрес на соединение, без retry, без записи, интервал ≥30 с.

| Приоритет | Адрес | Параметр | Кол-во | Тип | Масштаб | Ожидаемый диапазон | Сверка | Условие успеха | Риск |
|---:|---:|---|---:|---|---|---|---|---|---|
| 1 | 183 | Battery voltage | 1 | U_WORD | ×0.01 | 40–60 В | DISPLAY: battery page | decoded в диапазоне и совпадает с дисплеем ±0.5 В | низкий |
| 1 | 191 | Battery current | 1 | S_WORD | ×0.01 | −250…250 А | DISPLAY: battery page | знак и модуль совпадают с дисплеем | знак может быть инвертирован |
| 1 | 190 | Battery power | 1 | S_WORD | ×1 **и** кандидат ×0.1 | −3000…3000 Вт | DISPLAY + V×I | выбран масштаб, знак согласован с 191 | конфликт источников |
| 2 | 186 | PV1 power | 1 | U_WORD | ×1 | 0…20000 Вт | DISPLAY: PV1 | совпадение ±1 % | отрицательный PV не проверен |
| 2 | 187 | PV2 power | 1 | U_WORD | ×1 | 0…20000 Вт | DISPLAY: PV2 | совпадение ±1 % | то же |
| 2 | 188 | PV3 power | 1 | U_WORD | ×1 | 0…20000 Вт | DISPLAY: PV3 | совпадение ±1 % | только на 12 кВт есть 3 MPPT |
| 2 | 109/111/113 | PV voltage | 1each | U_WORD | ×0.1 | 0…500 В | DISPLAY | совпадение ±1 % |исторический raw 2733→273.3 В |
| 2 | 110/112/114 | PV current | 1each | U_WORD | ×0.1 | 0…30 А | DISPLAY | совпадение ±0.2 А | малые токи могут быть 0.3 А |
| 3 | 150/151 | Grid voltage L1/L2 | 1each | U_WORD | ×0.1 | 200–260 В | DISPLAY/meter | совпадение ±1 % | 151 ≠ частота |
| 3 | 169 | Total grid power | 1 | S_WORD | ×10 (проверить ×1) | −12000…12000 Вт | DISPLAY/meter | масштаб и знак подтверждены | конфликт ×1/×10 |
| 3 | 192 | Frequency | 1 | U_WORD | ×0.01 | 49–51 Гц | DISPLAY | 5000 → 50 Гц | — |
| 3 | 175 | Load power | 1 | S_WORD | ×1 | 0…12000 Вт | DISPLAY + баланс | совпадение ±1 % | адрес из другой группы |
| 4 | 90/91 | DC/AC temperature | 1each | S_WORD | ×0.1, offset? | −10…80 °C | DISPLAY | разрешить конфликт offset | 141 °C явно нефизично |
| 4 | 96–97 / 108 | Energy counters | 2 / 1 | U_DWORD/U_WORD | ×0.1 | 0…100000 kWh | DISPLAY | порядок слов и единицы | нулевое/переполненное значение при HW,LW |

## 12. Предложение Stage 35

Только после отдельного разрешения: первая физическая сессия — **три одиночных FC03-чтения** адресов `183`, `191`, `190` с интервалом 30 с, без записей, без сканирования, без изменения production poller. Владелец одновременно фотографирует дисплей батареи (V, I, P, SOC). Затем сравниваем raw→decoded с дисплеем, выбираем scale для 190 и знак, обновляем `RegisterMetadata` и только после второго разрешения добавляем адреса в `ACTIVE_POLL_REGISTERS` по одному. Следующая сессия — PV (186–188) и V/I (109–114), затем_grid/load/temps.

## 13. Что НЕ изменялось

- Production-сервисы не перезапускались; `sunlink-https.service`, poller и регистр 184 работают как после Stage 33.
- `.env`, systemd, SQLite, сертификаты, MikroTik, NAT, DNS, firewall, Ebyte, инвертор — без изменений.
- Android production-код и APK не менялись; новый релиз не создавался.
- Ни одного физического Modbus-запроса и ни одной write-команды.
- Мастер-ТЗ `мобильное приложение.txt` не трогался.

## STAGE 34 — FINAL REPORT

### 1. Общий статус
**PASS** — аудит, карта регистров, offline-тесты и план выполнены; физическое расширение не выполнялось по требованию.

### 2. Git
Initial HEAD `43817c4bdac044ac57dd80a5325387542398278f`; commit `19b140aa8966d839d49fe21ea68ad4aef7019a24` (`stage34: document Deye register map and telemetry validation plan`), затем docs-коммит с этим отчётом; branch `main`; `HEAD = origin/main`; untracked мастер-ТЗ сохранён.

### 3. Production safety
Production не изменялся (см. §13).

### 4. Изученные компоненты
См. §2 и §3.

### 5. Реальные регистры
Физически подтверждён только `184` ( Battery SOC); остальные — CORROBORATED / PROVISIONAL / UNKNOWN / REJECTED.

### 6. Таблица кандидатов
См. §6 и `NEXT_READ_ONLY_TEST_PLAN` в §11.

### 7. Тесты
Backend 299 PASS (23 новых Stage 34); Android 115 PASS (без изменений); физические тесты NOT RUN; HMI-сверка NOT RUN.

### 8. Найденные проблемы
См. §9 (7 пунктов).

### 9. Изменённые файлы
`tests/test_stage34_register_validation.py`, этот отчёт, README/CHANGELOG (см. Git diff).

### 10. Production
Сервисы не перезапускались, poller не менялся, Ebyte не перенастраивался, Modbus write не выполнялся, инвертор/MikroTik/DNS/NAT/firewall/TLS не изменялись.

### 11. Следующий этап
Stage 35 — разрешённые FC03-чтения 183/191/190 с синхронной фотографией дисплея.

### 12. Что требуется от пользователя
Отдельное разрешение на физические read-only запросы к адресам **183, 191, 190** (первая сессия) и синхронные показания дисплея батареи. Без этого расширение active map не производится.
