# Этап 9.3 — Аудит фактической топологии Deye ↔ Ebyte NA111-E ↔ Home Assistant

**Статус:** выполнено. Исключительно документальный и статический аудит.  
**Запрещённые действия не выполнялись:** kein TCP к `192.168.25.15:502` и `192.168.25.14:8899`, keine Modbus-запросы, kein сканирование сети, geen перенастройка Ebyte/инвертора/HA/backend/Android, geen изменение `.env`/Redis/firewall.  
**Ветка:** `main` (origin/main).  
**HEAD на начало:** `c2dc416db0`.

---

## 1. Проверенное Git-состояние

| Проверка | Значение |
|---|---|
| Ветка | `main` |
| Локальный HEAD | `c2dc416db0446949d03172942b8d8827de3b9ab3` |
| origin/main | `c2dc416db0` |
| Совпадение HEAD | Да |
| Незакоммиченные изменения (не build) | Отсутствуют |

## 2. Использованные источники

| # | Файл / путь | Роль |
|---|---|---|
| 1 | `docs/stages/stage-6-ha-register-cross-check.md` | Данные HA (лог Sunsynk add-on), серийный номер, адреса |
| 2 | `docs/stages/stage-3-first-real-read.md` | Первый физический read-only опрос через Ebyte `192.168.25.15:502` |
| 3 | `docs/stages/stage-4-modbus-display-verification.md` | Точечная верификация регистров через Ebyte |
| 4 | `docs/stages/stage-9.2-official-protocol-documentation.md` | Официальные мануалы Deye и Ebyte NA111-E |
| 5 | `docs/stages/stage-8-modbus-integration.md` | Polling service, не в production |
| 6 | `docs/stages/stage-2.5-readonly-audit.md` | Аудит read-only адаптера, остановка HA |
| 5 | `docs/equipment.md` | Оборудование, ограничения мастера |
| 6 | `docs/readonly-driver.md` | Read-only драйвер, адреса, slave_id |
| 7 | `docs/architecture.md` | Архитектура, бэкенд `:8000`/`:8800` |
| 8 | `README.md` | Схема, адреса |
| 9 | `INSTALL.md` | Инструкция установки, адреса, порты |
| 10 | `DEYE_READONLY_ADAPTER_STAGE2.md` | План переключения Modbus Master, учёт HA |
| 11 | `DEYE_SG02LP1_MODBUS_VERIFICATION.md` | Верификация карты, окружение, конфиги |
| 12 | `sunlink-svitgrid/server/deye_gateway.py` | Gateway CONFIG (host `192.168.25.15`, port `502`, slave_id `1`) |
| 13 | `sunlink-svitgrid/server/config.yaml` | Конфиг (host `192.168.1.100` — рассинхрон) |
| 14 | `sunlink-svitgrid/server/deye_readonly_adapter.py` | Адаптер (defaults `192.168.25.15:502`, slave_id `1`) |

**Примечание:** `simple_svitgrid.py` и `deye_handler.py` не найдены в репозитории — они находятся на сервере `192.168.25.20` по пути `/opt/sunlink/backend/` (вне этого repo). Production бэкенд не изменялся и не подключался.

---

## 3. Таблица IP/портов

| Адрес | Порт | Протокол | Устройство / роль | Источник | Статус |
|---|---|---|---|---|---|
| `192.168.25.15` | `502` | Modbus TCP (server) | Ebyte NA111-E → RS485 → инвертор Deye | stage 3, 4, deye_gateway.py, equipment.md | USER_REPORTED (заводские: `192.168.3.7:8887`) |
| `192.168.25.14` | `8899` | Solarman TCP | HA Sunsynk add-on → инвертор | stage-6.md, demo_stage7.py | USER_REPORTED (лог HA, не Modbus TCP) |
| `192.168.25.20` | `8000` | HTTP API | SvitGrid backend v2.0.0 (`simple_svitgrid.py`) | architecture.md, INSTALL.md | CONFIRMED (документ) |
| `192.168.25.20` | `8800` | HTTP API | deye_gateway.py (Modbus gateway) | README, architecture.md | CONFIRMED (документ) |
| `192.168.25.117` | `8800` | HTTP API | deye_gateway.py (локальный gateway) | README, INSTALL.md | CONFIRMED (документ) |
| `ha4a.pp.ua` | `8800` | HTTP (внешний) | Проброс MikroTik к gateway | README, INSTALL.md | CONFIRMED (документ) |

