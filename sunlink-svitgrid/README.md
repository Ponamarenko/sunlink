# SunLink

**SunLink** — Android-застосунок (Kotlin, Jetpack Compose, Material3) для моніторингу сонячної станції на базі інвертора **Deye** у стилі SvitGrid-дашборду: вузли PV/інвертор/батарея/мережа/дім, потоки енергії (Bezier-лінії з анімацією), статус з’єднання, екран відладки сирих даних.

Поточна версія застосунку: **v2.48.0** (versionCode 69).

## Можливості
- Дашборд з реальними даними (PV-рядки, батарея, мережа, навантаження) та відображенням `—` для відсутніх полів
- Історія вимірювань з backend (`/api/measurements/history`), явне маркування DEMO-джерела, стани offline/stale
- Визначення напрямку потоку мережі (імпорт/експорт/баланс) і анімовані потоки енергії
- Екран **«Відладка / Сирі дані»** (автооновлення ~5 с)
- Підтримка джерел: SvitGrid Server v2.0.0 (`/api/v1/*`, `:8000`) та `deye_gateway.py` (`/api/live/raw`, `/live`, `:8800`) з fallback-логікою

## Схема (спрощено)

```
[Інвертор Deye 192.168.25.15:502] -> [deye_gateway.py 192.168.25.117:8800]
    -> [MikroTik NAT :8800] -> [ha4a.pp.ua:8800] -> [Android SunLink]
```

## Структура репозиторію

```
.
├── app/                         # Android-застосунок (модуль Gradle)
│   └── src/main/java/com/sunlink/svitgrid/
│       ├── ui/                  # UI (dashboard, debug, навігація)
│       ├── viewmodel/           # бізнес-логіка, завантаження/мердж даних
│       ├── network/             # Retrofit/OkHttp, API
│       └── data/                # налаштування (SettingsRepository)
├── sunlink-svitgrid/
│   └── server/                  # Python gateway (Modbus TCP -> HTTP)
│       ├── deye_gateway.py
│       ├── requirements.txt
│       └── ...
├── INSTALL.md                   # розгортання з нуля
├── ARCHITECTURE.md              # архітектура, API, регістри, логіка Android
└── CHANGELOG.md                 # історія змін
```

## Швидкий старт

### Сервер/gateway
```bash
cd sunlink-svitgrid/server
pip install -r requirements.txt
nohup python3 deye_gateway.py > /tmp/deye_gateway.log 2>&1 &
curl -s http://127.0.0.1:8800/api/live/raw | head
```

### Android
```bash
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```
У застосунку: **Налаштування → адреса `ha4a.pp.ua`, порт `8800`** (або локальна адреса).

Для інтеграційних тестів — ізольований DEMO backend (окремий порт `8801`, тестова SQLite, без інвертора/HA/Redis):
```bash
cd sunlink-svitgrid/server
python3 demo_backend.py --port 8801 --db /tmp/sunlink-stage12-demo.db
# або повна перевірка цілого ланцюжка:
./scripts/verify_stage12_integration.sh
```

