# Stage 20 — Read-only Modbus Polling Engine (Isolated Implementation)

**Статус: PASS (все критерии этапа выполнены).**
**HEAD до/после кода:** `dc8289cadd4a5b2646db7611c315124867d15656`.
**Physical Modbus polling was NOT executed in Stage 20.**

## 1. Архитектура

```
ModbusTcpTransport (MBAP, FC03, валидация ответа)
    -> ReadOnlyModbusClient (register-level API, без write)
        -> DeyePollingService (TelemetrySource, lifecycle/state/stats)
            -> ACTIVE_POLL_REGISTERS (только 184)
                -> TelemetrySnapshot (source=deye_modbus, stale flag)
                    -> SQLite Data Layer (repository)
                        -> существующий API -> Android DTO
```

Новые файлы:
- `modbus/modbus_tcp_transport.py` — MBAP-кадрирование и проверки;
- `modbus/read_only_modbus_client.py` — тонкая read-only обёртка;
- `services/deye_polling_service.py` — polling engine.

Изменены: `data_source_config.py` (`deye_modbus`, консервативный интервал 30 c),
`domain/register_metadata.py` (`ACTIVE_POLL_REGISTERS = {184}`),
`domain/telemetry.py` (`stale`, `persists_itself`), `deye_gateway.py` (нет
дублирующей записи, если источник сам пишет в Data Layer).

Контракт `TelemetrySource` (read/close) сохранён. Write-методов нигде нет.

## 2. Transport implementation

- **MBAP**: Transaction ID (инкрементальный), Protocol ID = 0, Length, Unit ID.
- **FC03** `Read Holding Registers`, `count=1` для активного регистра.
- **Одно соединение на запрос**: гарантирует предсказуемое закрытие сокета,
  отсутствие «зависших» соединений после timeout/disconnect и упрощает проверку
  полного ответа по MBAP Length. Нет persistent connection и нет retry.
- **Полное чтение ответа**: `_recv_exact()` циклически читает 7 байт MBAP, затем
  `Length-1` байт PDU — один `recv()` не обязан вернуть весь ответ.
- **Проверки ответа**: TID, Unit ID, Protocol ID, MBAP Length (2..512),
  Function Code (0x03 или exception 0x83), Byte Count == 2 × quantity,
  фактическая длина PDU, количество регистров.
- **Ошибки**: `ModbusTimeout`, `ModbusDisconnect`, `ModbusInvalidResponse`,
  `ModbusExceptionResponse` (с кодом exception). Сокет закрывается в `finally`
  и при успехе, и при ошибке.
- Сокет вводится через `socket_factory` — тесты подменяют его фиктивным.

Байты Stage 19 воспроизведены в тесте: запрос `000100000006010300B80001`,
ответ `0001000000050103020063`.

## 3. Polling lifecycle

Состояния: `disabled`, `connecting`, `online`, `degraded`, `offline`, `stopped`.
Инвертор **не считается online** только потому, что поток запущен: `online`
устанавливается после успешного чтения.

- `start()` — фоновый поток, состояние `connecting` до первого ответа;
- `stop(timeout)` — остановка потока, состояние `stopped`;
- `close()` — stop + закрытие транспорта;
- один владелец соединения; `threading.Lock` запрещает наложение циклов;
  если цикл уже идёт — возвращается последний добрый snapshot с `stale=True`;
- учёт `success_count`, `error_count`, `last_success_utc`, `last_error_utc`,
  `last_error` (без секретов); `get_status()` — минимальный статус источника.
- Интервал по умолчанию для физического polling — **30 c** (консервативный,
  настраивается через `SUNLINK_PHYSICAL_POLL_INTERVAL`). В Stage 20 polling
  не запускался.

## 4. Конфигурация и безопасное включение

- По умолчанию `SUNLINK_DATA_SOURCE=demo` (без сети).
- `deye_modbus` (новый явный режим) или алиас `modbus` требуют
  `SUNLINK_ENABLE_MODBUS=1`; без флага — понятная ошибка.
- Production `.env` не читается и не меняется; systemd/Redis/HA/firewall не
  трогались; production backend не переключался на Modbus.
- Нет автоматического fallback с Modbus на DEMO: при ошибке физического
  источника состояние `offline`/`error`, источник остаётся `deye_modbus`.

