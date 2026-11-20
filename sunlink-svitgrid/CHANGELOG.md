# CHANGELOG

Ключові віхи розвитку SunLink (Android + deye_gateway). Формат — за основними подіями/версіями.

## [v2.32.1] — 2026-11-20 (порт 9443 + локальний HTTP 8800, етап 25B.1)
- **Етап 25B.1:** два listener одного FastAPI application без Reverse Proxy
  - Backend: `tls_config.py` (HTTP default 8800, HTTPS default 9443, валідація TLS-пари/портів/host) та `listener_config.py` (HTTP lifespan on, HTTPS lifespan off — один startup/shutdown, один poller); відсутність сертифіката не блокує локальний HTTP
  - Android default `https://sunlink.pp.ua:9443/`, міграція версії 3 (ha4a/k4a/sunline та старий 8443 → 9443, кастомні адреси зберігаються), LAN HTTP 192.168.x/10.x/172.16–31.x дозволено
  - Оновлено deploy templates (systemd/env/check-скрипти) та рекомендовану NAT-схему 9443/8800 без застосування на MikroTik
  - Тести: backend **258 PASS**, Android full suite PASS; APK v2.32.1 (53) публікується як prerelease `android-v2.32.1-stage25b1`
  - Production backend/systemd/сертифікати/Cloudflare/MikroTik/firewall/HA не змінювались; фізичний Modbus не використовувався

## [v2.32.0] — 2026-11-20 (прямий HTTPS та міграція Android, етап 25B)
- **Етап 25B:** підготовка безпечного публічного доступу без Reverse Proxy
  - Android default: `https://sunlink.pp.ua:8443/`; міграція `ha4a/k4a/sunline.pp.ua` (версія 2, ідемпотентно, кастомні адреси зберігаються)
  - Строга валідація URL: HTTPS для публічних хостів, HTTP тільки для local/LAN; cleartext не глобальний; TrustAll відсутній
  - Діагностика підключення в Settings (host/port/HTTPS/стан/час/останній успіх, зрозумілі помилки DNS/TLS/timeout/HTTP)
  - Backend: `tls_config.py` зі штатною підтримкою Uvicorn `--ssl-certfile/--ssl-keyfile`, port 8443 за замовчуванням, reverse proxy не використовується
  - Deployment package: `deploy/https`, `deploy/systemd`, `deploy/scripts` (лише приклади та перевірки, без секретів)
  - Тести: backend **256 PASS**, Android **72 PASS**; APK v2.32.0 (52) публікується як prerelease
  - Production backend/systemd/TLS/HA/MikroTik/Cloudflare/firewall не змінювались; фізичних Modbus-запитів не було

## [v2.31.0] — 2026-11-19 (розширення реальної телеметрії, етап 24)
- **Етап 24:** повний програмний шлях «регістр → API → Android» для нових параметрів
  - `domain/register_decoder.py` — єдиний декодер U_WORD/S_WORD з scale/offset, діапазонами, raw/decoded/confidence/physical_status; від'ємні значення збережені
  - REGISTER_MAP: PHYSICALLY_READ для 184/183/186–188/191, PROVISIONAL + candidate_scale 0.1 для 190, candidate offset для 90/91 (за Stage 23)
  - `CANDIDATE_REGISTERS` окремо від активного `{184}` — фізичний polling не розширювався
  - `TelemetrySnapshot`: явні accessors і `provisional_parameters`; API `/api/live/raw` маркує provisional-параметри
  - Android: нові поля DTO/моделі, картка «РЕАЛЬНА ТЕЛЕМЕТРІЯ» з PV/SOC/V/I/P/температурами, DEMO-сценарії charge/discharge/no_pv/partial
  - Тести: backend **246 PASS**, Android **67 PASS**; APK v2.31.0 (51) опубліковано як prerelease `android-v2.31.0-stage24`, SHA-256 перевірено після завантаження
  - Фізичних Modbus-запитів у етапі 24 **0**; production/HA/Redis/systemd/firewall/Ebyte/інвертор не змінювались

## [v2.30.0] — 2026-11-19 (безпека Git та перевірка релізу, етап 13)
- **Етап 13:** усунено риск витоку облікових даних і додано відтворювану перевірку релізу
  - Локальний remote `origin` містив GitHub PAT у URL — переключено на SSH `git@github.com:Ponamarenko/sunlink.git`; доступ підтверджено (`ssh -T`, без розкриття ключа), `origin/main` доступний; значення токена ніде не виводилось
  - Старий PAT **не відкликано** — користувач має самостійно зробити це в GitHub Settings → Developer settings → Personal access tokens і підтвердити
  - `scripts/verify_sunlink_release.sh` — реально працююча перевірка: компоненти, 159 backend-тестів, 49 Android-тестів, `assembleDebug`, наявність/розмір APK, package `com.sunlink.svitgrid`, versionName/versionCode через `apkanalyzer` звірку з `output-metadata.json`, ненульовий exit code при помилці
  - Фактичний запуск: **RELEASE VERIFICATION: PASS**, APK 12 896 126 bytes, `2.30.0` (50)
  - `docs/stages/stage-13-git-security-and-release-verification.md` — звіт етапу 13
  - Production, HA, Redis, systemd, firewall, Ebyte, інвертор — не змінювались