## Документація
- [INSTALL.md](INSTALL.md) — вимоги, інвертор, gateway, MikroTik NAT, збірка, налаштування застосунку
- [ARCHITECTURE.md](ARCHITECTURE.md) — схема, формат `/api/live/raw`, карта регістрів Deye, логіка fallback/парсингу/візуалізації
- [CHANGELOG.md](CHANGELOG.md) — історія версій та ключові віхи
- [docs/modbus-register-map.md](docs/modbus-register-map.md) — карта регістрів Deye SG02LP1 (уровень B, требує верифікації на пристрої)
- [docs/readonly-driver.md](docs/readonly-driver.md) — read-only Modbus-драйвер DeyeReadonlyAdapter (тільки FC 03)
- [docs/equipment.md](docs/equipment.md) — обладнання: інвертор, Ebyte NA111-E, мережа
- [docs/architecture.md](docs/architecture.md) — архітектура read-only драйвера (доповнення до кореневого ARCHITECTURE.md)
- [docs/stages/](docs/stages/) — звіти етапів розробки (стадія 2.5 аудит read-only драйвера, стадія 3 перший реальний read-only опрос, стадія 4 верифікація дисплея, стадія 5 Sunsynk дослідження, стадія 6 HA перехресна перевірка, стадія 7 Data Layer, стадія 8 Modbus інтеграція, стадія 9.2 верифікація офіційного протоколу, стадія 9.3 аудит топології, стадія 9.3.1 сверка доказательств)
- [docs/research/third-party-deye-mqtt-mcp-projects.md](docs/research/third-party-deye-mqtt-mcp-projects.md) — дослідження сторонніх Deye/MQTT/MCP проєктів (етап 3.5)
- [docs/stages/stage-7-data-layer.md](docs/stages/stage-7-data-layer.md) — базовий Data Layer SunPortal: моделі, нормалізація, SQLite-х історію (етап 7)
- [docs/stages/stage-8-modbus-integration.md](docs/stages/stage-8-modbus-integration.md) — Modbus опросчик та інтеграція Data Layer (етап 8)
- [docs/stages/stage-9.2-official-protocol-documentation.md](docs/stages/stage-9.2-official-protocol-documentation.md) — верифікація офіційної документації протоколу Deye та Ebyte NA111-E (етап 9.2)
- [docs/stages/stage-9.3-network-topology-and-master-audit.md](docs/stages/stage-9.3-network-topology-and-master-audit.md) — статичний аудит топології Deye ↔ Ebyte NA111-E ↔ Home Assistant, ролі Modbus master (етап 9.3)
- [docs/stages/stage-9.3.1-evidence-reconciliation.md](docs/stages/stage-9.3.1-evidence-reconciliation.md) — аудит доказательств, сверка топологии и восстановление документации (етап 9.3.1)
- [docs/stages/stage-10-gateway-data-layer-integration.md](docs/stages/stage-10-gateway-data-layer-integration.md) — интеграция deye_gateway.py с Data Layer: read-only production API, нормализация, SQLite, новые endpoints (етап 10)
- [docs/stages/stage-11-android-application.md](docs/stages/stage-11-android-application.md) — Android моніторинг-застосунок: Dashboard, History, Inverter Details, Diagnostics, Settings; виправлення контрактів, 32 тести, APK (етап 11)
- [docs/stages/stage-12-backend-android-integration.md](docs/stages/stage-12-backend-android-integration.md) — інтеграція Backend ↔ Android: реальні API-контракти, DEMO backend, E2E-тести, offline/stale, 159+49 тестів, APK (етап 12)
- [docs/stages/stage-13-git-security-and-release-verification.md](docs/stages/stage-13-git-security-and-release-verification.md) — очищення Git remote від PAT, безпечний SSH, release verification script (етап 13)
- [docs/stages/stage-14-android-apk-validation.md](docs/stages/stage-14-android-apk-validation.md) — аудит Android, виправлення налаштування backend, 62 тести, збірка APK, підготовка до встановлення (етап 14)
- [docs/stages/stage-16-real-data-pipeline.md](docs/stages/stage-16-real-data-pipeline.md) — єдиний інтерфейс джерел demo/mock/modbus, read-only FC03, карта регістрів, підготовка до фізичного тесту (етап 16)
- [docs/stages/stage-17-rs485-topology-safety-audit.md](docs/stages/stage-17-rs485-topology-safety-audit.md) — аудит топології RS485, статуси HA/Ebyte/Unit ID, безпечний план фізичного тесту (етап 17)
- [docs/stages/stage-18-single-fc03-test.md](docs/stages/stage-18-single-fc03-test.md) — статус BLOCKED: один FC03-запит не відправлено без підтвердження ексклюзивності RS485 Master (етап 18)
- [docs/stages/stage-19-github-history-and-single-fc03.md](docs/stages/stage-19-github-history-and-single-fc03.md) — історія обох репозитаріїв, один FC03 до reg 184: raw 0x0063 = 99 % SOC (етап 19)
- [docs/stages/stage-20-read-only-modbus-polling-engine.md](docs/stages/stage-20-read-only-modbus-polling-engine.md) — read-only Modbus TCP engine, лише reg 184, 212 backend-тестів, фізичний polling не запускався (етап 20)
- [docs/stages/stage-21-controlled-physical-polling-test.md](docs/stages/stage-21-controlled-physical-polling-test.md) — 3 контрольовані FC03-запити до reg 184: стабільні 95 % SOC (етап 21)
- [docs/stages/stage-22-real-telemetry-register-validation.md](docs/stages/stage-22-real-telemetry-register-validation.md) — повна карта телеметрії, рівні достовірності, план наступного фізичного тесту (етап 22)
- [docs/stages/stage-23-controlled-register-validation.md](docs/stages/stage-23-controlled-register-validation.md) — 5 контрольованих FC03: PV, V/I/P батареї, температури; гіпотеза scale reg 190 ×0.1 (етап 23)
- [docs/stages/stage-24-real-telemetry-expansion.md](docs/stages/stage-24-real-telemetry-expansion.md) — розширення телеметрії: backend/API/Android, 246 backend + 67 Android тестів, APK v2.31.0 (етап 24)
- [docs/stages/stage-25a-connectivity-diagnostic.md](docs/stages/stage-25a-connectivity-diagnostic.md) — діагностика DNS/TCP/HTTPS: причина CLEARTEXT-помилки, сертифікат, топологія (етап 25A)
- [docs/stages/stage-25b-direct-https.md](docs/stages/stage-25b-direct-https.md) — прямий HTTPS на backend, міграція URL Android, deployment package, реліз v2.32.0 (етап 25B)
- [docs/stages/stage-25b1-direct-https-9443.md](docs/stages/stage-25b1-direct-https-9443.md) — два listener: HTTP 8800 (LAN) + HTTPS 9443 (публічний), міграція 8443→9443 (етап 25B.1)
- [docs/stages/stage-27-real-inverter-integration.md](docs/stages/stage-27-real-inverter-integration.md) — єдиний read-only poller, аддитивні міграції SQLite, production pipeline без симуляції (етап 27)
- [docs/stages/stage-28-real-hardware-validation.md](docs/stages/stage-28-real-hardware-validation.md) — реальний FC03 reg 184 (94 % SOC), перевірка pipeline/API/WebSocket на фізичних даних (етап 28)
- [docs/stages/stage-30-dashboard-real-telemetry.md](docs/stages/stage-30-dashboard-real-telemetry.md) — WebSocket у Android, оновлений дашборд з ассетами `Icon/`, реальні REST/WS кадри (етап 30)
- [docs/stages/stage-31-premium-dashboard.md](docs/stages/stage-31-premium-dashboard.md) — Premium Design System,_light/dark/system, локалізація uk/en, нова енергетична схема (етап 31)
- [docs/stages/stage-32-hotfix-release-endpoint.md](docs/stages/stage-32-hotfix-release-endpoint.md) — виправлення застарілого public :8800 → https://sunlink.pp.ua:9443/, діагностика UPDATE/REST/WS (hotfix)
- [docs/stages/stage-33-production-deployment.md](docs/stages/stage-33-production-deployment.md) — розгорнуто HTTPS 9443 + реальний read-only poller (reg 184), перевірено REST/WS (етап 33)
- [docs/stages/stage-34-deye-register-map.md](docs/stages/stage-34-deye-register-map.md) — таблиця достовірності регістрів Deye, offline-тести декодерів, план безпечного FC03-тесту (етап 34)
- [docs/stages/stage-35a-production-safety-hardening.md](docs/stages/stage-35a-production-safety-hardening.md) — ізоляція DEMO/PRODUCTION, безпечна обробка Modbus-помилок, чесні поля /status (етап 35A)
- [docs/stages/stage-36-comprehensive-development.md](docs/stages/stage-36-comprehensive-development.md) — повна модель телеметрії, статистика, екрани «Телеметрія» та «Статистика» (етап 36)
- [docs/stages/stage-37-android-completion.md](docs/stages/stage-37-android-completion.md) — завершені екрани Battery/PV/Grid/Load/Energy Flow, швидкий доступ, 126 Android тестів (етап 37)
- [docs/stages/stage-38-android-quality.md](docs/stages/stage-38-android-quality.md) — локалізація, Compose UI-тести, стани завантаження/помилки, 130 Android тестів (етап 38)
- [docs/stages/stage-39-android-product-completion.md](docs/stages/stage-39-android-product-completion.md) — WebSocket оновлює повну телеметрію, сортування історії, 135 Android тестів (етап 39)
- [docs/stages/stage-40-maximum-telemetry.md](docs/stages/stage-40-maximum-telemetry.md) — 49 регістрів у карті, 68 полів телеметрії, екран інвертора, 345 backend + 139 Android тестів (етап 40)
- [docs/stages/stage-41-final-report.md](docs/stages/stage-41-final-report.md) —екран «Всі параметри», якість телеметрії, безпечний extended polling (етап 41)