**Важное расхождение:** Заводские настройки Ebyte по мануалу (этап 9.2): IP `192.168.3.7`, порт `8887`, baud `115200`. Упоминаемые в проекте `192.168.25.15:502` и `9600` — **пользовательские (перенастроенные) значения**, не подтверждённые документом текущего состояния устройства.

---

## 4. Карта топологии (Mermaid)

```mermaid
flowchart LR
    subgraph LAN_192_168_25_0 ["LAN 192.168.25.0/24"]
        direction TB
        INV["Deye SUN-12K-SG02LP1-EU-AM3<br/>RS485 9600 8N1<br/>Slave ID: 1 (подтверждён этап 3)"]
        EBYTE["Ebyte NA111-E<br/>192.168.25.15:502<br/>TCP Server, Modbus TCP→RTU Simple"]
        SERVER20["192.168.25.20:8000<br/>SvitGrid backend v2.0.0<br/>simple_svitgrid.py (симулятор)"]
        GW117["192.168.25.117:8800<br/>deye_gateway.py<br/>Modbus TCP клиент к Ebyte"]
        HA["Home Assistant<br/>Sunsynk add-on<br/>был на 192.168.25.14:8899<br/>СЕЙЧАС: остановлен (этап 3-4)"]
        ANDR["Android SunLink<br/> fallback :8000/:8800"]
        EXT["ha4a.pp.ua:8800<br/>MikroTik NAT проброс"]
    end

    INV ====|RS485| EBYTE
    EBYTE -.->|TCP :502 (FC 03)| GW117
    EBYTE -.->|TCP :502 (потенциально)| SERVER20
    GW117 -->|HTTP :8800| EXT
    SERVER20 -->|HTTP :8000| ANDR
    GW117 -->|HTTP :8800| ANDR
    EXT -->|HTTP| ANDR
    HA -.->|Solarman TCP :8899 (остановлен)| INV

    classDef confirmed fill:#e1f5e1,stroke:#2e7d32;
    classDef user_reported fill:#fff9c4,stroke:#f9a825;
    classDef unknown fill:#ffcdd2,stroke:#c62828;
    classDef inferred fill:#e3f2fd,stroke:#1565c0;

    class INV,EBYTE,GW117,SERVER20,ANDR,EXT confirmed;
    class HA user_reported;
```

### Две независимые цепочки (не объединять без доказательств)

**Цепочка A — HA Sunsynk:**
```
Deye → RS485 → Solarman/WiFi-dongle (192.168.25.14:8899) → HA Sunsynk add-on
```
- Протокол: Solarman TCP (не Modbus TCP)
- Адрес:порт `.14:8899` ≠ Ebyte `.15:502`
- Статус: **остановлен пользователем** (подтверждено этапами 3–4)
- Доказательство: лог HA (stage 6), запись в stage-3/4 про "остановку клиента Home Assistant / sunsynk на `.14:8899`"

**Цепочка B — SunLink/Ebyte:**
```
Deye → RS485 → Ebyte NA111-E (192.168.25.15:502) → TCP → deye_gateway.py (:8800) / polling service (этап 8)
```
- Протокол: Modbus TCP → RTU (Simple)
- Адрес:порт `.15:502`
- Статус: шлюз существует, но **не используется в production** (`simple_svitgrid.py` — симулятор)

---

## 5. Анализ HA ↔ Solarman TCP (`192.168.25.14:8899`)

### Подтверждено логом HA (stage 6):
- Инвертор с серийным номером, заканчивающимся на `45705` (маскировано в отчёте)
- Modbus ID `1`
- `SENSOR_DEFINITIONS: single-phase`
- Номинал 12000 W
- Подключение `solarman-tcp://192.168.25.14:8899`
- Протокол 3.1
- Значения регистров (один снимок, время не синхронизировано с этапами 3–4)

### Что НЕ подтверждено:
- Что `.14:8899` — это тот же физический шлюз Ebyte NA111-E. Адрес и порт отличаются от `.15:502`.
- Что Solarman TCP ≡ Modbus TCP. Это разные протоколы.
- Что порт 8899 — Modbus TCP или Modbus TCP-to-RTU gateway.
- Текущее состояние HA Sunsynk add-on (запущен или остановлен сейчас).