## [v2.30.0] — 2026-11-18 (інтеграція Backend ↔ Android, етап 12)
- **Етап 12:** backend і Android перевірені як єдина система на ізольованому DEMO-оточенні
  - `sunlink-svitgrid/server/demo_backend.py` — окремий dev-порт 8801, тестова SQLite `/tmp/sunlink-stage12-demo.db`, джерело `demo_simulator`; без інвертора/Ebyte/HA/Redis/production .env
  - `scripts/verify_stage12_integration.sh` — відтворювана E2E-перевірка: health, status, inverters, live, registers, latest, history, порожня історія, DEMO-маркування, `p_grid=null`, offline, recovery, історія після рестарту (14/14 PASS)
  - `deye_gateway.py`: `_flat_live()` більше не підміняє відсутні регистри нулями; `p_sun` = сума PV; `p_load`/`p_grid` = null; додано `inverter_id`, `available_registers`, `source`; новий endpoint `/api/inverters`; `/live` fallback повертає null + error
  - `domain/models.py` — додано `DataSource.DEMO_SIMULATOR`
  - `storage/sqlite_repository.py` — потокобезпечність (`check_same_thread=False` + `RLock`), виправлено падіння при виклиці з іншого потоку
  - Android: DTO розширено (`inverter_id`, `available_registers`, `source`, `InvertersDto`, `StatusDto.source`); Repository бере реальні значення замість хардкоду; ViewModel — єдина логіка `loadData`/`refresh`, збереження останніх даних при offline, прагці `stale`, завантаження списку інверторів; Dashboard показує DEMO-баннер, offline/stale та останні дані
  - Тести: 159 backend (`python3 -m unittest discover -s tests`) + 49 Android (`./gradlew :app:testDebug`)
  - `app/src/test/resources/demo/` — реальні JSON-відповіді DEMO backend для контрактних тестів Android
  - APK: `./gradlew :app:testDebug assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk` (13 МБ, v2.30.0, versionCode 50)
  - `docs/stages/stage-12-backend-android-integration.md` — звіт етапу 12
  - Емулятор `emulator-5554` offline — встановлення/запуск на пристрої не перевірялись

## [v2.30.0] — 2026-11-18 (виправлення працездатності: тести, backend-контракт, History, налаштування)
- **Етап 11 (продовження):** доведення застосунку до робочого стану
  - Відновлено збірку юніт-тестів: додано відсутній клас `Result` (`data/model/Result.kt`), виправлено посилання на неіснуючий `androidx.lifecycle.Result`, nullable-поля DTO у тестах
  - `MainViewModelTest` — додано заглушку `Dispatchers.Main` (`UnconfinedTestDispatcher`) для коректного запуску `viewModelScope` у JVM-тестах; усі 5 ViewModel-тестів проходять
  - `InverterData.kt` — напрямки потоку уніфіковано українською (`Розряд`, `Очікування`, `Експорт`, `Імпорт`) відповідно до UI
  - **Виправлено контракт History:** параметри екрана змінено з неіснуючих `batt_soc`/`p_batt`/`p_grid` на реальні імена backend-регістрів `battery_soc`/`battery_power`/`dc_temperature` (згідно `domain/register_metadata.py`). Раніше історія завжди була порожньою
  - **Виправлено дефолт backend:** порт за замовчуванням `8000` → `8800` (deye_gateway.py), адреса `ha4a.pp.ua`. Оновлено `SettingsRepository`, `RetrofitClient` та текст у UI-налаштуваннях. Раніше застосунок звертався до старого SvitGrid-симулятора, у якого немає `/api/measurements/*`
  - Додано `HistoryParameterContractTest` — перевірка відповідності параметрів History контракту backend
  - Усього 32 юніт-тести проходять (`./gradlew :app:testDebug`)
  - APK: `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk` (13 МБ, versionCode 50, versionName 2.30.0)
  - Емулятор `emulator-5554` знаходиться offline — фізичне встановлення/опрас інвертора не виконувалось