## Перевірка релізу

```bash
./scripts/verify_sunlink_release.sh
```

Скрипт запускає backend та Android тести, збирає debug APK і перевіряє package/version/розмір.

## Завантаження APK

Кожен зібраний APK публікується в [GitHub Releases](https://github.com/Ponamarenko/sunlink/releases).
Поточний debug-реліз Stage 25B.1 (prerelease):
https://github.com/Ponamarenko/sunlink/releases/tag/android-v2.32.1-stage25b1

Повторна публікація нової збірки:

```bash
./scripts/publish_android_release.sh
```

Докладніше — [docs/stages/stage-15-android-acceptance-and-github-releases.md](docs/stages/stage-15-android-acceptance-and-github-releases.md).
Дані демо-backend симулюються; встановлення на реальний пристрій не перевірялось.

## Історія версій (коротко)

| Версія | Основне |
|---|---|
| v2.16.0 | Base URL з налаштувань (без жорстких fallback у конфігу) |
| v2.17.0 | Дашборд: поля API, Lottie-вузли, Bezier-потоки з анімацією |
| v2.18.0 | Виправлення підключення/статусу, додаткові серверні поля |
| v2.19.0 | SvitGrid v2.0.0 `/api/v1/*`, 2 інвертори, merge, LAN fallback `:8000` |
| v2.20.0 | Екран «Відладка / Сирі дані» (5 с) |
| v2.21.0 | Fallback на gateway `:8800`, парсинг вкладеного `/live` |
| v2.22.0 | PV у Вт/кВт, плавніші потоки, імпорт/експорт мережі, фікси gateway/Modbus, `/api/live/raw` |
| v2.23.0 | Етап 10: deye_gateway.py інтегровано з Data Layer — read-only production API, нормалізація через domain.normalizer, збереження в SQLite, endpoints /api/measurements/*, видалено запису Modbus |
| v2.23.0 | Етап 11: Android застосунок — Dashboard, History, Inverter Details, Diagnostics, Settings; тестування, APK, документування |
| v2.30.0 | Етап 11 (продовження): виправлено тести, контракт History (реальні імена регістрів), дефолт backend `ha4a.pp.ua:8800`, 32 тести, робочий APK |
| v2.30.0 | Етап 12: Backend ↔ Android інтеграція — реальні контракти, DEMO backend, 159 backend + 49 Android тестів, offline/recovery, APK |
| v2.31.0 | Етап 24: розширення реальної телеметрії (PV, V/I/P батареї, температури), кандидатські декодери, API/Android, 246 backend + 67 Android тестів, APK |
| v2.32.0 | Етап 25B: прямий HTTPS `https://sunlink.pp.ua:8443/`, міграція старих URL, діагностика з'єднання, backend TLS config, deployment package, 256 backend + 72 Android тестів |
| v2.32.1 | Етап 25B.1: HTTP 8800 (LAN) + HTTPS 9443 (публічний), міграція 8443→9443, два listener одного FastAPI app, 258 backend + Android тести, APK |
| v2.33.0 | Етап 30: WebSocket-клієнт у Android, дашборд з реальними станами та ассетами `Icon/`, 276 backend + 93 Android тестів, debug APK |
| v2.34.0 | Етап 31: Premium Design System, Dark/Light/System, uk/en, нова схема потоків, 276 backend + 105 Android тестів, debug APK |
| v2.35.0 | Hotfix: міграція збереженого public :8800 → 9443, заборона публічного HTTP, debug LAN 18800, діагностика UPDATE/REST/WS, 276 backend + 115 Android тестів |

## Відомі застереження
- Карта регістрів/масштаби Deye можуть відрізнятися для конкретної моделі/прошивки (потрібна верифікація значень: напруга мережі, температури, лічильники енергії).
- На деяких Deye/WiFi-логерах можливі попередження pymodbus про розсинхрон `transaction_id`.
- Зовнішня доступність (`ha4a.pp.ua:8800`) залежить від DNS, пробросу та firewall на роутері/хості.

## Ліцензія
MIT
