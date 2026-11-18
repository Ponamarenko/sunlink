# CHANGELOG

Ключові віхи розвитку SunLink (Android + deye_gateway). Формат — за основними подіями/версіями.

## [v2.23.0-prepare] — 2026-11-18 (верификация официальной документации протокола)
- **Этап 9.2:** документальная верификация протокола Deye SUN-12K-SG02LP1-EU-AM3 и Ebyte NA111-E
  - `docs/stages/stage-9.2-official-protocol-documentation.md` — отчёт этапа 9.2
  - Официальный мануал Deye (BManualSUN-5-12K-SG02LP1-EU-AM3, 2026-05-19, 55 стр.) проанализирован: **нет карты регистров, нет протокола Modbus, нет FC03 для параметров инвертора** — только физический RS485 pinout (Приложение I, стр. 48) и интерфейсы RS232/RS485/CAN
  - Официальный мануал Ebyte NA111-E (51 стр.) проанализирован: режимы TCPServer/Client/UDP/HTTPC/MQTTC, Modbus шлюзы (simple conversion, multi-host, storage, configurable), factory default IP 192.168.3.7:8887, baud 115200 8/None/1, до 6 клиентов, один Modbus master; FC03 разрешено, FC05/06/0F/10 — команды записи
  - **Не подтверждено:** MBAP header, RTU CRC, Unit ID/Slave ID передача, текущие IP/порт/baud (192.168.25.15:502, 9600 — не заводские)
  - Все 9 регистров проекта (90, 91, 183, 184, 186, 187, 188, 190, 191) имеют статус EXPERIMENTALLY_OBSERVED / PROVISIONAL — ни один не OFFICIAL_CONFIRMED
  - Физический инвертор не опрашивался; Ebyte не подключался; HA не останавливался; production backend, Android, .env, Redis, firewall не изменялись
  - Веб-поиск недоступен (auth 401); 분석 выполнен на основе двух официальных PDF

## [v2.23.0-prepare] — 2026-11-17 (подготовка, первый опрос, базовый Data Layer и Modbus интеграция)
- **Этап 8:** реализован Modbus polling service с интеграцией Data Layer
  - `server/services/modbus_polling_service.py` — цикл последовательного опроса регистров этапа 7 (FC 03 только)
  - `server/services/inverter_state.py` — состояние инвертора: online/offline/degraded/unknown, актуальность данных
  - `server/services/polling_config.py` — настройки интервалов, таймаутов, ретраев, бэкоффа
  - `server/modbus/client_interface.py` — интерфейс Modbus клиента (ABC)
  - `server/modbus/mock_client.py` — mock Modbus устройство для офлайн-тестирования (задержки, таймауты, TCP отказ, Modbus исключения, ошибки регистров, восстановление)
  - `server/demo_stage8.py` — демонстрационный сценарий с mock устройством и временной SQLite БД
  - `server/tests/test_polling_*.py` — 50 новых тестов (конфигурация, состояние, mock клиент, интеграционные сценарии опроса)
  - Все 116 тестов проходят (22 этапа 2 + 44 этапа 7 + 50 этапа 8)
  - Физический инвертор, Ebyte, Solarman, HA, production backend, Android не затрагиваются
  - Нет Modbus write операций; опрос только через mock устройство

## [v2.23.0-prepare] — 2026-11-17 (подготовка, первый опрос и базовый Data Layer)
- **Этап 7:** реализован базовый Data Layer SunPortal (независимый слой данных инвертора)
  - `server/domain/models.py` — типизированные модели измерений (`Measurement`, `NormalizedReading`, перечисления качества/достоверности/источника)
  - `server/domain/register_metadata.py` — метаданные регистров с уровнями достоверности (корректно: 9 рег., provisional: 190, 90, 91)
  - `server/domain/normalizer.py` — чистые функции нормализации (U_WORD/S_WORD, масштаб, температурный offset −100)
  - `server/storage/repository.py` — интерфейс `MeasurementRepository` (ABC)
  - `server/storage/sqlite_repository.py` — SQLite-хранение истории (версия схемы, индексы, транзакции, параметризованные SQL)
  - `server/demo_stage7.py` — демонстрационный сценарий с данными этапов 4 и 6 (временная SQLite БД)
  - `server/tests/` — 44 новых теста (модели, нормализатор, SQLite, API-контракт); все 66 тестов проходят (22 существующих + 44 новых)
  - Физический инвертор, production backend и Android не затрагиваются; новых Modbus-запросов нет

