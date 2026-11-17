# Modbus Register Map — Deye SUN-12K-SG02LP1-EU-AM3 (SG02LP1 family)

**Статус:** карта сверена со сторонним источником (MarkLabs, таблица семейства SG02LP1).  
**Уровень уверенности:** A* для адресов/типов/масштабов, подтверждённых независимой реализацией kbialek/deye-inverter-mqtt; B — привязка к модели `SUN-12K-SG02LP1-EU-AM3` официально **не подтверждена**.  
Все значения требуют сверки с экраном инвертора при первом read-only опросе.

## Источники

| # | Источник | Ссылка | Уровень |
|---|---|---|---|
| 1 | Официальная страница продукта Deye (линейка SUN-7.6/8/10/12K-SG02LP1-EU-AM2/AM3) | https://www.deyeinverter.com/product/hybrid-inverter-1/sun7-6-8-10-12ksg02lp1euam2-am3-7-612kw-single-phase-2-mppt-hybrid-inverter-lv-battery-supported.html | Подтверждение модели/семейства |
| 2 | Сторонняя сводная карта регистров Deye (SG02LP1 выделена отдельно) | https://marklabs.pl/en/deye-modbus-complete-register-list/ | Уровень B (семейство SG02LP1) |
| 3 | Реализация Deye MQTT bridge с картой регистров SG02LP1 | https://github.com/kbialek/deye-inverter-mqtt (docs/metric_group_deye_sg02lp1.md, …_battery.md, …_timeofuse.md) | Уровень B (подтверждение адресов/типов/масштабов независимой реализацией) |
| 4 | Текущий read-only драйвер `sunlink-svitgrid/server/deye_readonly_adapter.py` | локальный файл | Реализация |

> Карта не является официальным документом Deye. Официальный Modbus Protocol / Register Map для SG02LP1-EU-AM2/AM3 не найден в рамках аудита.

## Подтверждённые регистры (уровень B, familia SG02LP1)

| Адрес (DEC) | Параметр | Тип | Кол-во рег. | Знак | Масштаб | Ед. | R/W | Источник | Уверенность | Примечание |
|---:|---|---|---:|:---:|---:|---|:---:|---|---|---|
| 186 | PV1 Power | U_WORD | 1 | U | 1 | W | R | 2 | A* | 10/12K — 3-й MPPT применим |
| 187 | PV2 Power | U_WORD | 1 | U | 1 | W | R | 2 | A* | |
| 188 | PV3 Power | U_WORD | 1 | U | 1 | W | R | 2 | A* | Только для 10/12K (3 MPPT) |
| 109 | PV1 Voltage | U_WORD | 1 | U | 0.1 | V | R | 2 | A* | |
| 110 | PV1 Current | U_WORD | 1 | U | 0.1 | A | R | 2 | A* | |
| 111 | PV2 Voltage | U_WORD | 1 | U | 0.1 | V | R | 2 | A* | |
| 112 | PV2 Current | U_WORD | 1 | U | 0.1 | A | R | 2 | A* | |
| 113 | PV3 Voltage | U_WORD | 1 | U | 0.1 | V | R | 2 | A* | |
| 114 | PV3 Current | U_WORD | 1 | U | 0.1 | A | R | 2 | A* | |
| 183 | Battery Voltage | U_WORD | 1 | U | 0.01 | V | R | 2 | A* | |
| 184 | Battery SOC | U_WORD | 1 | U | 1 | % | R | 2 | A* | |
| 190 | Battery Power | S_WORD | 1 | S | 1 | W | R | 2 | A* | Знак заряда/разряда требует проверки |
| 191 | Battery Current | S_WORD | 1 | S | 0.01 | A | R | 2 | A* | Знак заряда/разряда требует проверки |
| 167 | Grid CT L1 Power | S_WORD | 1 | S | 1 | W | R | 2 | A* | Измерение через внешний CT |
| 169 | Total Grid Power | S_WORD | 1 | S | ×10 | W | R | 2 | A* | Масштаб ×10 по документации группы; знак импорт/экспорт требует проверки |
| 90 | DC Temperature | S_WORD | 1 | S | 0.1 | °C | R | 2 | A* | |
| 91 | AC Temperature | S_WORD | 1 | S | 0.1 | °C | R | 2 | A* | |
| 96–97 | Total PV Production | U_DWORD | 2 | U | 0.1 | kWh | R | 2 | A* | LW=96, HW=97; порядок LW,HW |
| 108 | Daily PV Production | U_WORD | 1 | U | 0.1 | kWh | R | 2 | A* | Один регистр, не DWORD |
| 72–73 | Total Battery Charge | U_DWORD | 2 | U | 0.1 | kWh | R | 2 | A* | LW=72, HW=73 |
| 74–75 | Total Battery Discharge | U_DWORD | 2 | U | 0.1 | kWh | R | 2 | A* | LW=74, HW=75 |
| 248 | TOU Enable | U_WORD | 1 | U | 1 | — | R/W | 2 | B | Не читается в read-only драйвере |
| 250 | TOU Time 1 | U_WORD | 1 | U | 1 | HHMM | R/W | 2 | B | Не читается в read-only драйвере |

