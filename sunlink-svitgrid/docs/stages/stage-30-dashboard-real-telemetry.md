# Stage 30 — Dashboard + реальна Android-телеметрія

**Статус: PASS (код, тести, збірка, реліз). Встановлення на пристрій/емулятор і скріншот живого екрана — NOT VERIFIED (пристрій недоступний).**

## 1. Commit

- Початковий HEAD: `8d716b803d6335da270c3973c88de83d2bfa2ba6` (= `origin/main`, «Add files via upload», папка `Icon/`).
- Комміт коду Stage 30: `e36dbf35d95ab2a68cc88c36187202e83c4a2ace` — `Stage 30: integrate dashboard assets and real telemetry`, push без force, `HEAD = origin/main`.
- Цей звіт додається наступним docs-коммітом; APK у Git не додавався.

## 2. Ассети (папка `Icon/`, без ліцензійних обмежень у MD)

MD-файли `инвертор.md` і `столб.md` містять лише опис Lottie-анімацій і JSON-код; ліцензій/заборон на використання немає.

### Lottie (12, скопійовані в `app/src/main/assets/lottie/icon_*.json`)
`icon_battery_charge_discharge.json`, `icon_deye_12kw_inverter.json`, `icon_house_weather_all_in_one.json`, `icon_house_weather_clouds.json`, `icon_house_weather_fog.json`, `icon_house_weather_rain.json`, `icon_house_weather_snow.json`, `icon_house_weather_sun.json`, `icon_isometric_pole_sparks.json`, `icon_red_generator_animation.json`, `icon_solar_house_animation.json`, `icon_solar_panel_white_frame.json`.

Усі 12 валідні JSON (перевірено `json.load`), у APK присутні всі 12.

### WebP (6, 512 px, `res/drawable-nodpi/sunlink_*.webp`)
| Файл | Джерело | Розмір у APK |
|---|---|---:|
| `sunlink_battery.webp` | `батарейка.jpg` | 5 532 B |
| `sunlink_grid_pole.webp` | `столб.jpg` | 6 328 B |
| `sunlink_house.webp` | `дом.jpg` | 33 284 B |
| `sunlink_house_weather.webp` | `Дома.jpg` | 38 876 B |
| `sunlink_inverter.webp` | `инвертор.jpg` | 7 754 B |
| `sunlink_solar_panel.webp` | `панель.jpg` | 15 230 B |

Походження JPG: 1024×1024 або 1408×768; оптимізовано до WebP quality 82. Усі зображення виводяться через `IconImage(..., ContentScale.Fit)` — без розтягування.

## 3. Реалізовані елементи Dashboard

- Логотип/назва **SunLink** + модель `Deye SUN-12K-SG02LP1-EU-AM3` у шапці.
- П'ять станів з'єднання: **Loading / Online / Offline / Stale data / Connection error** (`ConnectionPresentation`, кольорова індикація + текст).
- Стан інвертора (онлайн/офлайн/очікування/немає даних).
- SOC батареї, PV / Battery / Grid / Load — окремі картки з відповідними WebP; значення показується тільки коли отримано, інакше «Немає даних».
- Напруги, струми, потужності й температури — у картці реальної телеметрії; відсутні — «—», provisional позначається `*`.
- Час останнього оновлення (локальний час із ISO-мітки backend).
- Попередження DEMO/mock, якщо `source` відповідний; реальний `deye_modbus` не підмінюється.
- Lottie використано **лише** для анімації заряд/розряд батареї, і тільки коли є реальне `p_batt`; інакше статичне WebP. Інші вузли потокової схеми — статичні зображення.