## [v2.23.0-prepare] — 2026-11-17 (подготовка и первый read-only опрос)
- **DeyeReadonlyAdapter:** отдельный read-only Modbus-драйвер для SUN-12K-SG02LP1-EU-AM3 (только FC 03 Read Holding Registers, нет методов записи)
- **Тесты:** 22 mock-теста (U/S_WORD декодирование, отрицательные значения, DWORD порядок LW,HW, отсутствие write-вызовов на клиенте) — пройдены
- **Документация:** docs/modbus-register-map.md, docs/readonly-driver.md, docs/equipment.md, docs/architecture.md, docs/stages/, docs/research/
- **Этап 3:** выполнен первый физический read-only опрос через readonly_diagnostic.py (один цикл, без polling; соединение закрыто)
  - Подтверждён Slave ID = 1 (reg 184 Battery SOC = 86%)
  - Все 19 регистров SG02LP1 прочитаны (measured, ошибок нет)
  - TCP 192.168.25.15:502 открыт, существующих соединений нет, другой Modbus Master не обнаружен
  - pymodbus 3.16.2, параметр device_id
  - Исправлен баг readonly_diagnostic.py (attribute last_successful_read адаптера, не снимка)
  - Водитель не интегрирован в backend; записей в инвертор не выполнялось

## [v2.22.0] — 2026-11 (поточний)
- **UI/дашборд:** потужність PV-рядків у **Вт/кВт**; плавніші Bezier-шляхи потоків, відображення частинок при малих потоках; підписи мережі **Експорт/Імпорт/Баланс** за знаком `p_grid`.
- **Gateway:** виправлення Modbus (хост `192.168.25.15`, `timeout`, використання `device_id` для pymodbus 3.x), корекції карти регістрів/масштабів, розрахунок/нормалізація `p_sun,p_load,p_batt,p_grid,p_string1-3`, плоский `/api/live/raw`, `/live` без тестових даних при недоступності Modbus (fallback зі статусом помилки).

## [v2.21.0]
- Додано fallback на **`deye_gateway.py :8800`** (`/live`, `/api/live/raw`) для отримання PV/батареї/мережі/навантаження.
- Парсинг вкладених полів `/live` (`data.{reg}.value`) та мапінг у плоскі ключі UI.

## [v2.20.0]
- Екран **«Відладка / Сирі дані»**: відображення сирих полів по інверторах, статистика/health, автооновлення ~5 с, маркери відсутніх значень.

## [v2.19.0]
- Перехід на API **SvitGrid Server v2.0.0** (`/api/v1/inverters`, `/api/v1/inverters/{id}/data`, `/system/stats`).
- Підтримка **2 інверторів** (INV001, INV002), мердж/агрегація даних.
- LAN fallback на `192.168.25.20:8000`.

## [v2.18.0]
- Виправлення підключення до API, відображення статусу з’єднання, спроби використати додаткові серверні поля.

## [v2.17.0]
- Переробка дашборду: відображення доступних полів API, Lottie-вузли, Bezier-лінії потоків з анімованими частинками.

## [v2.16.0]
- Прибрано жорстко закодовані локальні fallback-адреси API; base URL береться з налаштувань (`SettingsRepository`).

## Ранні ітерації (v2.12.0–v2.15.x)
- Послідовна перебудова UI в стилі SvitGrid: Material3, темна тема, 3D PNG/Lottie-вузли, потоки енергії, bottom sheet, drawer-навігація, локалізація (укр.), індикація `Є/Немає зв'язку`.

## Ключові технічні віхи (інфраструктура/інтеграція)
- **Діагностика API:** виявлено `404` для `/api/live/raw` та `/live` на SvitGrid v2.0.0 (`:8000`) і наявність робочих `/api/v1/*`; на `:8000` бракує полів `p_string*/pv*_voltage/current/p_batt/p_grid/p_load/grid_voltage_l1`.
- **Пошук/підключення інвертора:** визначено робочий Modbus TCP **`192.168.25.15:502`** (slave/device_id 1).
- **Gateway:** створено/доопрацьовано `deye_gateway.py` на **`:8800`** (слухає `0.0.0.0`) з ендпоінтами `/live` та `/api/live/raw`, плоскими полями для Android.
- **pymodbus:** перехід на сумісні виклики для **pymodbus 3.x** (`device_id` замість `slave_id/slave`), обробка помилок/швидкий вихід при недоступності Modbus; зафіксовано попередження про розсинхрон `transaction_id` на частині пристроїв.
- **Мережа:** налаштовано проброс на **MikroTik** `ether1 :8800 -> 192.168.25.117:8800`, публічний доступ через **`ha4a.pp.ua:8800`** (потребує коректного DNS/forward/firewall end-to-end).
- **Фінальна косметика UI:** стабілізація відображення реальних даних, підписів потоків/мережі та форматів потужності у v2.22.0.
