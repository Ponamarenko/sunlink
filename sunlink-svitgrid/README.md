# SunLink

**SunLink** — Android-застосунок (Kotlin, Jetpack Compose, Material3) для моніторингу сонячної станції на базі інвертора **Deye** у стилі SvitGrid-дашборду: вузли PV/інвертор/батарея/мережа/дім, потоки енергії (Bezier-лінії з анімацією), статус з’єднання, екран відладки сирих даних.

Поточна версія застосунку: **v2.22.0** (versionCode 42).

## Можливості
- Дашборд з реальними даними (PV-рядки, батарея, мережа, навантаження) та відображенням `—` для відсутніх полів
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

## Документація
- [INSTALL.md](INSTALL.md) — вимоги, інвертор, gateway, MikroTik NAT, збірка, налаштування застосунку
- [ARCHITECTURE.md](ARCHITECTURE.md) — схема, формат `/api/live/raw`, карта регістрів Deye, логіка fallback/парсингу/візуалізації
- [CHANGELOG.md](CHANGELOG.md) — історія версій та ключові віхи

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

## Відомі застереження
- Карта регістрів/масштаби Deye можуть відрізнятися для конкретної моделі/прошивки (потрібна верифікація значень: напруга мережі, температури, лічильники енергії).
- На деяких Deye/WiFi-логерах можливі попередження pymodbus про розсинхрон `transaction_id`.
- Зовнішня доступність (`ha4a.pp.ua:8800`) залежить від DNS, пробросу та firewall на роутері/хості.

## Ліцензія
MIT