### Видалені хардкодовані/вигадані дані
- «Без світла 16 хв · на акумуляторі» (не мало жодного зв'язку з телеметрією).
- «14° Мряка» у шапці.
- Фіктивний 24-сегментний timeline (green/red без даних).
- Декоративні Lottie у всіх вузлах схеми, які анімувалися незалежно від наявності даних.

## 4. Реальна інтеграція Android ↔ backend (Stage 29)

Цепочка (без другого poller):

```
Deye SUN-12K-SG02LP1-EU-AM3 → Ebyte NA111-E 192.168.25.15:502 → Modbus TCP FC03
→ backend DeyePollingService (один) → SQLite → REST /api/live/raw + WS /ws → Android
```

Нове в Android:
- `network/TelemetryWebSocketClient.kt` — єдиний OkHttp WS-клієнт; `ws://` або `wss://` формується з поточного backend URL (`RetrofitClient.getWebSocketUrl()`); при зміні адреси автоматичне перепідключення; bounded backoff 1 с → 30 с; коректне `stop()`; ping every 15 s; **TrustAll не використовується**.
- `data/dto/WebSocketDto.kt` — DTO реального кадру `live_data`, Gson-парсер і мапер. У значення потрапляють тільки measurement з `quality=valid` і `normalized_value != null`; усе інше лишається `null`; `confidence=provisional` позначається; `demo_simulator` ніколи не перетворюється на `deye_modbus`.
- `viewmodel/MainViewModel.kt` — `startLiveUpdates/stopLiveUpdates`, ONLINE тільки після реального кадру зі статусом `connection_status=true`, хоча б одним вимірюванням і наявною міткою часу; при `RECONNECTING` виконується **один REST-запит** як резерв; stale/offline/error зберігають останні добрі дані, не підміняючи нулями. `onCleared()` закриває WS.
- `SvitGridApp.kt` — WS живе рівно стільки, скільки екран застосунку ( DisposableEffect), тобто один клієнт на весь час роботи.
- `RetrofitClient` — additive listeners зміни адреси (без зміни існуючого API).

## 5. Фактичні дані (свіжий read-only запит)

Після Stage 30 виконано новий ізольований запуск backend із реальним транспортом (ізоляція: `/tmp`, тимчасова SQLite, `SUNLINK_ENABLE_MODBUS=1` тільки для процесу):

```
POLL state=online source=deye_modbus soc=90.0 stale=false
active_registers=[184]  success_count=2 error_count=0
```

REST `/api/live/raw` (фактичний відповідь):
```json
{"p_sun":null,"p_load":null,"p_batt":null,"p_grid":null,"batt_soc":90.0,
 ... "timestamp":"2026-11-20T09:47:40.318294+00:00",
 "connection_status":true,"inverter_id":"deye_sg02lp1_am3",
 "available_registers":9,"source":"deye_modbus","battery_soc":90.0}
```

WS `/ws` (фактичний кадр):
```json
{"type":"live_data","data":{"battery_soc":{"raw_value":90,"normalized_value":90.0,
 "unit":"%","confidence":"corroborated","quality":"valid","address":184,
 "timestamp":"2026-11-20T09:47:40.282179+00:00"}},
 "status":{"connection_status":true,"last_update":"2026-11-20T09:47:40.282179+00:00",
 "inverter_id":"deye_sg02lp1_am3","available_registers":9,"source":"deye_modbus"}}
```

**94 % з Stage 28 більше не є очікуваним значенням**: свіжий фізично підтверджений SOC = **90 %**. У тестах 94 % зберігається лише як історичний формат-фікстура, не як очікуване значення застосунку. Усі інші параметри — `null`/unavailable.

## 6. Тести

| Набір | Результат |
|---|---|
| Backend `python3 -m unittest discover -s tests` | **276 PASS** |
| Android `./gradlew :app:testDebugUnitTest` | **93 PASS**, 0 failures/errors/skipped |
| Збірка `./gradlew :app:assembleDebug` | **BUILD SUCCESSFUL** |
| Валідація 12 Lottie JSON | PASS |
| Валідація 6 WebP у APK | PASS |
| aapt badging | package `com.sunlink.svitgrid`, versionCode `54`, versionName `2.33.0`, minSdk 26/targetSdk 34 |

Нові Android-тести (19):
- `WebSocketTelemetryTest` — парсинг реального кадру, маппінг, відмова `quality != valid`, DEMO-захист, provisional, пошкоджений JSON.
- `MainViewModelWebSocketTest` — ONLINE тільки після реального обміну, відмова при `connection_status=false`, порожні дані, відсутній timestamp, stale, DEMO source, ігнор не-`live_data`.
- `TelemetryWebSocketClientTest` — доставка кадру з MockWebServer, ігнор `ping`, перепідключення з backoff після обриву.
- `Stage30RealTelemetryIntegrationTest` — REST + WS на щойно знятих кадрах із SOC 90 %, Gson-парсинг реальних імен полів.

Offline/reconnect/stale/null покриті й backend-тестами Stage 27/28.

## 7. APK і GitHub Release

- Файл: `app/build/outputs/apk/debug/app-debug.apk`, 13 040 259 B.
- Package: `com.sunlink.svitgrid`; versionCode `54`; versionName `2.33.0`; type **debug / prerelease**.
- SHA-256: `893e12872f204288abe8d024076f9cc52ff82bd2ecfe797b3a5e45f371db24df`.
- Реліз: https://github.com/Ponamarenko/sunlink/releases/tag/android-v2.33.0-stage30-debug
- Назва: `SunLink Stage 30 — Dashboard & Real Telemetry (Prerelease)`, `isPrerelease=true`, target `e36dbf35...`.
- APK asset: https://github.com/Ponamarenko/sunlink/releases/download/android-v2.33.0-stage30-debug/app-debug.apk
- Checksum asset: https://github.com/Ponamarenko/sunlink/releases/download/android-v2.33.0-stage30-debug/app-debug.apk.sha256
- Верифікація: APK скачано з релізу, розмір і SHA-256 збігаються з локальною збіркою (PASS).
- Опубліковано через `scripts/publish_android_release.sh`; APK/checksum у Git-комміт **не додані**.

## 8. Скріншоти

**Немає.** У цьому середовищі відсутній підключений Android-пристрій і емулятор (`adb devices` — порожньо, AVD/system image відсутні). Скріншот головного екрана не створювався; у релізі й у звіті немає твердження про встановлення APK. Замість скріншота — схематичний предпросмотр у чаті (НЕ скріншот застосунку).

## 9. Змінені файли

**Код:** `app/src/main/java/.../data/dto/WebSocketDto.kt` (new), `network/TelemetryWebSocketClient.kt` (new), `network/RetrofitClient.kt`, `ui/SvitGridApp.kt`, `ui/dashboard/DashboardScreen.kt`, `viewmodel/MainViewModel.kt`, `app/build.gradle.kts`.

**Ассети:** 12 × `assets/lottie/icon_*.json` (new), 6 × `res/drawable-nodpi/sunlink_*.webp` (new).

**Тести:** `data/dto/WebSocketTelemetryTest.kt`, `integration/Stage30RealTelemetryIntegrationTest.kt`, `network/TelemetryWebSocketClientTest.kt`, `viewmodel/MainViewModelWebSocketTest.kt` (усі new).

**Документи/інше:** `docs/stages/stage-30-dashboard-real-telemetry.md` (new), `README.md`, `CHANGELOG.md`, `scripts/publish_android_release.sh`.

## 10. Production-інфраструктура

Не змінювались: production `.env`, systemd, production SQLite/DB, MikroTik, NAT/firewall/DNS, сертифікати/HTTPS, налаштування Ebyte чи інвертора. Write-команди Modbus (FC05/06/15/16) не виконувались. Другий production poller не запускався. Усі фізичні запити були read-only FC03 до регістра 184 в ізольованому процесі; тести WS/REST використовували локальний MockWebServer і кадри, зняті з реального backend.