### Вывод:
`192.168.25.14:8899` и `192.168.25.15:502` — это **разные конечные точки**. Нельзя утверждать, что HA обращается к инвертору через Ebyte NA111-E.

---

## 6. Анализ Ebyte ↔ Modbus TCP/RTU

### Подтверждено:
- Ebyte NA111-E на `192.168.25.15:502` — TCP Server, Modbus TCP → RTU Simple (этапы 3, 4, equipment.md, deye_gateway.py)
- Physically verified Slave ID `1` на этапе 3 (FC03 к reg 184, ответ 86%)
- FC 03 только (read-only адаптер)
- Нет активных TCP-соединений к `:502` на момент этапов 3–4 (подтверждено)
- HA Sunsynk остановлен на `.14:8899` (другой адрес:порт)

### Не подтверждено (этап 9.2):
- MBAP header, RTU CRC, Unit ID/Slave ID передача в TCP→RTU
- Текущие IP/порт/baud устройства (заводские: `192.168.3.7:8887`, `115200 8N1`; упомянутые: `192.168.25.15:502`, `9600`)
- Текущий режим работы NA111-E (simple conversion vs multi-host vs transparent)

---

## 7. Анализ ролей Modbus Master

| Компонент | Возможная роль | Подтверждение | Статус |
|---|---|---|---|
| HA Sunsynk add-on | Modbus/инверторный клиент через Solarman TCP | Лог HA stage 6 (подключение `.14:8899`, Modbus ID 1) | Был активен, сейчас **остановлен** (этапы 3–4) |
| Ebyte NA111-E | TCP/RTU шлюз (не master) | Официальный мануал этап 9.2 | Шлюз, прозрачно транслирует кадры |
| SunLink polling service (этап 8) | Подготовленный Modbus master | Код создан, тесты проходят | **Не в production**, не запущен |
| `simple_svitgrid.py` (`:8000`) | Симулятор, не Modbus master | DEYE_SG02LP1_MODBUS_VERIFICATION.md: "активный simple_svitgrid.py Modbus не использует", "данные — mock" | Не Modbus master |
| `deye_gateway.py` (`:8800`) | Modbus TCP клиент к Ebyte | Код используется `ModbusTcpClient`, slave_id=1 | Диагностический, не production сервис |

### Ответы на ключевые вопросы:

1. **Подтверждает ли лог HA фактический Modbus polling?**  
   Лог подтверждает, что HA Sunsynk add-on подключался к `solarman-tcp://192.168.25.14:8899` с Modbus ID 1 и получал данные регистров. Это был активный клиент инвертора. Но протокол — Solarman TCP, не Modbus TCP.

2. **Подтверждает ли лог использование именно Ebyte NA111-E?**  
   **Нет.** Адрес `.14:8899` ≠ Ebyte `.15:502`. HA подключался к другой конечной точке.

3. **Может ли HA обращаться к другому Solarman/RS485 адаптеру?**  
   **Да, возможно.** `.14:8899` указывает на другое устройство (или другую конфигурацию того же оборудования). Это не Ebyte NA111-E на `.15:502`.

4. **Можно ли утверждать, что Ebyte сейчас свободен?**  
   На момент этапов 3–4: да (нет активных TCP-соединений к `:502`, HA остановлен). Текущее состояние **неизвестно** — не проверялось на этом этапе.

5. **Можно ли безопасно запускать второй master?**  
   **Нет, нельзя утверждать.** NA111-E допускает только один Modbus master (официальный мануал этап 9.2). Если HA Sunsynk будет перезапущен, возникнет коллизия. Текущий владелец RS485/Modbus-сеанса **не установлен**.

---

## 8. Таблица параметров Ebyte NA111-E

