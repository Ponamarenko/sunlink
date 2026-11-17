# Этап 2 — Deye SG02LP1 READ-ONLY Modbus драйвер (реализация)

**Статус:** реализован, протестирован, **не подключён к production**, не запущен в svitgrid.service.  
**Режим:** строго read-only (только FC 03 Read Holding Registers). Никаких функций записи в инвертор не существует и не может быть вызвано.  
**Физический инвертор не трогал:** нет подключений к 192.168.25.15:502, нет реальных Modbus-запросов, нет polling.  
**Ничего не коммитилось и не пушилось.**

## 1. Созданные файлы (только новые, существующие не менялись)

| Путь | Назначение |
|---|---|
| `sunlink-svitgrid/server/deye_readonly_adapter.py` | Отдельный read-only Modbus-драйвер (DeyeReadonlyAdapter) |
| `sunlink-svitgrid/server/tests/__init__.py` | Пакет тестов |
| `sunlink-svitgrid/server/tests/test_deye_readonly_adapter.py` | 22 автоматических mock-теста |
| `DEYE_READONLY_ADAPTER_STAGE2.md` | Данный отчёт |

Не изменялись: `deye_gateway.py`, `config.yaml`, `simple_svitgrid.py`, `svitgrid.service`, `sunlink.service`, `.env`, Android, Home Assistant, Ebyte, файрвол.

## 2. Краткое описание драйвера (`deye_readonly_adapter.py`)

**Класс `DeyeReadonlyAdapter`** — только методы чтения:
- `connect()` / `disconnect()` — открытие/закрытие Modbus TCP соединения.
- `read_register(address)` — чтение одного holding-регистра (FC 03, count=1).
- `read_dword_pair(low, high)` — чтение пары регистров как одного 32-битного значения (LW,HW).
- `read_all()` — единый снимок всех зарегистрированных регистров + сводка.
- Свойства: `connected`, `last_successful_read`, `last_error`.

**Явно отсутствуют (по дизайну):** `write_register`, `write_registers`, `write_coil`, `write_coils`, любые TOU/батарейные/лимитные/аварийные команды. Нет ни одного пути выполнения Modbus Write.

**Происхождение данных (каждое значение помечено):**
- `measured` — прочитано напрямую из подтверждённого регистра;
- `calculated` — получено расчётом (сумма PV, p_grid оценка);
- `unconfirmed` — адрес не подтверждён для этого инвертора.

**Не отдаёт расчётную мощность сети за измеренную:** `p_grid` берётся только из подтверждённого регистра 169 (или CT 167), с явным полем `p_grid_source`; балансовый расчёт `p_load - p_sun - p_batt` идёт отдельно под меткой `p_grid_estimated` и только при наличии всех входов.

**Верифицированная карта SG02LP1 (внедрённая):**
- PV: 186/187/188 power (U_WORD, 1 W), 109-114 V/I (U_WORD, 0.1 V/A) — 3-й MPPT (10/12K).
- Батарея: 183 V (0.01), 184 SOC (1 %), 190 P (S_WORD, 1 W), 191 I (S_WORD, 0.01 A).
- Сеть: 167 CT L1 (S_WORD, 1 W), 169 Total Grid Power (S_WORD, ×10 W).
- Температуры: 90 DC (S_WORD, 0.1 °C), 91 AC (S_WORD, 0.1 °C).
- Энергия (DWORD, LW,HW): 96-97 Total PV, 72-73 Total Battery Charge, 74-75 Total Battery Discharge (все U_DWORD, 0.1 kWh); 108 Daily PV (U_WORD, 0.1 kWh).

**Не включены (неподтверждённые / ошибочные в старом драйвере):** 182 battery_temp (нет в карте SG02LP1), 166 (был S_WORD micro-inverter — неверный тип/семантика), 90 был U_WORD (исправлено на S_WORD), 151/172/175/176, TOU 248-279 (не реализованы), старые энергоадреса 60/63/74/76/78/80/84/85 (заменены на 96-97/72-73/74-75/108), 103/104 faults.

## 3. Результаты тестирования

Запуск: `cd sunlink-svitgrid/server && python3 -m unittest tests.test_deye_readonly_adapter -v`

```
Ran 22 tests in 0.031s
OK
```

Все 22 теста пройдены:

| Тест | Что проверяет |
|---|---|
| `test_no_write_methods_exist` | Класс не содержит методов записи |
| `test_defaults` | Начальное состояние: не подключён, нет ошибок |
| `test_u_word_voltage_scale` | U_WORD + масштаб 0.1 (напряжение) |
| `test_u_word_current_scale` | U_WORD + масштаб 0.1 (ток) |
| `test_battery_voltage_scale` | U_WORD + масштаб 0.01 |
| `test_battery_soc` | U_WORD масштаб 1 |
| `test_s_word_positive` | S_WORD положительное |
| `test_grid_power_x10` | S_WORD ×10 (сеть) |
| `test_ct_l1_power` | S_WORD отрицательное/положительное CT |
| `test_negative_grid_power` | S_WORD >32767 → отрицательное (импорт/экспорт) |
| `test_dword_lw_hw_order` | DWORD = LW + (HW<<16), порядок слов |
| `test_snapshot_structure` | Структура read_all() |
| `test_pv_present_and_calculated_p_sun` | PV1/2/3 + расчёт p_sun |
| `test_battery_soc_present` | SOC в сводке |
| `test_grid_power_measured_from_169` | p_grid из регистра 169, источник помечен |
| `test_grid_power_falls_to_ct_when_169_absent` | p_grid из CT 167 при отсутствии 169 |
| `test_no_estimated_grid_without_load` | Нет за silent-оценки без нагрузки |
| `test_last_successful_read_set` | Время последнего успешного чтения |
| `test_unconfirmed_register_not_in_map` | 182 не в карте, ошибка, unconfirmed |
| `test_modbus_error_marks_connection_down` | Ошибка чтения → connection_down |
| `test_unavailable_device_read_all` | Недоступное устройство → connection_ok=False |
| `test_read_all_only_calls_read` | Клиент вызывает только connect + read_holding_registers, нет write-вызовов |

