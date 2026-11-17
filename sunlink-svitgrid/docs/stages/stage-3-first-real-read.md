# Этап 3 — Первый реальный READ-ONLY опрос Deye через Ebyte

**Статус:** выполнен. Физический read-only опрос проведён 17 ноября 2026.  \n**Режим:** строго read-only, один цикл чтения, без интеграции в backend, без фоновых процессов.

## 1. Параметры подключения

| Параметр | Значение |
|---|---|
| Хост (Ebyte NA111-E) | `192.168.25.15` |
| TCP порт | `502` |
| UART | `9600 8N1` |
| Режим | Modbus TCP → RTU, Simple |
| Function Code | 03 (Read Holding Registers только) |
| Таймаут | 2 с |
| Slave/Unit ID | **1 (подтверждён)** |
| Дата/время начала | `2026-11-17T13:09:28.493224+00:00` |
| Версия pymodbus | `3.16.2` |
| Параметр клиента | `device_id` (поддерживается), `slave` не используется |

## 2. Предварительные проверки безопасности (пройдены перед подключением)

| Проверка | Результат |
|---|---|
| TCP порт `192.168.25.15:502` | OPEN |
| Существующие TCP-соединения на :502 | отсутствуют |
| Существующие TCP-соединения с `192.168.25.15` | отсутствуют |
| Другой Modbus Master к Ebyte | не обнаружен (пользователь подтвердил остановку клиента Home Assistant / sunsynk на `.14:8899`) |
| Адаптер содержит только FC 03 | подтверждено (нет методов write_*) |
| Диагностический скрипт: один цикл, нет polling, нет фоновых задач, закрывает соединение | подтверждено |
| 22 mock-теста адаптера | 22/22 прошли (до и после исправления бага скрипта) |

## 3. Подтверждение Slave ID

Выполнен ограниченный перебор FC03 к регистру 184 (Battery SOC):

| Slave ID | Результат | reg184 raw | reg184 value |
|---|---|---|---|
| 1 | **OK** | 86 | 86.0% |

Slave ID **= 1 подтверждён**. Перебор 2–10 не requeridos. Широковещательные запросы не отправлялись.

## 4. Результат диагностического опроса (один цикл read_all())

Время подключения: 3.0 ms  
Время чтения: 960.0 ms  
Ошибок: none  
Соединение закрыто по завершении: `not adapter.connected = True` (активных TCP-соединений к `.15:502` после работы скрипта не обнаружено)

### Регистры (сырые и декодированные значения)

| Адрес | Параметр | Тип | Raw | Значение | Ед. | Источник | Подтверждён |
|---:|---|---|---:|---:|---|---|---|
| 72 | Total Battery Charge | U_DWORD | 5105 | 510.5 | kWh | measured | True |
| 74 | Total Battery Discharge | U_DWORD | 4858 | 485.8 | kWh | measured | True |
| 90 | DC Temperature | S_WORD | 1369 | 136.9 | °C | measured | True |
| 91 | AC Temperature | S_WORD | 1426 | 142.6 | °C | measured | True |
| 96 | Total PV Production (DWORD low 96 + high 97) | U_DWORD | 6433 | 643.3 | kWh | measured | True |
| 108 | Daily PV Production | U_WORD | 37 | 3.7 | kWh | measured | True |
| 109 | PV1 Voltage | U_WORD | 2733 | 273.3 | V | measured | True |
| 110 | PV1 Current | U_WORD | 3 | 0.3 | A | measured | True |
| 111 | PV2 Voltage | U_WORD | 2761 | 276.1 | V | measured | True |
| 112 | PV2 Current | U_WORD | 2 | 0.2 | A | measured | True |
| 113 | PV3 Voltage | U_WORD | 2444 | 244.4 | V | measured | True |
| 114 | PV3 Current | U_WORD | 2 | 0.2 | A | measured | True |
| 167 | Grid CT L1 Power | S_WORD | 0 | 0.0 | W | measured | True |
| 169 | Total Grid Power | S_WORD | 0 | 0.0 | W | measured | True |
| 183 | Battery Voltage | U_WORD | 5326 | 53.26 | V | measured | True |
| 184 | Battery SOC | U_WORD | 86 | 86.0 | % | measured | True |
| 186 | PV1 Power | U_WORD | 101 | 101.0 | W | measured | True |
| 187 | PV2 Power | U_WORD | 66 | 66.0 | W | measured | True |
| 188 | PV3 Power | U_WORD | 52 | 52.0 | W | measured | True |
| 190 | Battery Power | S_WORD | 279 | 279.0 | W | measured | True |
| 191 | Battery Current | S_WORD | 52 | 0.52 | A | measured | True |

DWORD пары (LW,HW): 96–97, 72–73, 74–75. Сборка `value = low_word + (high_word << 16)`. Порядок LW,HW подтверждён независимой реализацией kbialek/deye-inverter-mqtt (`DoubleRegisterSensor(low_word_first=True)`).

### Сводка (измеренное vs расчётное)