| Параметр | Известное значение | Источник | Статус | Как безопасно подтвердить |
|---|---|---|---|---|
| Model | NA111-E | equipment.md, product page cdebyte.com | CONFIRMED | Визуально на устройстве / веб-интерфейс |
| Firmware | неизвестно | — | UNKNOWN | Веб-интерфейс устройства |
| IP | `192.168.25.15` | stage 3, 4, deye_gateway.py, equipment.md | USER_REPORTED | Веб-интерфейс или AT-команды |
| TCP port | `502` | stage 3, 4, deye_gateway.py | USER_REPORTED | Веб-интерфейс устройства |
| Work mode | TCP Server | equipment.md, stage 3, 4 | USER_REPORTED | Веб-интерфейс устройства |
| Gateway mode | Modbus TCP to RTU Open / Simple | equipment.md, stage 3, 4 | USER_REPORTED | Веб-интерфейс устройства |
| Baudrate | `9600` | equipment.md, stage 3, 4 | USER_REPORTED (заводской: 115200) | Веб-интерфейс устройства |
| Data bits | 8 | stage 3, 4 (UART 9600 8N1) | USER_REPORTED | Веб-интерфейс устройства |
| Parity | None | stage 3, 4 (8N1) | USER_REPORTED | Веб-интерфейс устройства |
| Stop bits | 1 | stage 3, 4 (8N1) | USER_REPORTED | Веб-интерфейс устройства |
| Timeout | 1000 ms | USER_REPORTED (этап 9.2 контекст) | USER_REPORTED | Веб-интерфейс устройства |
| Address filter | 0 | USER_REPORTED | USER_REPORTED | Веб-интерфейс устройства |
| Polling interval | 0 | USER_REPORTED | USER_REPORTED | Веб-интерфейс устройства |
| Keep time | 10 s | USER_REPORTED | USER_REPORTED | Веб-интерфейс устройства |
| Slave ID (инвертора) | `1` | stage 3 (физически подтверждён) | CONFIRMED (этап 3) | Повторный read-only опрос |
| Число TCP клиентов | до 6 (TCP server), 1 Modbus master | Официальный мануал этап 9.2 | CONFIRMED (документ) | Не требует подтверждения |

---

## 9. Solarman TCP vs Modbus TCP — различия

| Аспект | Solarman TCP (`192.168.25.14:8899`) | Modbus TCP (`192.168.25.15:502`) |
|---|---|---|
| Протокол | Solarman (произвольно-специфичный) | Modbus (открытый стандарт) |
| Порт | 8899 | 502 |
| Адрес | 192.168.25.14 | 192.168.25.15 |
| Используется HA | Да (Sunsynk add-on) | SunLink/Ebyte (девайс гейтвей) |
| Подтверждено как равный | **Нет** — не взаимозаменяемы | — |
| Статус | USER_REPORTED (лог HA) | USER_REPORTED (этапы 3, 4) |

**Вывод:** Не утверждать, что Solarman TCP и Modbus TCP взаимозаменяемы. Не предполагать, что порт 8899 — Modbus TCP.

---

## 10. Обнаруженные противоречия

| # | Противоречие | Детали | Резолюция |
|---|---|---|---|
| 1 | Адреса HA и Ebyte отличаются | HA: `192.168.25.14:8899` (Solarman TCP); Ebyte: `192.168.25.15:502` (Modbus TCP) | Это разные конечные точки, не смешивать |
| 2 | Заводские vs настроенные параметры Ebyte | Заводские: `192.168.3.7:8887`, `115200 8N1`; настроенные: `192.168.25.15:502`, `9600 8N1` | Настроенные значения не подтверждены документом текущего состояния |
| 3 | config.yaml vs deye_gateway.py | `config.yaml`: host `192.168.1.100`, port `8000`; `deye_gateway.py`: host `192.168.25.15`, port `8800`, slave_id `1` | `config.yaml` рассинхронизирован, не используется кодом |
| 4 | HA был активен, сейчас остановлен | Этапы 3–4 подтверждают остановку Sunsynk на `.14:8899`; текущее состояние неизвестно | Перед запуском любого мастера к Ebyte требуется подтверждение |
| 5 | Производство не использует Modbus | `simple_svitgrid.py` — симулятор (mock), pymodbus не установлен на сервере | Production не является Modbus master |

---

## 11. Подтверждённые факты

