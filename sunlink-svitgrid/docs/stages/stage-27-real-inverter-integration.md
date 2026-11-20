# Stage 27 — Real Deye inverter integration preparation

**Статус: PASS (программная подготовка); физический Modbus НЕ запускался, production НЕ изменялся.**

## 1. Аудит текущего кода и истории

- Репозиторий `Ponamarenko/sunlink`, HEAD `c05fdb98...`, source tree чистый.
- В репозитории **нет** `simulation_thread` — симуляция живёт только в production `simple_svitgrid.py` (не в Git). Historically: SunPortal/SunLink legacy backend использует mock-данные INV001.
- Production (read-only аудит Stage 26): `simple_svitgrid.py:332` запускает `simulation_thread`; `deye_handler.py`/`inverter_handler.start()` встречаются только в неактивных `full_main.py`/`svitgrid_server.py`; активный `deye_gateway.py` на production отсутствует. Реальный Modbus-опросчик не запущен.
- RS485-LAN адаптер: Ebyte NA111-E, `192.168.25.15:502`, Modbus TCP → RTU (Simple), Unit ID 1, UART 9600 8N1 — по документации проекта; физическая доступность **не проверялась** (нет разрешения на опрос).
- Регистры: активная физическая карта `{184}`; остальные — CORROBORATED/PROVISIONAL; декодеры U_WORD/S_WORD/offset в `domain/register_decoder.py` и `normalizer.py`; расширенный decoding Stage 24 проверен тестами.

## 2. Единый production pipeline

```
Deye -> RS485 -> Ebyte -> ModbusTcpTransport (FC03, one request per connection)
     -> ReadOnlyModbusClient -> DeyePollingService (single owner, bounded backoff)
     -> ACTIVE_POLL_REGISTERS {184} -> TelemetrySnapshot (source=deye_modbus)
     -> SQLiteMeasurementRepository -> /api/* + /ws -> Android
```

Новый `services/production_pipeline.py`:
- `build_production_poller(config, repository)` — только при `source=deye_modbus` и `SUNLINK_ENABLE_MODBUS=1`; физический адрес обязателен; profile `stage7`.
- `attach_to_gateway(gateway, poller)` — подключает poller к существующему FastAPI app; второй source запрещён.
- Модули симуляции (`demo_backend`, `demo_stage7/8`, `modbus.mock_client`) **не импортируются и не запускаются** production pipeline. DEMO остаётся только для тестов/локального запуска.

## 3. Единственный poller, состояние, reconnect

`services/deye_polling_service.py` дополнен:
- реестр активных poller по `inverter_id` → второй `start()` выбрасывает `PollerAlreadyRunning`;
- bounded reconnect backoff (5 c → максимум 60 c), сброс после успеха;
- `PollingState.CONNECTING` при повторной попытке, `ONLINE` только после реального ответа;
- `reconnect_count`, `last_error`, `last_success` в `get_status()`;
- корректный `stop()` с deregistration и `close()`;
- один цикл за раз (`_cycle_lock`), без параллельного опроса;
- stale/offline без подмены данными.

## 4. SQLite без потери данных

Новый `storage/migrations.py`:
- только аддитивные миграции, `user_version` никогда не понижается;
- перед миграцией создаётся копия БД `.bak.<timestamp>`;
- схема v1 уже содержит все поля Data Layer.

`SQLiteMeasurementRepository._init_schema()` больше не выполняет `PRAGMA user_version = 1` на существующей БД (риск downgrade); для новой БД версия фиксируется, для более старой вызывается аддитивная миграция. Тесты подтверждают сохранение строк и версии 5 после попытки миграции.

## 5. API и WebSocket

Существующие endpoint'ы `/status`, `/api/inverters`, `/api/live/raw`, `/api/measurements/latest`, `/api/measurements/history` и WebSocket `/ws` работают с единым `TelemetrySnapshot`. Контракт не менялся; Android DTO из Stage 24 совместим. DEMO/MOCK источники остаются явно помеченными (`source`), реальный — `deye_modbus`.

## 6. Android

Проверено: default `https://sunlink.pp.ua:9443/`, миграция 8443→9443, отображение реальной телеметрии (PV, battery V/I/P, temperatures), provisional-маркировка, negative values, stale/offline. Изменений Android-кода в Stage 27 **нет** — новый APK не собирался и не публиковался.

## 7. Тесты

Backend **268 PASS** (`python3 -m unittest discover -s tests`), включая 10 новых Stage 27 тестов:
- единственный poller + повторный старт после stop;
- backoff/reconnect/stale;
- аддитивные миграции, backup и отсутствие downgrade;
- отказ production pipeline для demo/без флага, single attach;
- API + WebSocket `live_data` через telemetry source;
- все предыдущие Stage 12–26 тесты сохранены.

Android: 74 PASS (без изменений), сборка APK проверена в Stage 26.

## 8. Deployment и rollback

Подготовлено в `/tmp/sunlink-stage26-bundle` (Stage 26 gate) и дополнено этим pipeline:
1. Backup (systemd units + env + БД с SHA-256).
2. Проверка сертификата и конфигурации.
3. Доставка кода в **новый** каталог `/opt/sunlink/backend-stage25b1` (без перезаписи legacy-файлов).
4. Установка unit с одним процессом и одним poller.
5. Переключение служб только после всех preflight-проверок.
6. Проверка HTTP 8800 / HTTPS 9443 / одного poller.
7. При FAIL — автоматический rollback без удаления SQLite.

Полные команды — `docs/DEPLOYMENT_PLAN.md` в bundle. Deployment Stage 26 **не выполнялся**.

## 9. Риски и NEXT

- Реальная доступность Ebyte `192.168.25.15:502`, Unit ID 1 и baud 9600 8N1 **не подтверждена физически**.
- Только регистр 184 PHYSICALLY_READ; остальные требуют отдельных разрешённых FC03-тестов.
- Production-зависимости отличаются от `requirements.txt` (fastapi 0.162/uvicorn 0.54, нет pymodbus) — нужна проверка совместимости при deployment.
- Реальная SQLite application-БД на production не найдена — путь и миграция должны быть подтверждены до включения физического polling.
- Симулятор в production `simple_svitgrid.py` должен быть заменён новым pipeline только после отдельного согласования.

Следующий безопасный шаг: по разрешению владельца — физическая проверка доступности адаптера и одного FC03 к регистру 184 через новый pipeline, затем точечная верификация остальных регистров.

Остановился: deployment, выпуск сертификата, перезапуск служб и Modbus-запросы не выполнялись.
