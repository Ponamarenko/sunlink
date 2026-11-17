# Этап 8 — Modbus Integration: опросчик и интеграция Data Layer SunPortal

**Статус:** выполнено. Реализован полноценный программный конвейер получения данных инвертора с mock-тестированием.  
**Режим:** только разработка и тестирование с mock Modbus-устройством. Физический инвертор не опрашивался.  
**Ветка:** `main` (origin/main).

## 1. Архитектура конвейера

```
[Mock Modbus / DeyeReadonlyAdapter]  →  [Polling Service]  →  [Normalizer]  →  [SQLite Repository]
     (FC 03 read only)                       (schedule, seq)   (RAW → eng)     (history, UTC)
```

Разделение ответственности:

| Компонент | Ответственность | Путь |
|---|---|---|
| Modbus adapter | Чтение регистров (FC 03 только) | `server/deye_readonly_adapter.py` |
| Polling service | Расписание, последовательность, reconnect, состояние | `server/services/modbus_polling_service.py` |
| Normalizer | RAW → инженерные величины | `server/domain/normalizer.py` |
| Repository | Хранение истории в SQLite | `server/storage/sqlite_repository.py` |
| Inverter state | Доступность и актуальность данных | `server/services/inverter_state.py` |
| Polling config | Интервалы, таймауты, бэкофф | `server/services/polling_config.py` |

Никаких write-операций (FC 05/06/15/16) не существует в конвейере.

## 2. Реализованные модули

| Модуль | Путь | Назначение |
|---|---|---|
| `services/polling_config.py` | `server/services/polling_config.py` | Настройки интервалов, таймаутов, ретраев, бэкоффа |
| `services/inverter_state.py` | `server/services/inverter_state.py` | Состояние инвертора: online/offline/degraded/unknown, актуальность данных |
| `services/modbus_polling_service.py` | `server/services/modbus_polling_service.py` | Цикл опроса, нормализация, сохранение, состояние |
| `modbus/client_interface.py` | `server/modbus/client_interface.py` | Интерфейс Modbus клиента (ABC) |
| `modbus/mock_client.py` | `server/modbus/mock_client.py` | Mock Modbus устройство для тестирования без физического инвертора |
| `demo_stage8.py` | `server/demo_stage8.py` | Демонстрационный сценарий с mock устройством |
| `tests/test_polling_config.py` | `server/tests/test_polling_config.py` | Тесты конфигурации (8 тестов) |
| `tests/test_inverter_state.py` | `server/tests/test_inverter_state.py` | Тесты состояния инвертора (11 тестов) |
| `tests/test_mock_client.py` | `server/tests/test_mock_client.py` | Тесты mock Modbus клиента (13 тестов) |
| `tests/test_polling_service.py` | `server/tests/test_polling_service.py` | Интеграционные тесты сервиса опроса (16 тестов) |
| `tests/test_polling_service_extra.py` | `server/tests/test_polling_service_extra.py` | Дополнительные сценарии (6 тестов) |

## 3. Последовательность опроса

1. Проверить состояние соединения адаптера.
2. При необходимости — соединиться (reconnect при разрыве).
3. Последовательно прочитать все регистры этапа 7 (FC 03, count=1):
   90, 91, 183, 184, 186, 187, 188, 190, 191.
4. Для каждого успешного чтения:
   - Захватить timestamp UTC (время получения, не сохранения).
   - Сохранить raw_value.
   - Передать в normalizer → NormalizedReading.
   - Сохранить через MeasurementRepository (пакетная запись в транзакции).
5. Обновить InverterState (online / degraded / offline).
6. Закрыть соединение при остановке сервиса.

Ошибка одного регистра не уничтожает успешно полученные измерения остальных регистров.

## 4. Используемые регистры (этап 7)

Все 9 регистров из метаинформации этапа 7:

| Рег. | Параметр | Достоверность |
|---:|---|:---:|
| 183 | Battery voltage | corroborated |
| 184 | Battery SOC | corroborated |
| 186 | PV1 power | corroborated |
| 187 | PV2 power | corroborated |
| 188 | PV3 power | corroborated |
| 191 | Battery current | corroborated |
| 190 | Battery power | provisional (знак не подтверждён) |
| 90 | DC temperature | provisional (offset −100 не измерен на AM3) |
| 91 | AC temperature | provisional (offset −100 не измерен на AM3) |

Другие регистры (166, 167, 169, 172, 175–178, 182, 192, 194, 76–84, 248–279) не опрашиваются.

## 5. Политика reconnect и обработка ошибок

| Ошибка | Обработка |
|---|---|
| TCP connection refused | `connect()` возвращает False, состояние → degraded |
| Connection timeout | Адаптер помечает соединение неактивным, следующий цикл пытается reconnect |
| Modbus timeout | Соединение сбрасывается, следующий цикл пытается reconnect |
| Modbus exception | Соединение сбрасывается, следующий цикл пытается reconnect |
| Ошибка отдельного регистра | Регистр пропускается, остальные продолжают опрашиваться, состояние → degraded |
| Ошибка SQLite | Ошибка явно видна в состоянии сервиса и результате цикла |
| Повторное подключение | Поддерживается — каждый цикл проверяет `connected` и соединяется при необходимости |

Ограниченный retry: `max_consecutive_failures` (по умолчанию 3) — после этого состояние → offline. Нет бесконечного быстрого цикла повторных подключений — интервал между циклами управляется `polling_interval`.

## 6. Модель состояния инвертора (`InverterState`)

