# Stage 28 — Real hardware validation: Deye SUN-12K-SG02LP1-EU-AM3

**Итоговый статус: `PASS — PIPELINE`** (физический FC03, реальные данные в repository/API/WebSocket подтверждены; полная верификация всех регистров и синхронная сверка с дисплеем — NOT TESTED).

## 1. Git
Initial HEAD `c1134ec0f56a915fb45ff1bdd20f12d558d689de` = `origin/main`, source tree чистый. Итоговый commit создаётся поверх него.

## 2. Оборудование и конфигурация
- Инвертор: Deye SUN-12K-SG02LP1-EU-AM3.
- Адаптер: Ebyte NA111-E, RS485→LAN, Modbus TCP→RTU.
- Endpoint: `192.168.25.15:502`, Unit ID `1`.
- Запрошенный регистр: `184` (`0x00B8`), FC03, quantity 1.
- Сервер SunLink: Debian 13, `192.168.25.20` (только read-only SSH).

## 3. Карта потока данных (фактические классы и методы)

| Звено | Файл / класс / метод |
|---|---|
| Adaptation | Ebyte NA111-E (внешнее устройство) |
| Modbus TCP | `modbus/modbus_tcp_transport.py` → `ModbusTcpTransport.read_holding_registers()` |
| Read-only client | `modbus/read_only_modbus_client.py` → `ReadOnlyModbusClient.read_register()` |
| Poller | `services/deye_polling_service.py` → `DeyePollingService.read()` / `_run_cycle()` |
| Pipeline | `services/production_pipeline.py` → `build_production_poller()` / `attach_to_gateway()` |
| Repository | `storage/sqlite_repository.py` → `save_measurements_batch()` / `get_latest()` |
| API | `deye_gateway.py` → `/status`, `/api/live/raw`, `/api/measurements/latest` |
| WebSocket | `deye_gateway.py` → `/ws` → `send_text(json.dumps(message))` |
| Android | `LiveRawDto` / `InverterData` / Dashboard (Stage 24, без изменений) |

Physical pipeline не импортирует DEMO-модули; второй `attach_to_gateway` запрещён; write-методов нет.

## 4. TCP connectivity
Команды (с сервера и из среды OpenCode):
```
ip route get 192.168.25.15
timeout 4 bash -c 'cat < /dev/null > /dev/tcp/192.168.25.15/502'
```
Результат: `TCP_REACHABLE`, две проверки с сервера — exit 0 за 66 мс и 11 мс; две локальные — 11 мс и 12 мс. ARP-запись не отображалась, маршрут через `ens32` есть. Сеть и MikroTik не изменялись.

## 5. Modbus и физическое чтение регистра 184
Выполнено ровно три FC03-чтения (`/tmp/stage28_fc03_read.py`, транспорт из репозитория, без retry, один запрос на соединение):

| # | UTC | TX | RX | raw | decoded | время |
|---:|---|---|---|---:|---:|---:|
| 1 | 08:08:51 | `000100000006010300B80001` | `000100000005010302005E` | 0x005E=94 | 94 % | 221 мс |
| 2 | 08:08:56 | то же | то же | 94 | 94 % | 38.6 мс |
| 3 | 08:09:01 | то же | то же | 94 | 94 % | 39.8 мс |

TID=1 (новое соединение каждый раз), Unit=1, FC=03, адрес 184, quantity 1, Byte Count=2. Три значения идентичны — стабильность в рамках короткого теста.

Контракт регистра из `domain/register_metadata.py`: `battery_soc`, U_WORD, scale 1.0, unit `%`, formula `raw × 1`, без offset, unsigned. Декодер `domain/register_decoder.py`. Адрес используется напрямую (zero-based в коде, смещений нет).

### Происхождение статуса `PHYSICALLY_READ`
- Stage 19: реальный FC03 ответ `0x0063 = 99 %` (документированный HEX).
- Stage 21: три реальных FC03 ответа `0x005F = 95 %`.
- Stage 28 (этот отчёт): три реальных FC03 ответа `0x005E = 94 %`.
Это **не** mock и **не** симулятор; симулятор PASS в Stage 12/24 использует source `demo_simulator`.

