# Architecture — SunLink (Supplement to root ARCHITECTURE.md)

Этот файл дополняет (не дублирует) корневой `ARCHITECTURE.md` и описывает компонент read-only Modbus-драйвера Deye SG02LP1.

## Общая схема (текущее состояние)

```
[Инвертор Deye SUN-12K-SG02LP1-EU-AM3 192.168.25.15:502]
      | Modbus TCP FC 03 (holding registers)
      v
[Ebyte NA111-E: TCP Server, Modbus TCP→RTU Simple, RS485 9600 8N1]
      |
      | (один активный Modbus Master в момент времени)
      v
[DeyeReadonlyAdapter — read-only диагностика]  (порт :502, один цикл чтения)
      | (не подключён к svitgrid.service :8000 на этом этапе)
```

## Компоненты

| Компонент | Путь | Назначение | Статус |
|---|---|---|---|
| deye_gateway.py (gateway) | `sunlink-svitgrid/server/deye_gateway.py` | FastAPI HTTP gateway :8800, Modbus↔HTTP, read+write | Существует, не изменялся |
| DeyeReadonlyAdapter (read-only драйвер) | `sunlink-svitgrid/server/deye_readonly_adapter.py` | Только FC 03 чтение, карта SG02LP1, сводка measured/calculated | Реализован, протестирован (mock) |
| Диагностический скрипт | `sunlink-svitgrid/server/readonly_diagnostic.py` | Один цикл read_all(), закрытие соединения | Готов к запуску |
| **Data Layer — domain/models.py** | `sunlink-svitgrid/server/domain/models.py` | Типизированные модели измерений, качество, достоверность, источник | **Реализован, протестирован (этап 7)** |
| **Data Layer — domain/register_metadata.py** | `sunlink-svitgrid/server/domain/register_metadata.py` | Метаданные регистров, уровни достоверности, формулы | **Реализован, протестирован (этап 7)** |
| **Data Layer — domain/normalizer.py** | `sunlink-svitgrid/server/domain/normalizer.py` | Чистые функции нормализации (U/S_WORD, scale, temperature offset) | **Реализован, протестирован (этап 7)** |
| **Data Layer — storage/repository.py** | `sunlink-svitgrid/server/storage/repository.py` | Интерфейс `MeasurementRepository` (ABC) | **Реализован, протестирован (этап 7)** |
| **Data Layer — storage/sqlite_repository.py** | `sunlink-svitgrid/server/storage/sqlite_repository.py` | SQLite-хранение истории (версия схемы, индексы, транзакции) | **Реализован, протестирован (этап 7)** |
| SvitGrid backend v2.0.0 | `192.168.25.20:8000` (`simple_svitgrid.py`) | Мок-данные INV001, `/api/v1/*` | Работает, не изменяется |
| Android SunLink | `app/src/main/java/...` | Дашборд, fallback на :8000/:8800 | Не изменяется |

## Flow данных read-only драйвера

1. `connect()` — открытие Modbus TCP к `192.168.25.15:502` (slave_id по умолчанию 1).
2. `read_all()` — последовательное чтение подтверждённых регистров SG02LP1 (FC 03, count=1 для слов, count=2 для DWORD пар).
3. Декодирование: U_WORD (беззнаковое), S_WORD (знаковое, >32767 → отрицательное), U_DWORD (LW + HW<<16), применение scale.
4. Формирование `AdapterSnapshot` с пометкой источника каждого значения (`measured` / `calculated` / `unconfirmed`).
5. `disconnect()` — закрытие соединения. Фоновые процессы не остаются.

## Flow данных Data Layer (этап 7)

```
[Сырые значения регистров (из адаптера или вручную)]
       |
       v
[normalizer.py]  →  NormalizedReading (инженерные величины, сырой сохранён)
       |
       v
[models.py]      →  Measurement (полная provenience, качество, достоверность)
       |
       v
[sqlite_repository.py]  →  История в SQLite (UTC, индексы, транзакции)
```

Data Layer не подключён к production backend и не делает Modbus-запросов. Интеграция запланирована на следующий этап.

## Что не входит в этот компонент

- Запись в регистры инвертора (FC 05/06/15/16) — отсутствует по дизайну.
- TOU, лимиты заряда/разряда, аварийные команды — не реализованы.
- Автоматический polling / постоянный опрос — не реализовано.
- Интеграция в `svitgrid.service` (:8000) — не выполнена на этом этапе.
- Изменение Android-приложения, `.env`, Redis, firewall, Ebyte — не выполняется.
- Подключение SQLite к production backend — не выполнено на этом этапе.

## Связь с корневым ARCHITECTURE.md

Корневой `ARCHITECTURE.md` описывает схему через `deye_gateway.py :8800` и MikroTik NAT, а также Android fallback на `:8000`/`:8800`. Read-only драйвер и Data Layer являются отдельными диагностическими компонентами и не меняют существующую схему доставки данных в Android.