| Поле | Описание |
|---|---|
| `inverter_id` | Идентификатор инвертора |
| `connection_status` | unknown / online / offline / degraded |
| `last_successful_poll` | Время последнего успешного опроса (UTC) |
| `last_failed_poll` | Время последнего неудачного опроса (UTC) |
| `last_error` | Текст последней ошибки |
| `consecutive_failures` | Количество подряд идущих неудач |
| `last_poll_duration_ms` | Длительность последнего цикла |
| `last_successful_measurement` | Последнее успешное измерение |
| `data_freshness_seconds` | Порог актуальности данных |

Правила:
- **online** — успешный опрос.
- **offline** — соединение отсутствует или `consecutive_failures >= max_consecutive_failures`.
- **degraded** — частичные ошибки регистров или 1–2 неудачных цикла подряд.
- **unknown** — опрос ещё не выполнялся.

Одна ошибка регистра не делает инвертор offline. Старые значения в SQLite не считаются актуальными — актуальность определяется по timestamp и `data_freshness_seconds`.

## 7. Конфигурация (`PollingConfig`)

| Параметр | По умолчанию | Описание |
|---|---|---|
| `inverter_id` | `"2507245705"` | ID инвертора для измерений |
| `polling_interval` | `5.0` с | Интервал между циклами (0 = один цикл и стоп) |
| `connect_timeout` | `2.0` с | Таймаут соединения Modbus TCP |
| `read_timeout` | `2.0` с | Таймаут чтения регистра |
| `max_register_retries` | `1` | Ретраи чтения регистра в цикле |
| `retry_backoff_base` | `0.5` с | База экспоненциального бэкоффа |
| `retry_backoff_max` | `2.0` с | Максимальный бэкофф |
| `max_consecutive_failures` | `3` | Порог перехода в offline |
| `data_freshness_seconds` | `30.0` | Порог актуальности данных |

Путь к SQLite БД задается через конструктор `SQLiteMeasurementRepository(db_path)`, не зашит в код.

## 8. Демонстрационный сценарий

`server/demo_stage8.py`:
1. Создает mock Modbus устройство с тестовыми значениями регистров.
2. Подключает polling service к mock через обертку.
3. Выполняет 3 цикла опроса (нормализация + сохранение во временную SQLite).
4. Имитирует временный сбой (TCP refused) — состояние → degraded.
5. Показывает восстановление соединения — состояние → online.
6. Выводит состояние инвертора и последние сохранённые измерения.
7. Все данные помечены MOCK/DEMO.

Демо не зависит от работающего backend или физического инвертора.

## 9. Тестирование

| Тестовый файл | Тестов | Статус |
|---|---|:---:|
| `tests/test_deye_readonly_adapter.py` | 22 | OK (этап 2, без регрессий) |
| `tests/test_models.py` | 8 | OK (этап 7) |
| `tests/test_normalizer.py` | 11 | OK (этап 7) |
| `tests/test_sqlite_repository.py` | 14 | OK (этап 7) |
| `tests/test_api_contract.py` | 11 | OK (этап 7) |
| `tests/test_polling_config.py` | 8 | OK (этап 8) |
| `tests/test_inverter_state.py` | 11 | OK (этап 8) |
| `tests/test_mock_client.py` | 13 | OK (этап 8) |
| `tests/test_polling_service.py` | 16 | OK (этап 8) |
| `tests/test_polling_service_extra.py` | 6 | OK (этап 8) |
| **Всего** | **120** | **OK** |

Покрытие сценариев этапа 8:
1. Успешное подключение ✓
2. Успешное чтение всех разрешённых регистров ✓
3. Правильное сохранение RAW ✓
4. Правильная нормализация ✓
5. Сохранение в SQLite ✓
6. UTC timestamps ✓
7. Частичная ошибка одного регистра ✓
8. Полный timeout ✓ (mock client)
9. TCP connection refused ✓
10. Modbus exception ✓ (mock client)
11. Повторное подключение после сбоя ✓
12. Ошибка SQLite ✓
13. Остановка polling service ✓
14. Повторный запуск после остановки ✓
15. Отсутствие двух параллельных polling циклов ✓
16. online/offline/degraded/unknown ✓
17. Устаревание данных ✓
18. Provisional confidence для 90, 91, 190 ✓
19. Отсутствие Modbus write operations ✓

## 10. Ограничения

- Физический инвертор не опрашивался (запрещено на этапе 8).
- Ebyte `192.168.25.15:502` не затрагивался.
- Solarman `192.168.25.14:8899` не затрагивался.
- Home Assistant не останавливался и не перенастраивался.
- Production backend (`simple_svitgrid.py`, `:8000`) не изменялся.
- `svitgrid.service`, `sunlink.service` не изменялись.
- Android-приложение не изменялось.
- `.env`, Redis, firewall не изменялись.
- Polling service не запускается в production на этом этапе.
- Нет FC05/06/15/16 — только FC 03.

## 11. GitHub

| Поле | Значение |
|---|---|
| Ветка | `main` (origin/main) |
| Локальный HEAD | `04c8b4a` (после коммита этапа 8) |
| Remote HEAD (origin/main) | совпадает с локальным |
| Изменённые файлы | `server/services/`, `server/modbus/`, `server/demo_stage8.py`, `server/tests/test_polling_*.py`, `docs/stages/stage-8-modbus-integration.md`, `README.md`, `CHANGELOG.md` |
| Секреты в коммите | отсутствуют |
| Тесты | 116/116 проходят (до этапа 8: 66, нового: 50) |
| Ссылка на коммит | https://github.com/Ponamarenko/sunlink/commit/04c8b4a |

## 12. Что не подключено к production

- `simple_svitgrid.py` — не изменён.
- `deye_gateway.py` — не изменён.
- `svitgrid.service` / `sunlink.service` — не изменены.
- SQLite не подключён к production backend.
- Polling service не запускается автоматически при импорте.
- Интеграция с working backend запланирована на следующий этап.