1. Инвертор `Deye SUN-12K-SG02LP1-EU-AM3` имеет интерфейс RS485 (официальный мануал Deye, etapa 9.2).
2. Ebyte NA111-E — RS485→Ethernet сервер с Modbus TCP шлюзом (официальный мануал Ebyte, etapa 9.2).
3. Ebyte NA111-E на `192.168.25.15:502` используется как Modbus TCP Server с TCP→RTU Simple (этапы 3, 4, deye_gateway.py, equipment.md).
4. Slave ID `1` физически подтверждён на этапе 3 (FC03 к reg 184, ответ 86% SOC).
5. На этапах 3–4 не было активных TCP-соединений к Ebyte `:502`; другой Modbus Master не обнаружен.
6. HA Sunsynk add-on подключался к `solarman-tcp://192.168.25.14:8899` с Modbus ID 1 (этап 6).
7. HA Sunsynk на `.14:8899` был остановлен пользователем перед этапами 3–4 (подтверждено в stage-3/4).
8. Production бэкенд `simple_svitgrid.py` на `:8000` — симулятор, не Modbus master.
9. `deye_gateway.py` на `:8800` — Modbus TCP клиент к Ebyte, диагностический, не production сервис.
10. Через один шлюз NA111-E в каждый момент времени должен работать только один активный Modbus Master (оборудование, документация Ebyte).
11. `simple_svitgrid.py` и `deye_handler.py` не находятся в этом репозитории (на сервере `192.168.25.20`).

---

## 12. Неизвестные параметры

| Параметр | Причина неизвестности | Необходимое действие |
|---|---|---|
| Текущий владелец RS485/Modbus-сеанса | HA остановлен, но неизвестно, будет ли перезапущен | Получить от пользователя вручную |
| Текущий IP Ebyte (заводской `192.168.3.7`, упомянутый `192.168.25.15`) | Не проверялось на этом этапе | Веб-интерфейс устройства |
| Текущий TCP порт Ebyte (заводской `8887`, упомянутый `502`) | Не проверялось | Веб-интерфейс устройства |
| Текущий baudrate Ebyte (заводской `115200`, упомянутый `9600`) | Не проверялось | Веб-интерфейс устройства |
| Режим работы NA111-E (simple conversion / multi-host / transparent) | Не проверялось | Веб-интерфейс устройства |
| Формат кадров NA111-E (MBAP, CRC, Unit ID) | Не документировано в мануале (этап 9.2) | Фактическая проверка на устройстве |
| Статус HA Sunsynk add-on сейчас (запущен/остановлен) | Не проверялось | Пользователь вручную |
| `simple_svitgrid.py` на сервере `192.168.25.20` — не в репозитории | Не доступен для чтения | Пользователь вручную |

---

## 13. Безопасные способы дальнейшей проверки

1. **Веб-интерфейс NA111-E** (заводской IP `192.168.3.7`, логин admin/admin — мануал стр. 40) — для проверки текущих IP, порта, baudrate, режима. Требует доступа к локальной сети, но не Modbus-запросов к инвертору.
2. **Подтверждение остановки HA Sunsynk** — у пользователя, перед любым подключением к Ebyte.
3. **Чтение протокола Solarman** — документация Solarman для понимания протокола на `.14:8899` (отдельно от Modbus).
4. **Проверка `simple_svitgrid.py`** на сервере `192.168.25.20` — чтение файла, не запуск.

---

## 14. Вывод о готовности к следующему этапу

**Нельзя переходить к физической диагностике через Ebyte NA111-E без дополнительных подтверждений.**

Причины:
1. Текущий владелец RS485/Modbus-сеанса не установлен (HA мог быть перезапущен).
2. Параметры Ebyte (IP, порт, baudrate, режим) не подтверждены для текущего состояния устройства.
3. Формат кадров NA111-E (MBAP, CRC, Unit ID) не документирован.
4. HA Sunsynk использует отдельную конечную точку `.14:8899` (Solarman TCP), конфликт с Ebyte возможен при перезапуске HA.

**Необходимо получить от пользователя вручную:**
- Подтверждение, что HA Sunsynk add-on остановлен и не будет перезапущен во время диагностики.
- Текущие параметры Ebyte NA111-E (IP, порт, baudrate, режим) через веб-интерфейс.
- Подтверждение, что нет других Modbus мастеров в сети.

---

## 15. Изменённые файлы

| Файл | Изменение |
|---|---|
| `docs/stages/stage-9.3-network-topology-and-master-audit.md` | Создан (отчёт этапа 9.3) |
| `README.md` | Добавлена ссылка на отчёт этапа 9.3 |
| `CHANGELOG.md` | Добавлена запись об этапе 9.3 |

**Программный код, конфигурация, `.env`, сервисы, Android, Redis, firewall — не изменены.**