## 5. Активный и неподтверждённые регистры

| Регистр | Параметр | Тип | Scale | Ед. | Статус | В активном map |
|---:|---|---|---:|---|---|---|
| 184 | battery_soc | U_WORD | ×1 | % | CORROBORATED + реальный FC03 Stage 19 | **да** |
| 183 | battery_voltage | U_WORD | 0.01 | V | CORROBORATED | нет |
| 186/187/188 | PV power | S_WORD | 1 | W | CORROBORATED | нет |
| 191 | battery_current | S_WORD | 0.01 | A | CORROBORATED | нет |
| 190 | battery_power | S_WORD | 1 | W | PROVISIONAL | нет |
| 90/91 | temperatures | S_WORD | 0.1 −100 | °C | PROVISIONAL | нет |

`ACTIVE_POLL_REGISTERS = {184: ...}`. Исторические предположения не превращены
в VERIFIED. Неизвестные параметры дают `None`, не `0.0`.

Декодирование: `raw 0x0063 = 99` → `battery_soc = 99 %` (U_WORD × 1).

## 6. Ошибки, восстановление и stale data

Проверено offline: timeout, disconnect, неверный TID/Unit/Protocol/Length/FC/
Byte Count/register count, Modbus exception (0x83), короткий ответ, oversized response.
После ошибки состояние `offline`, источник остаётся `deye_modbus`, телеметрия не
выдумывается. Восстановление через следующий mock-ответ переводит в `online`.
При потере связи последнее достоверное значение сохраняется, но помечается
`stale=True` с исходным timestamp — UI видит, что данные устарели.

## 7. Data Layer и API

Интеграция через существующий `Measurement`/`SQLiteMeasurementRepository`:
source = `deye_modbus`, raw и normalized значение, timestamp, quality,
confidence. API-контракт не менялся: `/api/live/raw`, `/status`,
`/api/measurements/latest`, `/api/measurements/latest/{parameter}`,
`/api/measurements/history`, `/api/inverters`, Android DTO — совместимы.
Новый endpoint статуса источника не добавлялся: `/status` уже несёт `source`.
Write-endpointов (`/write`, `/set-register`, `/control`) нет.

Найдено и исправлено: при использовании polling-сервиса как `TelemetrySource`
один и тот же snapshot сохранялся дважды (сервисом и gateway). Добавлен флаг
`persists_itself` — владелец записи один.

## 8. Тестовые сценарии

`tests/test_stage20_polling_engine.py` — 33 теста, все offline:

Modbus transport (15): корректный FC03, полный ответ из нескольких TCP-чанков,
неверный TID/Unit/Protocol/MBAP Length/FC/Byte Count/register count,
Modbus exception, timeout, disconnect, malformed, закрытие сокета,
восстановление на новом сокете, отсутствие write-методов.
Decoder (4): `0x0063 → 99 %`, границы 0/100, некорректный raw, только reg 184.
Polling (10): успешный цикл, несколько циклов, stop/parallel guard, timeout,
stale, recovery, без DEMO fallback, SQLite/history, конфигурация и интервал.
API (3): live/status с `deye_modbus`, latest/history, отсутствие write-endpoint.

**Backend suite: 212 PASS** (`python3 -m unittest discover -s tests`).
Android-код не менялся → Android tests не запускались повторно, APK не собирался,
GitHub Release не создавался.

## 9. Ограничения и точный перечень НЕ проверенного на оборудовании

**Physical Modbus polling was NOT executed in Stage 20.** Не выполнялись:
TCP connect к `192.168.25.15`, ping/nmap/порт-скан, Modbus-запросы к инвертору,
проверка Unit ID, чтение дополнительных физических регистров, физический
polling. Разрешение Stage 19 было одноразовым и исчерпано.

Дополнительные ограничения:
- MBAP-формат assumed по стандарту; мануал Ebyte явно его не описывает;
- RTU CRC через TCP-шлюз недоступен;
- подтверждён физически только регистр 184; остальные — документ-claims;
- постоянный опрос на реальном инверторе требует отдельного разрешения.

## 10. Следующий безопасный шаг

После отдельного разрешения владельца — короткий контролируемый запуск polling
только для регистра 184 с фиксацией всех ответов и сверкой с дисплеем, затем
(по отдельным разрешениям) точечная верификация остальных регистров.