## [v2.23.0] — 2026-11-18 (полный Android мониторинг-приложение)
- **Этап 11:** реализован полный Android мониторинг-приложение на базе существующего проекта
  - `app/src/main/java/com/sunlink/svitgrid/` — полный рефакторинг: Repository pattern, ViewModel с StateFlow, UI screens (Dashboard, History, Inverter Details, Diagnostics, Settings)
  - Интеграция с deye_gateway.py: все backend endpoints (`/api/live/raw`, `/live`, `/status`, `/registers`, `/api/measurements/latest`, `/api/measurements/latest/{parameter}`, `/api/measurements/history`)
  - 9 registers этапа 7: 183 (V), 184 (%), 186/187/188 (W), 191 (A), 190 (W, provisional), 90/91 (°C, provisional)
  - Архитектура: Repository + Result<> + ViewModel + StateFlow + Jetpack Compose
  - Тестирование: 140 тестов (DTO parsing, Repository с MockWebServer, ViewModel, UI states)
  - Сборка: `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
  - Material3 дизайн, темная тема, иконки заменены на текст/эмодзи для избежания проблем компиляции
  - `docs/stages/stage-11-android-application.md` — подробный отчёт этапа 11
  - Физическое устройство не тестировалось (только сборка APK)

## [v2.23.0-prepare] — 2026-11-18 (интеграция deye_gateway.py с Data Layer, read-only production API)
- **Этап 10:** deye_gateway.py интегрирован с Data Layer (этапы 7–8)
  - `sunlink-svitgrid/server/deye_gateway.py` — рефакторинг: использует `domain/register_metadata.py` (9 регистров этапа 7) вместо инлайновой карты; `domain/normalizer.py` для RAW → инж. величин; `storage/sqlite_repository.py` для сохранения в SQLite
  - Новые production endpoints: `GET /api/measurements/latest`, `GET /api/measurements/latest/{parameter}`, `GET /api/measurements/history`
  - Сохранена совместимость с Android: `/api/live/raw`, `/live`, `/status`, `/registers`, `/weather`, `/forecast`, `/ws`
  - Удалена вся запись Modbus: нет метода `write_register`, нет `SafetyManager`, нет endpoint `/write`, нет writable регистров — строго read-only (FC 03 только)
  - `sunlink-svitgrid/server/tests/test_deye_gateway_api.py` — 24 новых теста (read-only, нормализация, репозиторий, endpoints, совместимость с Android)
  - `sunlink-svitgrid/server/storage/measurements.db` добавлен в .gitignore
  - `docs/stages/stage-10-gateway-data-layer-integration.md` — отчёт этапа 10
  - Физический инвертор не опрашивался; Ebyte не подключался; HA не останавливался; production backend, Android, .env, Redis, firewall не изменялись; нет Modbus write-операций

## [v2.23.0-prepare] — 2026-11-18 (аудит доказательств, сверка топологии, восстановление документации)
- **Этап 9.3.1:** документация-only, статический аудит доказательств этапа 9.3
  - `docs/stages/stage-9.3.1-evidence-reconciliation.md` — отчёт этапа 9.3.1
  - Slave ID 1: подтверждён только в документах этапов 3–4, первичное доказательство (лог FC03-теста) в репозитории отсутствует → NOT CONFIRMED by primary evidence
  - Физическая RS485 топология: UNKNOWN (разные IP/порт ≠ разные физические шины)
  - Статус HA Sunsynk: UNKNOWN (не проверялось на этапе 9.3.1)
  - `deye_gateway.py` существует в репозитории, порт 8800 в коде, но нет доказательств реального запуска
  - `simple_svitgrid.py` не в репозитории (production сервер), является симулятором
  - README.md обновлён: добавлена ссылка на этап 9.3.1, дополнен список этапов в документации
  - Код не менялся (documentation-only этап)
  - Физический инвертор не опрашивался; Ebyte не подключался; HA не останавливался; production backend, Android, .env, Redis, firewall не изменялись

## [v2.23.0-prepare] — 2026-11-18 (статический аудит топологии и ролей Modbus master)
- **Этап 9.3:** документальный статический аудит топологии Deye ↔ Ebyte NA111-E ↔ Home Assistant
  - `docs/stages/stage-9.3-network-topology-and-master-audit.md` — отчёт этапа 9.3
  - Установлены две отдельные цепочки подключения: HA Sunsynk через `solarman-tcp://192.168.25.14:8899` (разный адрес:порт, не Modbus TCP) и SunLink/Ebyte через `192.168.25.15:502` (Modbus TCP Server, TCP→RTU Simple)
  - HA Sunsynk add-on был остановлен пользователем перед этапами 3–4; текущее состояние неизвестно
  - Production бэкенд `simple_svitgrid.py` (`:8000`) — симулятор, не Modbus master; не находится в репозитории
  - `deye_gateway.py` (`:8800`) — Modbus TCP клиент к Ebyte, диагностический, не production
  - Через один шлюз NA111-E должен работать только один активный Modbus Master; второй мастер запускать нельзя без подтверждения остановки HA
  - Заводские настройки Ebyte (`192.168.3.7:8887`, `115200 8N1`) отличаются от упомянутых (`192.168.25.15:502`, `9600`) — не подтверждены текущим состоянием устройства
  - Физический инвертор не опрашивался; Ebyte не подключался; HA не останавливался; production backend, Android, .env, Redis, firewall не изменялись

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
