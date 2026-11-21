# Stage 36 — Comprehensive Project Development

**Статус: PASS (разработка, тесты, APK). Production не изменялся.**

## A. Аудит

- HEAD до начала: `1c79a5990d8e46464f4d95078ce3f0a3169d1649`, `main = origin/main`, working tree — только untracked мастер-ТЗ.
- Реleases: последний `android-v2.35.0-stage32-debug` (hotfix endpoint).
- Backend: 321 тест, один poller `{184}`, REST/WS/stable, SQLite без потерь.
- Android: 115 тестов; реально работают Dashboard, History, InverterDetails, Diagnostics, Settings, Forecast; **не хватало** полноценных PV/Battery/Grid/Load/Statistics/Full-telemetry экранов и категориальной модели.

| Область | Состояние | Что сделано |
|---|---|---|
| Backend | PARTIAL | Добавлены DWORD-декодеры, категориальная модель, статистика, health/version API |
| Modbus | PASS | Один master, `{184}`, без записей — без изменений |
| Декодер | PASS | U/S WORD + U/S DWORD, word order, scale |
| Телеметрия | PARTIAL→PASS | Единая категориальная модель `to_full_dict()` |
| Хранение | PARTIAL→PASS | `get_statistics()` (min/max/avg/sum) |
| REST | PASS | +`/api/telemetry/full`, `/api/stats/summary`, `/api/health`, `/api/version` |
| WebSocket | PASS | Без изменений, совместимость тестами |
| Android Core | PASS | DTO, ApiService, Repository, ViewModel для новых API |
| UI/UX | PARTIAL→PASS | Экраны Telemetry и Statistics, обновлена Diagnostics |
| Графики | PARTIAL→PASS | `TelemetryChart` с честными пропусками |
| История/статистика | PASS | Периоды 24h/7d/30d/365d + параметры |
| Тесты | PASS | Backend 330, Android 120 |

## B. Backend — изменения

1. `domain/register_decoder.py`: `decode_u_dword()`, `decode_s_dword()` с `word_order` и scale.
2. `domain/telemetry.py`: `TelemetrySnapshot.to_full_dict()` — категории pv/battery/grid/load/inverter/energy, каждое значение с raw/normalized/unit/confidence/quality/timestamp; отсутствующие — `null`; `p_sun` считается только из присутствующих PV-мощностей и маркируется `calculated`.
3. `storage/repository.py` + `storage/sqlite_repository.py`: `get_statistics()` с агрегацией COUNT/MIN/MAX/AVG/SUM.
4. `deye_gateway.py`: 4 новых endpoint'а; старые `/status`, `/api/live/raw`, `/api/measurements/*`, `/ws` сохранены.

## C/D. Android — изменения

- DTO: `ComprehensiveDto.kt` (MeasurementValueDto, TelemetryFullDto, StatsSummaryDto, HealthDto, VersionDto).
- `ApiService`/`DelegatingApiService`: 4 новых вызова.
- `DataRepository`: `loadFullTelemetry()`, `loadStats()`, `loadHealth()`, `loadBackendVersion()`.
- `MainViewModel`: StateFlow `fullTelemetry`, `stats`, `backendHealth`, `backendVersion` + methods.
- `TelemetryScreen`: все категории, provenance, stale-маркировка; пропуски — «—».
- `StatisticsScreen`: периоды, параметры, min/max/avg/sum, график.
- `TelemetryChart`: сегменты не соединяются через пропуски.
- Навигация:臀部ные пункты «Телеметрія» и «Статистика»; Diagnostics показывает health/version backend.
- Локализация: uk/en строки.

## E. Тесты

| Набор | Результат |
|---|---|
| Backend `unittest discover` | **330 PASS** (9 новых Stage 36) |
| Android `testDebugUnitTest` | **120 PASS** (5 новых) |
| Физические Modbus-запросы | NOT RUN (запрещено) |
| Production deployment | NOT RUN (запрещено) |

## F. APK

Собирается с versionCode 57 / versionName 2.36.0. Package `com.sunlink.svitgrid`. Debug, unsigned production key — prerelease.

## Ограничения

- Категории заполняются только данными активного регистра 184 (`battery_soc`); остальные поля остаются `null` до отдельного решения по active map.
- Статистика работает поисторическим measurements из SQLite; при отсутствии истории показывает честное «Немає даних» / count 0.
- Физическая проверка новых регистров не проводилась и не требуется.

## Production safety

Не изменялись: endpoint, poller, active map `{184}`, `.env`, systemd, SQLite, TLS, MikroTik/DNS/firewall, Ebyte, инвертор; Modbus write не выполнялся.
