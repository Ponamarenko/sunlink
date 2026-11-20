# HOTFIX — Trace and fix wrong release endpoint

**Статус: PASS (конфигурация исправлена); подключение к production — NOT VERIFIED (сервер недоступен).**

## 1. Откуда появился порт 8800

Цепочка формирование URL:

```
SettingsRepository(prefs) → getBaseUrl() = buildBaseUrl(serverAddress, serverPort, useHttps)
  → RetrofitClient.setBaseUrl(...)  → baseUrl
  → REST: ApiService @GET("api/live/raw")  → http://<host>:<port>/api/live/raw
  → WebSocket: RetrofitClient.getWebSocketUrl() → ws(s)://<host>:<port>/ws
  → MainViewModel.loadData/refresh/applyError → UI: «Помилка оновлення: ...»
```

Источник `8800` — **не UpdateManager**. UpdateManager обращается к `https://api.github.com/repos/Ponamarenko/sunlink/releases/latest` (constant `UPDATE_ENDPOINT`), а не к `sunlink.pp.ua`.

`8800` сохранился в `SharedPreferences` (`server_address=sunlink.pp.ua`, `server_port=8800`, `server_https=false`) от более ранней версии приложения. Исторически:

- до `0ebba90`: дефолт `ha4a.pp.ua:8800` (HTTP);
- `0ebba90`: дефолт `sunlink.pp.ua:8443` + миграция v2 только для legacy-хостов;
- `3a1796f`: миграция v3 добавила замену `8443 → 9443`, но **только если сохранённый порт равен 8443**.

Поэтому сохранённый публичный хост с портом `8800` (или не-HTTPS) не попадал ни под `legacyHost`, ни под `oldDefaultPort`, миграция v3 помечала настройки как актуальные и порт `8800` сохранялся навсегда. В результате `getBaseUrl()` давал `http://sunlink.pp.ua:8800/`, и сетевой вызов REST/WS показывал `Failed to connect to sunlink.pp.ua/91.226.253.235:8800`. Надпись «Ошибка обновления» — это отображение `applyError("Помилка оновлення: ${e?.message}")` в `MainViewModel.refresh()`/`loadData()`, то есть ошибка обновления телеметрии, а не проверки обновлений APK.

## 2. Какой компонент обращался к нему

- `SettingsRepository.getBaseUrl()` — формировал неверный адрес из сохранённых Preferences.
- `RetrofitClient` — использовал его как `baseUrl` для `ApiService` (REST `/api/live/raw`, `/status`, `/api/measurements/*`) и как источник для `getWebSocketUrl()` (`/ws`).
- `MainViewModel` — показывал сетевую ошибку как «Помилка оновлення».
- Update checker (`UpdateManager`, кнопка в «Про додаток») — **не** обращался к `:8800`.

## 3. Endpoints сейчас

| Тип | Endpoint | Источник |
|---|---|---|
| UPDATE | `https://api.github.com/repos/Ponamarenko/sunlink/releases/latest` | `UpdateManager.UPDATE_ENDPOINT` |
| REST | `https://sunlink.pp.ua:9443/` + `api/...` | `BackendDefaults.BASE_URL`, `BaseUrlMigration v4` |
| WEBSOCKET | `wss://sunlink.pp.ua:9443/ws` | `RetrofitClient.getWebSocketUrl()` |
| Debug LAN (только Debug) | `http://192.168.25.20:18800/` | `BackendDefaults.DEBUG_LAN_URL`, кнопка в Настройках |

## 4. Изменённые файлы

- `data/BackendDefaults.kt` — `BASE_URL=https://sunlink.pp.ua:9443/`, `LEGACY_HTTP_PORT=8800` только как маркер миграции, `DEBUG_LAN_HOST/PORT/URL` (18800), `isPublicHost()`, `debugLanUrlOrNull()` gated by `BuildConfig.DEBUG`.
- `data/SettingsRepository.kt` — `CURRENT_MIGRATION_VERSION=4`; любой публичный хост с не-9443 портом или без HTTPS приводится к согласованному URL; `useDebugLanEndpoint()` только в Debug; `usePublicEndpoint()`.
- `network/RetrofitClient.kt` — WS fallback больше не `127.0.0.1:8800`, а канонический `wss://sunlink.pp.ua:9443/ws`; публичный HTTP запрещён на уровне `setBaseUrl`.
- `network/NetworkDiagnostics.kt` (новый) — `RequestType { UPDATE, REST, WEBSOCKET }`, события с endpoint (без userinfo/query/fragment), этапом, HTTP-кодом, типом ошибки и временем последнего успеха.
- `update/UpdateManager.kt` — диагностика UPDATE (REQUEST/RESPONSE/FAILED/DOWNLOAD), константа `UPDATE_ENDPOINT`.
- `data/repository/DataRepository.kt` — диагностика REST.
- `network/TelemetryWebSocketClient.kt` — диагностика WEBSOCKET (CONNECTING/OPEN/LIVE_DATA/FAILED).
- `ui/diagnostics/DiagnosticsScreen.kt` — карта последних 10 событий и времени последнего успеха.
- `ui/SvitGridApp.kt` — подсказка `Debug LAN :18800` вместо `:8800`; debug-only кнопки LAN/публичный endpoint.
- `app/build.gradle.kts` — versionCode 56 / versionName 2.35.0.
- Тесты: `data/SettingsRepositoryTest.kt` (+3 hotfix), `integration/HotfixEndpointTest.kt` (новый, 7 тестов).
- `README.md`, `CHANGELOG.md`, этот отчёт.

Обновление — отдельный поток: ошибка UPDATE не блокирует Dashboard; `DashboardStateMapper` и WS/REST работают независимо (проверено тестом `update failure does not block dashboard telemetry state`).

## 5. Тесты и сборка

- Backend: **276 PASS**.
- Android: **115 PASS** (10 новых hotfix-тестов: миграция `8800 → 9443`, частичные настройки, сохранение LAN-адреса, отказ публичного HTTP, WS URL, UPDATE≠backend, отсутствие `sunlink.pp.ua:8800` в исходниках, санитизация секретов, независимость Dashboard от UPDATE).
- `assembleDebug`: BUILD SUCCESSFUL.
- aapt badging: `com.sunlink.svitgrid`, versionCode **56**, versionName **2.35.0**, minSdk 26/targetSdk 34.

## 6. Git и Release

- Исходный HEAD: `d6f271b4365d13f24f90b3500d9be79b257f4cea`.
- Commit и tag/release указываются в финальном ответе и в CHANGELOG.
- APK: debug, prerelease, SHA-256/PK size — в финальном ответе. APK в Git не добавлялся.
- Мастер-ТЗ `мобильное приложение.txt` не изменялся.

## 7. Что НЕ проверено

- **Production-подключение не проверено.** Прямой read-only запрос из среды разработки:
  `curl -m 12 https://sunlink.pp.ua:9443/status` → `Failed to connect to sunlink.pp.ua port 9443`.
  То есть сервер по согласованному адресу сейчас недоступен (согласно Stage 26, HTTPS-листенер на production не развёрнут). Конфигурация исправлена, но «подключение к production восстановлено» **не заявляется**.
- Debug LAN `http://192.168.25.20:18800/` также недоступен из этой среды (сервис не запущен).
- На физическом телефоне: миграция сохранённых Preferences, отображение диагностики, переключение Debug LAN/публичный endpoint и получение телеметрии — **NOT VERIFIED**.
- Реальная установка/скриншот не выполнялись: устройства и эмулятора нет.