## DWORD (32-бит) — порядок слов

- Чтение двух holding-регистров FC 03, `count=2`.
- Сборка: `value = low_word + (high_word << 16)` (младшее слово по младшему адресу).
- Порядок LW,HW соответствует принятому в дереве Deye/Sunsynk, но **требует подтверждения на физическом устройстве**.

## Не включены в read-only драйвер (неподтверждённые для этой модели)

| Адрес | Параметр | Причина исключения |
|---:|---|---|
| 172, 175, 176 | Grid freq / CT / Load | Не в ключевой таблице SG02LP1 (151 — сетевое напряжение L2, не частота) |
| 60, 63, 74, 76, 78, 80, 84, 85 | Энергосчётчики | Старые адреса не соответствуют SG02LP1; заменены на 96–97 / 72–73 / 74–75 / 108 |
| 103, 104 | Fault codes | Формат/наличие для SG02LP1-AM3 не подтверждены |

### Подтверждено независимой реализацией (kbialek/deye-inverter-mqtt), но не входит в read-only драйвер

| Адрес | Параметр | Тип | Масштаб | Ед. | Примечание |
|---:|---|---|---:|---|---|
| 150 | Grid Voltage L1 | U_WORD | 0.1 | V | Сетевое напряжение L1 |
| 151 | Grid Voltage L2 | U_WORD | 0.1 | V | Сетевое напряжение L2 |
| 166 | Micro-inverter Power | U_WORD | 1 | W | Мощность микроинвертора |
| 182 | Battery Temperature | U_WORD | 0.1 | °C | Температура батареи |
| 250–255 | TOU Time 1–6 | U_WORD | 1 | HHMM | TOU не читается в read-only драйвере |
| 256–261 | TOU Power 1–6 | U_WORD | 1 | W | TOU не читается в read-only драйвере |
| 268–273 | TOU SOC 1–6 | U_WORD | 1 | % | TOU не читается в read-only драйвере |
| 274–279 | TOU Enable 1–6 | U_WORD | 1 | — | TOU не читается в read-only драйвере |

## Slave / Unit ID

- В драйвере по умолчанию: `slave_id = 1`.
- Фактический ID инвертора на шине RS485 **не подтверждён**.
- Перед первым опросом требуется определение ID (перебор 1–10, только FC 03, регистр 184 Battery SOC).

## Расчётные vs измеренные значения

| Поле | Происхождение | Примечание |
|---|---|---|
| pv1/2/3_power, pv*-voltage/current, battery_voltage/soc/power/current, grid_voltage | Измеренные | Прямое чтение регистров |
| p_sun | Расчётное | Сумма PV1+PV2+PV3 |
| p_batt | Измеренное | Регистр 190 (знак требует проверки) |
| p_grid_measured | Измеренное | Регистр 169, затем 167 (CT), с явным источником |
| p_grid_estimated | Расчётное | `p_load − p_sun − p_batt` — только если все входы есть, явно помечено |

> Расчётная мощность сетиnever не выдаётся за измеренную.
