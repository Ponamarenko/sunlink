# Read-Only Modbus Driver — DeyeReadonlyAdapter

**Файл:** `sunlink-svitgrid/server/deye_readonly_adapter.py`  
**Статус:** реализован, протестирован (22 mock-теста), не интегрирован в рабочий backend.  
**Режим:** строго read-only (только Modbus FC 03 Read Holding Registers).

## Назначение

Одиночный (read-only) опрос инвертора Deye SUN-12K-SG02LP1-EU-AM3 через шлюз Ebyte NA111-E (Modbus TCP :502). Предназначен для диагностического получения реальных данных и последующей проверки карты регистров перед возможной интеграцией.

## Архитектура безопасности (read-only по дизайну)

| Граница | Реализация |
|---|---|
| Function codes | Исключительно FC 03 (`read_holding_registers`) |
| Write methods | Отсутствуют на уровне класса (`write_register`, `write_registers`, `write_coil`, `write_coils` не определены) |
| TOU / batería / лимиты / аварийные команды | Не реализованы |
| FC 05 / 06 / 15 / 16 | Не могут быть вызваны из этого модуля |
| Автоподключение при импорте | Нет (`connect()` вызывается явно) |
| Фоновый polling | Нет (нет потоков, таймеров, циклов) |
| Изменение конфигурации Ebyte / инвертора | Нет |

## Публичный API класса

```python
adapter = DeyeReadonlyAdapter(host="192.168.25.15", port=502, slave_id=1, timeout=2.0)
ok = adapter.connect()           # открыть Modbus TCP соединение
snapshot = adapter.read_all()    # один цикл чтения всех регистров
adapter.disconnect()             # закрыть соединение
```

- `adapter.connected` — состояние соединения.
- `adapter.last_successful_read` — ISO-время последнего успешного чтения.
- `adapter.last_error` — последняя ошибка (или None).

`read_all()` возвращает `AdapterSnapshot`:
- `timestamp` — начало цикла.
- `connection_ok` — флаг соединения.
- `error` — текст ошибки или None.
- `registers` — `dict[name -> RegisterValue]` для каждого регистра.
- `summary` — агрегированные поля (`p_sun`, `p_batt`, `p_grid_measured`, `p_grid_estimated`, temperatury, энергия и т.д.) с явной пометкой источника (`measured` / `calculated` / `unconfirmed`).

## Карта регистров (встроенная)

Опирается на карту SG02LP1 (уровень B). Полный перечень с адресами, типами, масштабами и источниками — в `docs/modbus-register-map.md`.

Неподтверждённые регистры (например, 182 battery_temp) не попадают в `REGISTER_MAP` и при попытке чтения возвращают `source=unconfirmed` + ошибку, не выдаваясь как достоверные.

## Диагностический запуск

```bash
cd sunlink-svitgrid/server
python3 readonly_diagnostic.py --host 192.168.25.15 --port 502 --slave-id 1 --timeout 2
```

Один цикл чтения, соединение закрывается по завершении, фоновых процессов не остаётся.

## Требуется перед первым реальным подключением

1. Подтвердить, что другой Modbus-мастер к тому же Ebyte (например, клиент Home Assistant) остановлен — иначе коллизии кадров на RS485.
2. Определить Slave/Unit ID инвертора (по умолчанию 1; при необходимости перебор 1–10, только FC 03, регистр 184).
3. Сверять полученные значения с экраном инвертора (адреса, порядок слов DWORD, знаки battery/grid).
4. Не интегрировать драйвер в работающий `svitgrid.service` без отдельного разрешения.

## Тесты

`sunlink-svitgrid/server/tests/test_deye_readonly_adapter.py` — 22 mock-теста (без реального подключения):

- absence of write methods, U/S_WORD decoding, negative values, scaling, DWORD LW,HW order, PV1–3, Grid ×10, CT L1, Battery SOC, timeout/unavailable-device handling, no write calls on client.

## Уровни уверенности в документации

- **подтверждено тестами:** логика декодирования/масштабирования/сборки DWORD, отсутствие write-методов.
- **подтверждено реальным оборудованием:** ещё не проведено (ожидает этапа 3).
- **основано на сторонней карте регистров:** адреса/типы/масштабы SG02LP1 (уровень B, источник MarkLabs).
- **предположение / требует проверки:** Slave ID, порядок слов DWORD, знаки мощности/тока, различия AM2 vs AM3, влияние прошивки.
