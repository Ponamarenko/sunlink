# Stage 22 — Deye Real Telemetry Register Validation & Expansion Plan

**Итог Stage 22:** карта телеметрии подготовлена; активный физический map по-прежнему содержит только регистр 184; план следующего физического теста вынесен на подтверждение владельца.
**HEAD до/после:** `a9c6df938be635edbe5176347f29c5701261b062`.
**Физических запросов в Stage 22: 0.**

## 1. Изучённые исторические источники

| Источник | Репозиторий / коммит | Что дал |
|---|---|---|
| `docs/modbus-register-map.md` | sunlink | Карта SG02LP1 уровня B: адреса, типы, масштабы, DWORD LW/HW, список исключённых |
| `docs/stages/stage-3-first-real-read.md` | sunlink [`393cc08`](https://github.com/Ponamarenko/sunlink/commit/393cc08) | Документ-claim: 21 регистр, raw-значения PV/battery/energy/temps |
| `docs/stages/stage-4-modbus-display-verification.md` | sunlink [`4fa01e6`](https://github.com/Ponamarenko/sunlink/commit/4fa01e6) | Документ-claim: 11 регистров, raw/HEX, время чтения, попытка сверки с дисплеем |
| `docs/stages/stage-6-ha-register-cross-check.md` | sunlink [`6020410`](https://github.com/Ponamarenko/sunlink/commit/6020410) | Лог HA: 166/169/175–178/182–194 со значениями; масштабы Sunsynk |
| `docs/stages/stage-9.2-official-protocol-documentation.md` | sunlink | Официальных register map Deye НЕТ; Ebyte factory defaults; Simple mode — 1 master |
| `docs/stages/stage-9.3.1-evidence-reconciliation.md` | sunlink | Первичных логов Stage 3/4 в репозитории нет — только document-claims |
| `deye_readonly_adapter.py` | sunlink | Старая карта: PV U_WORD, temps без offset, grid ×10, DWORD 96/72/74 |
| `deye_gateway.py` initial | sunlink [`64ed6b4`](https://github.com/Ponamarenko/sunlink/commit/64ed6b4) | Старый gateway с write-логикой и IP `.1.100` (устарел) |
| `deye_gateway.py` Deye integration | SunPortal [`e93d3a7`](https://github.com/Ponamarenko/SunPortal/commit/e93d3a7) | Дубликат reg 186, 185 battery_voltage, 191 ×0.1 — противоречия |
| Stage 16–21 | sunlink | Единый TelemetrySource, PHYSICALLY_READ 184 (99 % → 95 %), polling engine |
| Наблюдение владельца | сообщение Stage 22 | Перед Stage 21 дисплей показывал 95 %, батарея заряжалась от солнца — НЕ синхронная сверка |

## 2. Полная карта телеметрии

Уровни: VERIFIED / PHYSICALLY_READ / CORROBORATED / PROVISIONAL / UNKNOWN.
Ни один параметр не имеет VERIFIED: синхронной сверки с дисплеем документально нет.

### Аккумулятор

| Параметр | Адрес | Кол-во | Тип | Порядок | Scale/offset | Ед. | Знак | Источник | Достоверность | Физ. проверка |
|---|---:|---:|---|---|---|---|---|---|---|---|
| SOC | 184 | 1 | U_WORD | — | ×1 | % | unsigned | HA, Sunsynk, Stage 19/21 | **PHYSICALLY_READ** | 3 чтения Stage 21 (95 %), 1 Stage 19 (99 %) |
| Battery voltage | 183 | 1 | U_WORD | — | ×0.01 | V | unsigned | HA 53.02, Stage 3 53.26, Stage 4 53.22 | CORROBORATED | только document-claim Stage 3/4 |
| Battery current | 191 | 1 | S_WORD | — | ×0.01 | A | signed | HA raw 361→3.61, Stage 4 raw 81→0.81 | CORROBORATED | только document-claim Stage 3/4 |
| Battery power | 190 | 1 | S_WORD | — | ×1 | W | **знак спорен** | HA 1915 W, Stage 4 raw 434, дисплей 2426 W разряда | PROVISIONAL | document-claim, знак не сверен |
| Battery temperature | 182 | 1 | U_WORD | — | ×0.1 | °C | unsigned | HA 222 °C (нефизично) | UNKNOWN | нет |

### Солнечные панели

| Параметр | Адрес | Кол-во | Тип | Scale | Ед. | Источник | Достоверность | Физ. проверка |
|---|---:|---:|---|---:|---|---|---|---|
| PV1/2/3 power | 186/187/188 | 1 каждый | U_WORD* | ×1 | W | HA 211/131/107; Stage 3 101/66/52; Stage 4 93/57/49 | CORROBORATED | document-claim; *в Data Layer помечены S_WORD — см. §3 |
| PV1/2/3 voltage | 109/111/113 | 1 каждый | U_WORD | ×0.1 | V | Stage 3 raw 2733→273.3, 2761→276.1, 2444→244.4 | PROVISIONAL | один исторический источник |
| PV1/2/3 current | 110/112/114 | 1 каждый | U_WORD | ×0.1 | A | Stage 3 raw 3→0.3, 2→0.2 | PROVISIONAL | один исторический источник |

### Сеть и нагрузка

| Параметр | Адрес | Кол-во | Тип | Scale | Ед. | Источник | Достоверность |
|---|---:|---:|---|---:|---|---|---|
| Grid CT L1 power | 167 | 1 | S_WORD | ×1 | W | HA 0, Stage 3/4 0, kbialek | CORROBORATED (адрес), значение 0 непоказательно |
| Total grid power | 169 | 1 | S_WORD | **×1 vs ×10** | W | Sunsynk ×1; локальный адаптер ×10 | UNKNOWN (масштаб) |
| Grid voltage L1 | 150 | 1 | U_WORD | ×0.1 | V | kbialek | PROVISIONAL |
| Grid voltage L2 | 151 | 1 | U_WORD | ×0.1 | V | kbialek; SunPortal называет это «grid_frequency» ×0.01 | UNKNOWN (противоречие) |
| Grid frequency | 192 | 1 | U_WORD | ×0.01 | Hz | HA raw 5000→50 | PROVISIONAL |
| Load power | 175–178 | 1 | S_WORD | ×1 | W | HA 2084/208, Sunsynk single_phase | PROVISIONAL (адреса исключены из read-only драйвера) |
| Load voltage/current | — | — | — | — | — | достоверного адреса нет | UNKNOWN |
| Direction import/export | вычисляется | — | — | — | — | из знака grid power | UNKNOWN, пока не проверен знак 169 |

### Инвертор и температура

| Параметр | Адрес | Кол-во | Тип | Scale/offset | Ед. | Источник | Достоверность |
|---|---:|---:|---|---|---|---|---|
| DC temperature | 90 | 1 | S_WORD | ×0.1 (−100 ?) | °C | Stage 4 raw 1365 → 136.5 или 36.5 | PROVISIONAL |
| AC temperature | 91 | 1 | S_WORD | ×0.1 (−100 ?) | °C | Stage 4 raw 1424 → 142.4 или 42.4 | PROVISIONAL |
| Grid connected | 194 | 1 | binary | — | — | HA False | PROVISIONAL |
| Fault codes | 103/104 | — | — | — | — | формат не подтверждён | UNKNOWN |
| Efficiency | computes | — | — | — | % | только при наличии всех входов | UNKNOWN |

### Энергия

| Параметр | Адрес | Кол-во | Тип | Порядок | Scale | Ед. | Источник | Достоверность |
|---|---:|---:|---|---|---|---|---|---|
| Total PV production | 96–97 | 2 | U_DWORD | LW=96, HW=97 | ×0.1 | kWh | Stage 3 raw 6433→643.3, kbialek | PROVISIONAL |
| Daily PV production | 108 | 1 | U_WORD | — | ×0.1 | kWh | Stage 3 raw 37→3.7 | PROVISIONAL |
| Total battery charge | 72–73 | 2 | U_DWORD | LW,HW | ×0.1 | kWh | Stage 3 raw 5105→510.5 | PROVISIONAL |
| Total battery discharge | 74–75 | 2 | U_DWORD | LW,HW | ×0.1 | kWh | Stage 3 raw 4858→485.8 | PROVISIONAL |
| Grid import/export energy | 76/77, 78/80, 81/82 | 1–2 | U_WORD/U_DWORD | LW,HW | ×0.1 | kWh | только HA-имена, значений нет | UNKNOWN |
| Load energy | 84 | 1 | U_WORD | — | ×0.1 | kWh | только HA-имя | UNKNOWN |

## 3. Спорные адреса и причины расхождений

| Адрес/вопрос | Источник A | Источник B | Решение Stage 22 |
|---|---|---|---|
| 186 | SunPortal: `battery_temp` U_WORD 0.1 °C | все остальные: `pv1_power` W | В dict SunPortal дубликат ключа — battery_temp молча теряется. Считать 186 PV1 power; температуру батареи через 182 не подтверждать |
| 183 vs 185 | текущая карта: 183 battery_voltage | SunPortal: 185 battery_voltage | Оставить 183; 185 не добавлять без доказательств |
| 191 scale | HA/Stage 3/4: ×0.01 (361→3.61 A) | SunPortal: ×0.1 | Оставить ×0.01; старое значение ошибочно |
| 186–188 тип | `register_metadata.py`: S_WORD | `modbus-register-map.md` и адаптер: U_WORD; HA/Sunsynk: signed ×1 | Для положительных значений результат одинаков. Не менять тип без физического отрицательного значения; пометить PROVISIONAL по знаку |
| 90/91 offset | адаптер: ×0.1 без offset | register_metadata/kbialek: ×0.1 −100 | Не выбирать; требует сверки с дисплеем |
| 169 scale | Sunsynk/HA: ×1 | локальный адаптер/карта: ×10 | UNKNOWN; не показывать мощность сети как достоверную |
| 151 | kbialek: Grid voltage L2 | SunPortal: grid_frequency ×0.01 | UNKNOWN; не добавлять |
| 190 sign | адаптер: + = charge | Sunsynk: + = discharge; Stage 4 дисплей 2426 W vs raw 434 | Не инвертировать знак по предположению; PROVISIONAL |
| DWORD адреса | старый SunPortal: 60/63/74 | актуальная карта: 96–97/108/72–75 | Оставить актуальные; порядок LW,HW от kbialek |
| Энергия сети | HA 76/77/78/80/81/82 | карта исключает 76–84 | UNKNOWN; не добавлять |

## 4. Parameters, уже можно подключить

Только **184 SOC** — активный map Stage 20, PHYSICALLY_READ. Всё остальное требует как минимум одного разрешённого физического чтения и/или разрешения противоречия.

## 5. Параметры, требующие физической проверки

PV power 186–188, battery voltage 183, battery current 191, battery power 190 (знак), temperatures 90/91 (offset), grid power 169 (масштаб), grid voltage 150, PV voltage/current 109–114, load power 175–178, energy DWORD 96–97/72–75/108.

## 6. Минимальный план следующего физического теста (НЕ выполнять без подтверждения)

Принцип: только FC03, staging 30 с, без retry, без записи, только перечисленные адреса.

| # | Адрес | Кол-во | Тип | Ожидаемый диапазон | Почему проверять | Сравнение с дисплеем |
|---:|---:|---:|---|---|---|---|
| 1 | 186 | 3 (186–188) | U_WORD ×1 W | 0..12000 Вт | PV power — главный показатель; непрерывный блок 3 регистров, не несвязанные адреса | PV1/PV2/PV3 power на дисплее |
| 2 | 183 | 1 | U_WORD ×0.01 В | 40..60 В | Напряжение батареи, несколько источников но нет первичного лога | Battery voltage |
| 3 | 191 | 1 | S_WORD ×0.01 А | −100..100 А | Ток батареи + знак | Battery current |
| 4 | 190 | 1 | S_WORD ×1 Вт | −12000..12000 Вт | Разрешить конфликт знака | Battery charge/discharge power |
| 5 | 90 | 2 (90–91) | S_WORD ×0.1 °C | −20..100 °C (с offset) или 100..200 без | Разрешить offset −100 | Heat-sink / DC temperature |

Итого 5 FC03-запросов (один блок из 3 регистров и один блок из 2), 30 с между, максимум 5. До физического теста владелец должен подтвердить точный список, количество и интервал. Текущее разрешение Stage 21 исчерпано.

## 7. Архитектура расширения

- `ACTIVE_POLL_REGISTERS` расширять **только** после физического подтверждения каждого адреса.
- `normalizer.py` уже поддерживает U_WORD/S_WORD и scale/offset — переиспользовать.
- Для DWORD добавить чистую функцию `low + (high << 16)` с scale и явной проверкой диапазона (тесты Stage 22 уже задают правила), но не подключать к polling без разрешения.
- Отрицательная мощность: декодировать S_WORD как two's complement; знак НЕ инвертировать, пока не подтверждён.
- Недоступный регистр → `TelemetryValue(normalized_value=None, quality=unavailable)`; ноль не подставляется.
- При ошибке — последнее корректное значение + `stale=True` (уже реализовано в Stage 20).
- Источники различаются метками `demo_simulator` / `mock` / `deye_modbus`; fallback физического на DEMO запрещён.
- Write-функций и write-endpointов не добавлять.

## 8. Android и API

Существующий контракт сохраняется: `LiveRawDto` уже имеет `p_sun`, `pv1/2/3_power`, `batt_soc`, `battery_voltage`, `battery_current`, `dc/ac_temperature`, `grid_voltage`, `timestamp`, `connection_status`, `source`, `inverter_id`; `MeasurementDto` — `parameter_name`, `normalized_value`, `unit`, `quality`, `confidence`, `source`, `timestamp`.

Нужны интерфейсу: реальные PV, заряд/разряд батареи, направление потоков, мощность нагрузки, состояние подключения, время последнего успешного измерения, признак stale, источник. Большинство полей уже присутствует; новые поля (например `last_success`) можно добавить только обратно совместимо, без ломки DEMO/Mock. Android-код в Stage 22 не менялся — APK не собирался.

## 9. Тесты и ограничения

`python3 -m unittest discover -s tests` — **226 PASS** (212 Stage 20 + 14 новых Stage 22). Новые тесты покрывают U_WORD/S_WORD, отрицательную мощность, DWORD LW/HW и signed, некорректные значения, null для неподтверждённых параметров, дисциплину активного map и уровни достоверности.

Физические запросы в Stage 22 **НЕ выполнялись**: нет TCP к Ebyte, ping/nmap/scan/Modbus. Production backend, `.env`, systemd, Redis, HA, firewall, MikroTik, Ebyte и конфигурация инвертора не изменялись.

## 10. Итог

Точно известны физически подтверждённые данные: только SOC регистра 184 (3 чтения 95 %, одно 99 %). Все остальные параметры имеют документные источники с разной степенью достоверности и требуют отдельно разрешённого минимального физического теста из §6.