## 6. Изолированный DeyePollingService
Запущен из `/tmp` с отдельной SQLite `/tmp/stage28-isolated.db`, `SUNLINK_ENABLE_MODBUS=1` только для процесса, реальным `ModbusTcpTransport` к `192.168.25.15:502`, без production-процессов:
- poller создан один, `get_status()`: `state=online`, `source=deye_modbus`, `success_count=2`, `error_count=0`, `active_registers=[184]`;
- `ONLINE` установлен только после настоящего ответа (94 %), `stale=false`;
- в БД сохранена запись: `parameter=battery_soc`, `raw=94`, `normalized=94.0`, `source=deye_modbus`, timestamp реального чтения;
- процесс и сокеты закрыты; production-службы не затронуты.

## 7. Изолированный API и WebSocket
На отдельном порту `127.0.0.1:18080` с тем же репозиторием:
- `/status` → 200, `source=deye_modbus`, `connection_status=true`;
- `/api/live/raw` → 200, `batt_soc=94.0`, остальные поля `null` (честно);
- `/api/measurements/latest` → 200, одна record `raw=94`, `normalized=94.0`, `source=deye_modbus`;
- WebSocket `/ws` → кадр `{"type":"live_data", ...}` с `battery_soc.raw_value=94`, `source=deye_modbus` (проверено отдельным WS-клиентом; данные воспроизведены из сохранённой физической записи — не новый mock).

Тестовый процесс завершён, порты 18080/19443 закрыты.

## 8. Reconnect и ошибки
На mock transport (без физического отключения адаптера) проверено: timeout/disconnect → `OFFLINE`/`CONNECTING`, bounded backoff 5–60 с, `reconnect_count`, stale-данные без подмены, повторный успех → `ONLINE` только после реального ответа; второй poller → `PollerAlreadyRunning`.

## 9. Тесты
Backend **276 PASS** (`python3 -m unittest discover -s tests`), включая 8 новых Stage 28 тестов: FC03-only, запрет write, неверный Unit ID, Modbus exception, timeout, закрытие клиента, отказ physical pipeline без opt-in, ошибка не маскируется под ONLINE/demo. Все Stage 12–27 тесты сохранены.

## 10. Статусы по пунктам

| Проверка | Статус |
|---|---|
| TCP адаптера | **PASS** |
| Modbus TCP ответ | **PASS** |
| Unit ID 1 | **PASS** |
| Регистр 184 физически прочитан | **PASS** |
| Назначение регистра | **PASS** (документация проекта) |
| Raw/decoded значения | **PASS** |
| DeyePollingService с физическими данными | **PASS** |
| Repository сохраняет физические данные | **PASS** |
| API отдаёт реальные данные | **PASS** |
| WebSocket отдаёт реальные данные | **PASS** (replay сохранённой физической записи) |
| Simulator не участвует | **PASS** |
| Single-poller protection | **PASS** |
| Reconnect (mock) | **PASS** |
| Сверка с дисплеем инвертора | **NOT TESTED** |
| Остальные регистры | **NOT TESTED** |
| Production deployment | **NOT TESTED / запрещено** |

## 11. Изменённые файлы
`modbus/mock_fc03_transport.py` (request_count для тестов), `tests/test_stage28_hardware_pipeline.py`, этот отчёт, README/CHANGELOG.

## 12. Ограничения
Не проверены дисплей, остальные регистры, долговременная стабильность и production deployment. Production `.env`, systemd, SQLite, HA, MikroTik, сертификаты — не изменялись. Write-команды и запись регистров не выполнялись.

## 13. Следующий этап
По отдельному разрешению — точечная физическая верификация остальных регистров (PV power 186–188, battery V/I 183/191, power 190 sign, temperatures 90/91) с синхронной сверкой с дисплеем, затем обновление карты и уровня достоверности.
