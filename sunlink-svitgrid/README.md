# SunLink

**SunLink** — Android-застосунок (Kotlin, Jetpack Compose, Material3) для моніторингу сонячної станції на базі інвертора **Deye** у стилі SvitGrid-дашборду: вузли PV/інвертор/батарея/мережа/дім, потоки енергії (Bezier-лінії з анімацією), статус з’єднання, екран відладки сирих даних.

Поточна версія застосунку: **v2.30.0** (versionCode 50).

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

## Перевірка релізу

```bash
./scripts/verify_sunlink_release.sh
```

Скрипт запускає backend та Android тести, збирає debug APK і перевіряє package/version/розмір.

## Завантаження APK

Кожен зібраний APK публікується в [GitHub Releases](https://github.com/Ponamarenko/sunlink/releases).
Поточний debug-реліз Stage 14 (prerelease):
https://github.com/Ponamarenko/sunlink/releases/tag/android-v2.30.0-stage14

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

## Відомі застереження
- Карта регістрів/масштаби Deye можуть відрізнятися для конкретної моделі/прошивки (потрібна верифікація значень: напруга мережі, температури, лічильники енергії).
- На деяких Deye/WiFi-логерах можливі попередження pymodbus про розсинхрон `transaction_id`.
- Зовнішня доступність (`ha4a.pp.ua:8800`) залежить від DNS, пробросу та firewall на роутері/хості.

## Ліцензія
MIT
