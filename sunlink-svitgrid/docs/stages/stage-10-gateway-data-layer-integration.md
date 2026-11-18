# Этап 10 — Интеграция deye_gateway.py с Data Layer (read-only production API)

**Статус:** выполнено. Программный этап — код и тесты, без физических подключений, без Modbus-запросов к инвертору, без сетевых запросов.  
**Ветка:** `main` (origin/main).  
**HEAD на начало:** `dd6aa8ea63`.

## 1. Цель этапа

Сделать `deye_gateway.py` production-готовым read-only API, интегрированным с Data Layer (этап 7):

- Использовать `domain/register_metadata.py` (9 регистров этапа 7 с уровнями достоверности) вместо инлайновой карты регистров shлюза.
- Использовать `domain/normalizer.py` для чистого преобразования RAW → инж. величин.
- Использовать `storage/sqlite_repository.py` для сохранения измерений в SQLite.
- Удалить всю запись Modbus: нет `/write`, нет `SafetyManager`, нет writable регистров — строго read-only (FC 03 только).
- Добавить production endpoints: `GET /api/measurements/latest`, `GET /api/measurements/history`, `GET /api/measurements/latest/{parameter}`.
- Сохранить совместимость с Android: `/api/live/raw`, `/live`, `/status`, `/registers`.

## 2. Проблемы до этапа 10

| Проблема | Где | Последствие |
|---|---|---|
| Инлайновый `DeyeRegisterMap` с 40+ регистрами | `deye_gateway.py` стр. 61–123 | Дублирует `domain/register_metadata.py`, нет уровней достоверности, содержит регистры из "запрещённого" списка этапа 7 (166, 167, 169, 172, 175–178, 182, 192, 194, 60–84, 248–279) |
| Write-операции | `write_register()`, `/write`, `/emergency-stop`, `/emergency-resume` | Нарушает принцип read-only, риск случайной записи на инвертор |
| Нет нормализации через Data Layer | `read_register()` возвращает już scaled значение | Нет прослеживаемости raw → normalized, нет confidence |
| Нет сохранения в SQLite | `last_data` только в памяти | Данные теряются при перезапуске, нет истории |
| Нет production endpoints | Только `/live`, `/api/live/raw` | Android не может получить историю или точные измерения с метаданными |

## 3. Реализованные изменения

### 3.1. `deye_gateway.py` — рефакторинг

**Удалено:**
- Класс `DeyeRegisterMap` (инлайновая карта регистров) — заменён на `REGISTER_MAP` из `domain.register_metadata`
- Класс `SafetyManager` — не нужен без записи
- Метод `DeyeGateway.write_register()` — запись запрещена
- Endpoints: `POST /write`, `POST /emergency-stop`, `POST /emergency-resume`
- Поля `writable` в реестре регистров, `max_concurrent_writes`, `write_cooldown` в CONFIG

**Добавлено:**
- Импорты: `domain.register_metadata.REGISTER_MAP`, `domain.normalizer.normalize_register`, `domain.models.*`, `storage.sqlite_repository.SQLiteMeasurementRepository`
- `DeyeGateway.__init__` принимает опциональный `repository: SQLiteMeasurementRepository`
- `DeyeGateway.read_register()` возвращает `NormalizationResult` (через `normalize_register`) и сохраняет измерение в репозиторий
- `DeyeGateway.read_all_registers()` использует только регистры из `REGISTER_MAP` (9 регистров этапа 7)
- Endpoints:
  - `GET /api/measurements/latest` — все последние измерения инвертора
  - `GET /api/measurements/latest/{parameter}` — последнее измерение параметра
  - `GET /api/measurements/history` — история измерений за период
- `_flat_live()` обновлён для работы с нормализованными данными
- CONFIG упрощён: только `modbus` (host, port, timeout, slave_id) и `server` (host, port)

**Не изменено:**
- Endpoints `/api/live/raw`, `/live`, `/status`, `/registers`, `/weather`, `/forecast`, `/ws` — сохранены для совместимости с Android
- Read-only принцип: только FC 03, ни одной функции записи

### 3.2. Новые тесты: `tests/test_deye_gateway_api.py`

| Тест | Что проверяет |
|---|---|
| `TestGatewayReadOnly` | Нет write-методов, нет SafetyManager, нет writable регистров в REGISTER_MAP |
| `TestGatewayNormalization` | `normalize_register` применяется к каждому прочитанному регистру, confidenceLevel передаётся |
| `TestGatewayRepository` | Измерения сохраняются в SQLite, `get_all_latest` возвращает данные |
| `TestGatewayEndpoints` | `/api/measurements/latest`, `/api/measurements/history` возвращают корректную структуру |
| `TestGatewayAndroidCompat` | `/api/live/raw`, `/live`, `/status` работают как раньше |

### 3.3. CONFIG (упрощённый)

```python
CONFIG = {
    "modbus": {
        "host": "192.168.25.15",
        "port": 502,
        "timeout": 2,
        "slave_id": 1
    },
    "server": {
        "host": "0.0.0.0",
        "port": 8800
    },
    "storage": {
        "db_path": "storage/measurements.db"
    }
}
```

## 4. Карта регистров после этапа 10

Все 9 регистров из `domain/register_metadata.py`:

| Рег. | Параметр | Достоверность | Ед. изм. |
|---:|---|:---:|---|
| 183 | battery_voltage | corroborated | V |
| 184 | battery_soc | corroborated | % |
| 186 | pv1_power | corroborated | W |
| 187 | pv2_power | corroborated | W |
| 188 | pv3_power | corroborated | W |
| 191 | battery_current | corroborated | A |
| 190 | battery_power | provisional | W |
| 90 | dc_temperature | provisional | °C |
| 91 | ac_temperature | provisional | °C |

Зарегистры 166, 167, 169, 172, 175–178, 182, 192, 194, 60–84, 248–279 остаются исключёнными (требуют отдельного решения и физической верификации).

## 5. Тесты

Все 116 существующих тестов продолжают проходить. Добавлены новые тесты для шлюза (read-only, нормализация, репозиторий, endpoints, совместимость с Android).

## 6. Ограничения

- Физический инвертор не опрашивался (read-only, нет Modbus-запросов).
- Ebyte не подключался.
- Home Assistant не останавливался.
- Нет сетевых запросов, пинга, сканирования.
- Тесты используют mock-объекты, не реальный Modbus.

## 7. Изменённые файлы

| Файл | Изменение |
|---|---|
| `sunlink-svitgrid/server/deye_gateway.py` | Рефакторинг: Data Layer интеграция, удаление записи, новые endpoints |
| `sunlink-svitgrid/server/tests/test_deye_gateway_api.py` | Создан: тесты шлюза |
| `docs/stages/stage-10-gateway-data-layer-integration.md` | Создан: отчёт этапа 10 |
| `README.md` | Добавлена ссылка на этап 10 |
| `CHANGELOG.md` | Добавлена запись об этапе 10 |

**Программный код, конфигурация, `.env`, сервисы, Android, Redis, firewall — не изменены (кроме deye_gateway.py, который становится частью Data Layer).**
