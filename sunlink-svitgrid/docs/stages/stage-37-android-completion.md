# Stage 37 — Full Android Application Completion

**Статус: PASS (Android-разработка, тесты, сборка, Release). Production не изменялся.**

## 1. Git

Initial HEAD `425da667b315f8c2cce5a163cedd8500e59ed36c` (= `origin/main`). Изменения Stage 37 коммитятся отдельно; final HEAD указывается в финальном ответе. Working tree до начала — только untracked мастер-ТЗ `мобильное приложение.txt`.

## 2. Аудит

- 15 маршрутов в NavHost; нижняя навигация — 4 пункта, drawer — полный список.
- Существовали Battle-tested экраны Stage 36: Dashboard, History, Statistics, Telemetry, Diagnostics, Settings.
- Проблема: Battery/PV/Grid/Load/Energy Flow в SvitGridApp были простыми экранами из плоской `toMap()` модели Stage 24 и не использовали категориальную модель Stage 36.
- Решение: новые законченные экраны на базе `TelemetryFullDto` без изменения production API и poller'а.

## 3. Реализованные экраны

| Экран | Что сделано |
|---|---|
| Dashboard | Добавлен блок «Швидкий доступ» с переходами на Battery/PV/Grid/Load/Energy Flow/Statistics |
| Battery | Новый экран: крупный SOC-индикатор, визуальная батарея, V/I/P/temperature, daily charge/discharge, статус заряд/разряд/невідомо |
| PV | Новый экран: PV total, daily/total energy, отдельные MPPT PV1/PV2/PV3 (V/I/P) |
| Grid | Новый экран: power/voltage/frequency/import/export/connection, структура L1/L2扩展性 |
| Load | Новый экран: power/voltage/current/daily energy |
| Energy Flow | Новый экран: PV/Inverter/Battery/Grid/Load с подписью об отсутствии направления при неполных данных |
| History | Периоды 1h/6h/24h/7d/30d вместо захардкоженного 2026-01-01…12-31 |
| Statistics | Сохранён из Stage 36 (24h/7d/30d/365d, MIN/MAX/AVG/SUM) |
| Diagnostics | Сохранён из Stage 36 (health/version/REST/WS/source) |
| Settings | Сохранён из Stage 32 (endpoint, тема, debug LAN) |

## 4. Общая инфраструктура

- `CategoryScreens.kt`: `ValueCard`, `CategoryScaffold` (единые карточки, stale/source/время), BatteryDetailScreen, PvDetailScreen, GridDetailScreen, LoadDetailScreen, EnergyFlowScreen.
- Все значения приходят из `/api/telemetry/full`; отсутствующие остаются null и показываются через `PowerValue(..., null) → «—»` + «Немає даних».
- `confidence=calculated` явно подписывается «розраховане значення».
- Навигация: уникальные маршруты, drawer + bottom bar, быстрый доступ с Dashboard, `launchSingleTop` в bottom bar.
- Локализация: uk/en паритет ключей проверен тестом; новые ключи для всех экранов/статусов.

## 5. UI/UX

Material 3, design system Stage 31, тёмная/светлая/system темы, адаптивные Row/weights, touch targets, content descriptions на иконках. Визуальная проверка на эмуляторе **не проводилась** — эмулятор/ADB недоступны; пользователь проверяет APK на телефоне.

## 6. Тесты

| Набор | Результат |
|---|---|
| Backend `unittest discover` | **330 PASS** |
| Android `testDebugUnitTest` | **126 PASS** (6 новых Stage 37) |
| Android lint | **BUILD SUCCESSFUL** |
| `assembleDebug` | BUILD SUCCESSFUL |
| UI-тесты (Compose) | NOT RUN (нет ui-test-junit4 в инфраструктуре) |
| Эмулятор/визуальная проверка | NOT RUN |

Новые тесты: уникальность и наличие всех 10+ маршрутов; чтение категориальной модели без фабрикации; DTO raw vs normalized; загрузка из MockWebServer; паритет uk/en; отсутствие фиктивных значений в новых экранах.

## 7. APK

- Package: `com.sunlink.svitgrid`
- versionCode **58**, versionName **2.37.0**
- Debug build, без production-подписи, prerelease.

## 8. Production safety

Не изменялись: active map `{184}`, poller, `.env`, systemd, SQLite, TLS/DNS/NAT/firewall/MikroTik, Ebyte, инвертор, production endpoint. Modbus read/write — NOT RUN. Deployment — NOT RUN.

## 9. Ограничения

- Категории PV/Battery/Grid/Load заполняются только реально доступным регистром 184 (SOC); остальные поля — «Немає даних».
- Отдельные экраны показывают честное пустое состояние, а не фиктивные Values.
- Статистика/история зависят от накопленной БД; пустые наборы обрабатываются корректно.
- Локализация новых экранов использует существующие uk/en ресурсы; часть старых надписей остаётся в исходных строках Stage 24–31.