| Поле | Значение | Источник |
|---|---|---|
| p_sun | 219.0 W | calculated (101+66+52) |
| p_batt | 279.0 W | measured (reg 190) |
| battery_soc | 86.0 % | measured (reg 184) |
| battery_voltage | 53.26 V | measured (reg 183) |
| p_grid_measured | 0.0 W | measured (reg 169) |
| p_grid_source | measured_register_169 | — |
| p_grid_estimated | None | нет данных о нагрузке (reg total_load_power не в карте) |
| dc_temperature | 136.9 °C | measured (reg 90, ×0.1, без offset) |
| ac_temperature | 142.6 °C | measured (reg 91, ×0.1, без offset) |
| total_pv_production | 643.3 kWh | measured (reg 96–97) |
| daily_pv_production | 3.7 kWh | measured (reg 108) |
| total_battery_charge | 510.5 kWh | measured (reg 72–73) |
| total_battery_discharge | 485.8 kWh | measured (reg 74–75) |

## 5. Особое внимание к температуре (регистры 90 и 91)

| Вариант | reg 90 (DC) | reg 91 (AC) |
|---|---|---|
| `raw × 0.1` (текущая карта) | 136.9 °C | 142.6 °C |
| `raw × 0.1 − 100` (kbialek offset) | 36.9 °C | 42.6 °C |

Вариант с offset −100 выглядит более реалистично для работы инвертора, но окончательный выбор требует сравнения с экраном инвертора. Карта регистров **не изменялась** — оба варианта зафиксированы для сверки пользователем.

## 6. Особое внимание к знакам и порядку слов

- S_WORD декодирование: `raw > 32767 → raw − 65536` — корректно (тест `test_negative_grid_power`).
- Регистр 190 (Battery Power) = +279 W (заряжается). Знак заряда/разряда требует сверки с экраном.
- Регистр 169 (Total Grid Power) = 0 W — в момент опроса экспорт/импорт отсутствовал.
- DWORD порядок LW,HW подтверждён сторонней реализацией.

## 7. Ошибки и таймауты

- Ошибок Modbus: нет.
- Таймаутов: нет.
- Непрочитанных регистров: нет (все 19 из карты получены).
- `p_grid_estimated` = None — расчёт невозможен без данных о нагрузке (регистр total_load_power не в проверенной карте). Расчётная мощность сети не выдавалась за измеренную.

## 8. Подтверждения безопасности

| Утверждение | Подтверждение |
|---|---|
| Только FC 03, geen write-операций | ✓ (адаптер не содержит write-методов; скрипт вызывает только `read_holding_registers`) |
| Регистры 166, 182, 76–82, 248–279 не читались | ✓ (исключены из опроса по инструкции этапа 3) |
| TCP-соединение закрыто после цикла | ✓ (активных соединений к `.15:502` после работы не обнаружено) |
| Фоновый polling не запущен | ✓ (скрипт завершился, процессов не осталось) |
| Backend не изменён | ✓ (`svitgrid.service`, `deye_gateway.py`, `.env`, Redis, firewall, Android, HA без изменений) |
| Драйвер не интегрирован в backend | ✓ |

## 9. Ограничения и нерешённые вопросы

1. Slave ID = 1 подтверждён эмпирически (один успешный запрос reg 184), но Official подтверждение отсутствует.
2. Температуры 90/91: два варианта масштабирования (с offset −100 и без) требуют сверки с дисплеем инвертора.
3. Знак Battery Power (reg 190) и Total Grid Power (reg 169) требуют сверки с экраном.
4. Карта регистров имеет уровень B для модели `…AM3` (официальное подтверждение не получено).
5. `p_grid_estimated` не рассчитан (нет данных о нагрузке).
6. В `readonly_diagnostic.py` был исправлен баг: `snapshot.last_successful_read` → `adapter.last_successful_read` (атрибут принадлежит адаптеру, не снимку). Баг не повлиял на сбор данных (все регистры прочитаны до падения), но мешал корректному завершению скрипта и вызове `disconnect()`. Исправление не добавляет write-функций. Тесты 22/22 проходят после исправления.

## 10. Что требует сверки с экраном инвертора (приоритет)

1. Battery SOC = 86% (reg 184)
2. Battery Voltage = 53.26 V (reg 183)
3. Battery Power = +279 W (reg 190, знак)
4. PV1/PV2/PV3 Power = 101/66/52 W (reg 186–188)
5. Grid Voltage — reg 150 не в текущей карте, требует добавления после сверки
6. DC Temperature = 136.9 °C или 36.9 °C (с offset −100) (reg 90)
7. AC Temperature = 142.6 °C или 42.6 °C (с offset −100) (reg 91)

## 11. GitHub

- Commit этапа 3: будет создан после завершения отчёта.
- Ветка: `main` (origin/main).
- Изменённые файлы: `docs/stages/stage-3-first-real-read.md`, `sunlink-svitgrid/server/readonly_diagnostic.py` (исправление бага).