### Подтверждение отсутствия Modbus Write
- В классе `DeyeReadonlyAdapter` **нет** методов `write_register`/`write_registers`/`write_coil`/`write_coils` (проверено тестом `test_no_write_methods_exist`).
- Тест `test_read_all_only_calls_read` фиксирует, что моковый клиент вызывал только `connect` и `read_holding_registers`; ни один `write_*` вызов не зафиксирован.
- Функциональные коды 05/06/15/16 не реализованы и не могут быть вызваны из этого модуля.

## 4. Diff всех изменений

Поскольку добавлены только новые (untracked) файлы, `git diff` пуст относительно tracked-файлов. Полное содержимое новых файлов — это и есть diff. Краткий перечень:

**Новые файлы:**
- `sunlink-svitgrid/server/deye_readonly_adapter.py` — драйвер (~290 строк).
- `sunlink-svitgrid/server/tests/__init__.py` — пустой init пакета.
- `sunlink-svitgrid/server/tests/test_deye_readonly_adapter.py` — 22 теста.

**Изменений в существующих файлах:** нет.  
**Изменений в `simple_svitgrid.py`, `svitgrid.service`, `sunlink.service`, `.env`, Android, HA:** нет.

## 5. Инструкция по будущему безопасному подключению к Ebyte NA111-E

(Только при отдельном разрешении, после верификации карты на устройстве.)

1. **Подтвердить Unit/Slave ID** инвертора на шине RS485 (в драйвере по умолчанию `slave_id=1`, это предположение). Неверный ID при чтении не повреждает устройство, но даёт отсутствие ответов.
2. **Подтвердить на реальном устройстве** (read-only прогон): адреса 184,183,190,191,186-188,109-114,150,169,90,91,108,96-97,72-73,74-75; sanity-значения vs экран инвертора; порядок слов DWORD; знак network/battery.
3. **Развернуть драйвер отдельным процессом** (не в `svitgrid.service` на этом этапе), на хосте с сетевой доступностью до `192.168.25.15:502`. Типаж: один экземпляр драйвера = один активный Modbus-клиент к шлюзу.
4. **Таймауты:** `timeout=2.0 с`, повтор с backoff, защита от наложения запросов (один опрос за раз).
5. **Read-only по умолчанию:** запись в инвертор закрыта на уровне кода (нет методов записи).任何 попытка подключить запись требует нового этапа и отдельного разрешения.

## 6. План переключения Modbus Master с учётом существующего клиента Home Assistant

Ebyte NA111-E — **TCP Server, Modbus TCP→RTU Open/Simple, RS485 9600 8N1**. Через один шлюз на шине RS485 в каждый момент времени должен работать **один активный Modbus Master**. Параллельный опрос драйвером SunLink и клиентом Home Assistant вызовет коллизии кадров и таймауты.

Безопасная схема:

1. **Единая точка конфигурации** для всех клиентов: `host=192.168.25.15`, `port=502`, `slave_id` (подтверждён), `timeout`.
2. **Один активный мастер в момент времени:** либо драйвер SunLink, либо клиент Home Assistant. Перед переключением корректно закрывать соединение у текущего мастера (`disconnect()`), затем запускать другого. Либо организовать общую блокировку/очередь доступа к порту 502.
3. **Read-only по умолчанию для обоих клиентов.** Любая запись (TOU, лимиты, режимы) — только после верификации адресов и отдельного разрешения.
4. **Не менять конфигурацию Ebyte** (TCP Server, 502, 9600 8N1) без отдельного плана; при переключении мастера настройки шлюза не трогать.
5. **Мониторинг:** у каждого клиента — онлайн/ошибки/последняя причина недоступности/`last_successful_read`; бэкап конфигураций перед переключением.
6. **Контракт Android не меняется:** источник данных внутри `simple_svitgrid.py` (`:8000`) заменяется на адаптер с сохранением всех `/api/v1/*` эндпоинтов и ключей; сам драйвер `:8800` может оставаться диагностическим read-only источником.

## 7. Ограничения текущего этапа (выполнено)

- Не подключался к `192.168.25.15:502` ✓
- Не отправлялись реальные Modbus-запросы ✓
- Не запускался polling ✓
- Не менялись настройки инвертора/Ebyte ✓
- Не запускался новый драйвер в production ✓
- Не останавливались/перезапускались службы ✓
- Не менялся Home Assistant/Android ✓
- Не было git commit/push ✓
- Не редактировались исходники существующих файлов ✓

Дальнейшие действия (подключение к устройству, интеграция в `simple_svitgrid.py`, переключение мастера) требуют вашего отдельного разрешения.
