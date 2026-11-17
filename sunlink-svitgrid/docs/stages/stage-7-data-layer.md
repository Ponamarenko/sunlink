# Этап 7 — Реализация базового Data Layer SunPortal

**Статус:** выполнено. Реализован и протестирован независимый слой данных инвертора с нормализацией и SQLite-хранением истории.  
**Режим:** только разработка и тестирование. Физический инвертор, production backend и Android не затрагиваются.  
**Новых Modbus-запросов:** нет. Не подключались к Ebyte, Solarman или физическому инвертору.

## 1. Реализованные модули

| Модуль | Путь | Назначение |
|---|---|---|
| `server/domain/__init__.py` | `sunlink-svitgrid/server/domain/__init__.py` | Экспорт доменных типов |
| `server/domain/models.py` | `sunlink-svitgrid/server/domain/models.py` | Типизированные модели измерений (`Measurement`, `NormalizedReading`, перечисления качества/достоверности/источника) |
| `server/domain/register_metadata.py` | `sunlink-svitgrid/server/domain/register_metadata.py` | Метаданные регистров: адрес, тип, масштаб, единица, уровень достоверности, формула, причина provisional |
| `server/domain/normalizer.py` | `sunlink-svitgrid/server/domain/normalizer.py` | Чистые функции нормализации: U_WORD/S_WORD декодирование, масштабирование, температурный offset, фабрика Measurement |
| `server/storage/__init__.py` | `sunlink-svitgrid/server/storage/__init__.py` | Экспорт интерфейса хранилища |
| `server/storage/repository.py` | `sunlink-svitgrid/server/storage/repository.py` | Абстрактный интерфейс `MeasurementRepository` (ABC) |
| `server/storage/sqlite_repository.py` | `sunlink-svitgrid/server/storage/sqlite_repository.py` | Реализация `SQLiteMeasurementRepository` с версией схемы, индексами, транзакциями, параметризованными SQL |
| `server/demo_stage7.py` | `sunlink-svitgrid/server/demo_stage7.py` | Демонстрационный сценарий с данными этапов 4 и 6 (временная SQLite БД) |

## 2. Модель измерения

`Measurement` (frozen dataclass) содержит:

| Поле | Тип | Описание |
|---|---|---|
| `inverter_id` | str | Идентификатор инвертора (например, серийный номер `2507245705` из лога HA) |
| `register_address` | Optional[int] | Адрес регистра (None для расчётных значений) |
| `parameter_name` | str | Имя параметра (например, `battery_voltage`) |
| `raw_value` | Optional[int] | Сырое 16-битное значение регистра, сохраняется независимо |
| `normalized_value` | Optional[float] | Инженерная величина после нормализации |
| `unit` | str | Единица измерения (V, %, W, A, °C) |
| `timestamp` | datetime | UTC, timezone-aware |
| `quality` | MeasurementQuality | Техническая доступность: valid / unavailable / stale / unverified / error |
| `confidence` | ConfidenceLevel | Семантическая достоверность: verified / corroborated / provisional / unverified |
| `source` | DataSource | Происхождение: modbus_register / calculated / manual |
| `description` | str | Описание / причина статуса |

**Важно:** техническая доступность (`quality`) отделена от семантической достоверности (`confidence`). Регистр может быть прочитан без ошибок (VALID) при этом его семантика для модели AM3 остаётся provisional.

Сериализация: `to_dict()` / `from_dict()` для стабильного API-контракта.

## 3. Начальная метаинформация регистров (этап 7)

Включены только регистры первого набора мониторинга, согласно требованиям этапа 7:

| Рег. | Параметр | Формула | Достоверность | Примечание |
|---:|---|---|:---:|---|
| 183 | Battery voltage | raw × 0.01 V | corroborated | Подтверждено этапами 3-4, 6 |
| 184 | Battery SOC | raw × 1 % | corroborated | Подтверждено этапами 3-4, 6 |
| 186 | PV1 power | signed_raw × 1 W | corroborated | Подтверждено этапами 3-4, 6 |
| 187 | PV2 power | signed_raw × 1 W | corroborated | Подтверждено этапами 3-4, 6 |
| 188 | PV3 power | signed_raw × 1 W | corroborated | Только 10/12 кВт (3 MPPT) |
| 191 | Battery current | signed_raw × 0.01 A | corroborated | Подтверждено этапами 3-4, 6 |
| 190 | Battery power | signed_raw × 1 W | provisional | Знак заряда/разряда не подтверждён (этап 6) |
| 90 | DC temperature | signed_raw × 0.1 − 100 °C | provisional | Offset −100 по Sunsynk TempSensor, не измерен на AM3 |
| 91 | AC temperature | signed_raw × 0.1 − 100 °C | provisional | Offset −100 по Sunsynk TempSensor, не измерен на AM3 |

Всего: 6 corroborated + 3 provisional = 9 регистров.

**Явно не включены (без отдельного решения):** 166, 167, 169, 172, 175–178, 182, 192, 194, 76–84, 248–279.  
Регистр 169 не включён: масштаб ×1 (Sunsynk) vs ×10 (текущий адаптер) не разрешён.  
Регистр 182 не включён: в логе HA значение 222 °C — нефизическое.  
Операции записи (FC 05/06/15/16) не добавляются.

## 4. Нормализация

Чистые функции (без Modbus, без I/O):

- `decode_u_word(raw)` — unsigned 16-bit (0..65535)
- `decode_s_word(raw)` — signed 16-bit two's complement (−32768..32767)
- `apply_scale(value, scale)` — линейное масштабирование
- `apply_temperature_offset(value, offset=100)` — `value − offset` (конвенция Sunsynk TempSensor)
- `normalize_register(address, raw, metadata)` — комбинированная нормализация одного регистра
- `make_measurement(...)` — фабрика `Measurement`

Грид-мощность не вычисляется через `load − PV − battery` и не выдаётся за физическое измерение.

## 5. SQLite-хранилище

`SQLiteMeasurementRepository(db_path)` реализует `MeasurementRepository`:

- **Схема с версией:** `PRAGMA user_version = 1`
- **Таблица `measurements`:** все поля `Measurement` + автоинкрементный `id`
- **Индексы:** `(inverter_id, parameter_name)`, `(timestamp)`, `(inverter_id, parameter_name, timestamp)`
- **Timestamps:** UTC, ISO-8601
- **Пакетная запись:** `save_measurements_batch` в одной транзакции (`with conn:`)
- **Чтение:** `get_latest`, `get_history(start, end)`, `get_all_latest`
- **Закрытие:** `close()`, поддержка контекстного менеджера (`with`)
- **Параметризованные SQL:** все запросы через `?` плейсхолдеры
- **Обработка ошибок:** `sqlite3.Error` → `RuntimeError`
- **Путь к БД:** через конструктор (конфигурация), не зашит в код
- Для тестов используется временная SQLite БД (`tempfile`)

Хранилище независимо от Home Assistant; Redis не является обязательной зависимостью.

## 6. Дémonстрационный сценарий

`server/demo_stage7.py`:

1. Использует только тестовые данные из отчёта этапа 4 (физический опрос Ebyte) и лога HA (этап 6)
2. Не обращается к Ebyte, Solarman или физическому инвертору
3. Создаёт 16 измерений, нормализует их
4. Сохраняет во временную SQLite БД
5. Читает последние значения и историю за интервал
6. Выводит отчёт, помечая данные как исторические диагностические примеры

Пример вывода:
```
LATEST VALUES PER PARAMETER:
  ac_temperature       42.4 °C   raw=  1424 [PROVISIONAL] src=stage-4 Ebyte
  battery_current          3.61 A   raw=   361 [CORROBORATED] src=stage-6 HA
  battery_power          1915.0 W   raw=  1915 [PROVISIONAL] src=stage-6 HA
  battery_soc              85.0 %   raw=    85 [CORROBORATED] src=stage-6 HA
  battery_voltage         53.02 V   raw=  5302 [CORROBORATED] src=stage-6 HA
  dc_temperature           36.5 °C   raw=  1365 [PROVISIONAL] src=stage-4 Ebyte
  pv1_power               211.0 W   raw=   211 [CORROBORATED] src=stage-6 HA
  pv2_power               131.0 W   raw=   131 [CORROBORATED] src=stage-6 HA
  pv3_power               107.0 W   raw=   107 [CORROBORATED] src=stage-6 HA
```

## 7. Тестирование

| Тестовый файл | Тестов | Статус |
|---|---|:---:|
| `tests/test_deye_readonly_adapter.py` | 22 | OK (существующие, без регрессий) |
| `tests/test_models.py` | 8 | OK |
| `tests/test_normalizer.py` | 11 | OK |
| `tests/test_sqlite_repository.py` | 14 | OK |
| `tests/test_api_contract.py` | 11 | OK |
| **Всего** | **66** | **OK** |

Тесты покрывают: signed/unsigned conversion, scale и offset, корректность raw_value, неопределённую семантику (provisional), quality/confidence, SQLite insert/read, пакетную запись, выборку по времени, транзакции, повторное открытие БД, ошибки хранилища, API-контракт (dict-формат, roundtrip).

Существующие 22 теста адаптера проходят без изменений — нет регрессий.

## 8. Что не подключено к production

- `simple_svitgrid.py` — не изменён
- `svitgrid.service` / `sunlink.service` — не изменены
- Порт 8000 — не изменён
- Текущие API endpoints — не изменены
- SQLite не подключён к production backend
- Android-приложение — не изменено
- `.env`, Redis, firewall — не изменены

Интерфейсы подготовлены так, чтобы последующий этап интеграции был простым и не требовал переписывания моделей.

## 9. GitHub

| Поле | Значение |
|---|---|
| Ветка | `main` (origin/main) |
| Локальный HEAD | `7c3a...` (после коммита этапа 7) |
| Remote HEAD (origin/main) | совпадает с локальным |
| Изменённые файлы | `server/domain/`, `server/storage/`, `server/tests/test_*.py`, `server/demo_stage7.py`, `docs/stages/stage-7-data-layer.md`, `docs/architecture.md`, `README.md`, `CHANGELOG.md` |
| Секреты в коммите | отсутствуют |
| Тесты | 66/66 проходят |
| Ссылка на коммит | https://github.com/Ponamarenko/sunlink/commit/7c3a... |

## 10. Следующий шаг

Ожидание согласования перед интеграцией Data Layer в production backend (`simple_svitgrid.py` / `deye_gateway.py`) и перед любыми изменениями карты регистров или формул normalizer.
