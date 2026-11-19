# Stage 19 — GitHub history recovery + one authorized FC03 SOC read

**Статус: `PASS — HISTORY CONFIRMED, FC03 SUCCESS`**

**Дата/время теста (UTC):** 2026-11-19T05:58:42.526993+00:00 … 05:58:42.783832+00:00
**HEAD до и после:** `e9df1598b9754fd8dab19d98d961221727c408cb` (документационный коммит Stage 19 создаётся поверх него).

## 1. Разрешение владельца

Владелец подтвердил: на физической RS485-шине нет другого Modbus Master;
актуальный IP Ebyte `192.168.25.15`; Unit ID `1`; разрешён ровно один FC03 к
регистру `184` (SOC). Это снимает блокировку Stage 18. Разрешение **не** включает
сканирование, повторы, запись или изменение оборудования.

## 2. Изучённые репозитории, ветки, теги и коммиты

| Репозиторий | Ветки | Теги | Коммиты | Как studied |
|---|---|---|---|---|
| `Ponamarenko/sunlink` | `main`, `master`, `backup-before-cleanup`, `mobile_app`, `release-v1.1.3` (+ remote refs) | 62 | 93 | локальная working copy + `git log -S` по всей истории |
| `Ponamarenko/SunPortal` | `main`, `deye-gateway`, `archive/phone-session-2026-10-21` | 32 | 1158 (по всем refs) | clone в `/tmp/sunportal-history`, `git log -S` по всей истории |

Ключевые коммиты SunLink:
- [`64ed6b4d368e`](https://github.com/Ponamarenko/sunlink/commit/64ed6b4d368e) — Initial commit: Deye Gateway Server v1.0; `server.log` содержит неудачную попытку подключения к `192.168.1.100:502` (timed out, 2026-10-31).
- [`393cc083e8fe`](https://github.com/Ponamarenko/sunlink/commit/393cc083e8fe) — Stage 3: первый физический read-only опрос; документ сообщает `.15:502`, `9600 8N1`, FC03 reg 184, raw 86 → 86%.
- [`4fa01e611306`](https://github.com/Ponamarenko/sunlink/commit/4fa01e611306) — Stage 4: верификация по дисплею; документ сообщает `.15:502`, Unit ID 1, reg 184 raw 85 / HEX `0x0055` → 85%, 11 регистров, время `13:32:27.745465` UTC 17.11.2026.
- [`6020410e9bbc`](https://github.com/Ponamarenko/sunlink/commit/6020410e9bbc) — Stage 6: лог HA, `solarman-tcp://192.168.25.14:8899`, serial suffix `45705`, Modbus ID 1 (другой endpoint).
- [`04c8b4a82169`](https://github.com/Ponamarenko/sunlink/commit/04c8b4a82169) — Modbus polling integration.
- [`c2dc416db044`](https://github.com/Ponamarenko/sunlink/commit/c2dc416db044), [`2de129a7cf24`](https://github.com/Ponamarenko/sunlink/commit/2de129a7cf24), [`dd6aa8ea63a7`](https://github.com/Ponamarenko/sunlink/commit/dd6aa8ea63a7) — официальные мануалы, топология, сверка доказательств.
- [`eb565d44970f`](https://github.com/Ponamarenko/sunlink/commit/eb565d44970f), [`504452007e44`](https://github.com/Ponamarenko/sunlink/commit/504452007e44), [`e9df1598b975`](https://github.com/Ponamarenko/sunlink/commit/e9df1598b975) — Stage 16–18.

Ключевые коммиты SunPortal:
- [`e93d3a76d16f`](https://github.com/Ponamarenko/SunPortal/commit/e93d3a76d16f) и [`ef860c456ec5`](https://github.com/Ponamarenko/SunPortal/commit/ef860c456ec5) — Deye Gateway integration; `config.yaml`: `host: 192.168.1.100`, `port: 502`, `slave_id: 1` (устаревший IP, не `.25.15`).
- [`24bf04495867`](https://github.com/Ponamarenko/SunPortal/commit/24bf04495867) — merge deye-gateway into main.

## 3. Найденные старые реализации чтения

| Реализация | Репозиторий/путь | Коммит | Что делает |
|---|---|---|---|
| `deye_gateway.py` (initial) | SunLink, root | `64ed6b4` | FastAPI + `ModbusTcpClient`, CONFIG `192.168.1.100:502` (неуспешно по логу) |
| `DeyeReadonlyAdapter` | SunLink, `server/deye_readonly_adapter.py` | `ee777b4` | FC03 only, `read_holding_registers`, карта регистров |
| Diagnostic script | SunLink, `server/readonly_diagnostic.py` | `393cc08` | Ограниченный FC03-опрос, документ сообщает успех |
| `ModbusPollingService` | SunLink, `server/services/` | `04c8b4a` | FC03, normalizer, SQLite |
| Deye Gateway integration | SunPortal, `deye-integration/` | `e93d3a7` / `ef860c4` | Аналогичный gateway + мобильный клиент, старый IP |

## 4. Реальные успешные чтения vs mock/demo

| Сведения | Статус | Почему |
|---|---|---|
| Stage 3: reg 184 raw 86 → 86% через `.15:502` | REPORTED (документ) | Первичный лог/вывод скрипта в репозитории отсутствует (вывод Stage 9.3.1) |
| Stage 4: reg 184 raw `0x0055`=85 → 85%, 11 регистров | REPORTED (документ) | Первичных HEX-байтов запроса/ответа в истории нет |
| HA log: `.14:8899`, serial 45705, Modbus ID 1 | CONFIRMED (документ лога) | Это ДРУГОЙ сетевой endpoint, не доказательство Ebyte-пути |
| SunPortal `config.yaml`: `.1.100:502` | CONFIRMED (код) | `server.log` показывает timed out — успешного чтения не было |
| DEMO/mock значения 85/86% в тестах Stage 12–16 | DEMO | Явно помечены `demo_simulator` / `mock`, не реальный инвертор |

## 5. Историческая топология и параметры

- Два разных endpoint: `.14:8899` (Solarman TCP, HA Sunsynk) и `.15:502` (Ebyte Modbus TCP→RTU). Разные IP/порты **не** доказывают разные физические шины (Stage 17: UNKNOWN).
- Официальный мануал Ebyte: factory defaults `192.168.3.7:8887`, `115200 8N1`; Simple mode — только один Modbus master. MBAP/CRC/Unit ID явно не описаны.
- Порт `502` согласован во всех исторических конфигурациях и отчётах: Stage 3/4, `equipment.md`, `deye_gateway.py`, SunPortal `config.yaml`. IP `.25.15` и Unit ID 1 подтверждены владельцем; старый `.1.100` — устаревшее значение.

## 6. Сверка перед запросом

| Параметр | Значение | Источник / статус |
|---|---|---|
| Инвертор | Deye SUN-12K-SG02LP1-EU-AM3 | Подтверждено владельцем |
| Ebyte IP | `192.168.25.15` | Подтверждено владельцем |
| Ebyte TCP port | `502` | История (Stage 3/4, equipment.md, deye_gateway.py, SunPortal config) — согласовано |
| Unit ID | `1` | Подтверждено владельцем; совпадает с HA log и Stage 3/4 |
| UART speed | `9600` | Только исторические документы; не перепроверялось |
| UART framing | `8N1` | Только исторические документы; не перепроверялось |
| Function | `FC03` | Разрешено владельцем |
| Register | `184` | Разрешено владельцем |
| Count | `1` | Ровно один регистр |

Проверка адреса: `domain/register_metadata.py`: `REGISTER_MAP[184].name = battery_soc`,
`data_type = U_WORD`, `scale = 1.0`, `unit = "%"`. Адаптер и normalizer передают
адрес напрямую в `read_holding_registers` — **смещений адресов нет**. Stage 4
тоже использовал регистр `184` напрямую. Противоречий по адресу нет.

## 7. Результат единственного физического FC03-запроса

Выполнено: **одно TCP-подключение, один `sendall`, один ответ**. Повторов нет.

| Поле | Значение |
|---|---|
| Время начала (UTC) | `2026-11-19T05:58:42.526993+00:00` |
| Время отправки (UTC) | `2026-11-19T05:58:42.745626+00:00` |
| Время завершения (UTC) | `2026-11-19T05:58:42.783832+00:00` |
| Endpoint | `192.168.25.15:502` |
| Unit ID | `1` |
| Transaction ID | `1` |
| Function Code | `03` |
| Address / Quantity | `184` / `1` |
| HEX запроса | `000100000006010300B80001` |
| HEX ответа | `0001000000050103020063` |
| Transaction ID в ответе | `1` (совпадает) |
| Unit ID в ответе | `1` (совпадает) |
| Protocol ID / Length | `0` / `5` |
| Function в ответе | `03` (Modbus exception отсутствует) |
| Byte count | `2` |
| Raw значение | `0x0063` = `99` |
| Декодирование | U_WORD × 1 → **SOC = 99 %** |

Разбор запроса: `0001` TID, `0000` Protocol, `0006` Length, `01` Unit, `03` FC03,
`00B8` = 184, `0001` = 1 регистр. Ответ: `0001` TID, `0000`, `0005` Length,
`01` Unit, `03` FC, `02` byte count, `0063` = 99.

CRC: в Modbus TCP поле CRC отсутствует; RTU-кадр через шлюз NA111-E не暴露ен,
поэтому проверка RTU CRC невозможна. Формат MBAP предполагается по стандарту
(мануал Ebyte его явно не описывает).

Интерпретация: `99 %` получено из подтверждённой карты регистров (U_WORD, scale 1).
Исторические чтения: 86 % (Stage 3) и 85 % (Stage 4) — разные моменты, не противоречие.
Значения с дисплея инвертора **на момент теста нет**, поэтому сверка с дисплеем
не выполнялась. Значение не выдаётся за верифицированное показание без проверки масштаба/знака —
они проверены по карте.

## 8. Подтверждение ограничений

- Повторных запросов: **0**. Второй попытки не было.
- Другие регистры не опрашивались; FC04/05/06/15/16 не выполнялись.
- Не было ping, nmap, nc, port scan, перебора Unit ID или адресов.
- Production backend, `.env`, systemd, Redis, HA, Ebyte, инвертор, MikroTik,
  firewall, Android deployment, текущий источник телеметрии — **не изменялись**.
- Постоянный Modbus poller не запускался; `SUNLINK_DATA_SOURCE` не переключался.
- APK не создавался; GitHub Release не создавался.

## 9. Ограничения и следующие шаги

- Один ответ FC03 не подтверждает остальные 8 регистров карты — нужны отдельные
  авторизованные тесты.
- MBAP/RTU CRC не подтверждены документацией Ebyte; ответ Matching TID/Unit/FC
  косвенно подтверждает стандартный Modbus TCP.
- Для полной верификации SOC нужна синхронная сверка с дисплеем инвертора.
- Исторические Stage 3/4 остаются document-claims (нет первичных логов) — этот
  тест добавил первичный HEX-ответ, но только для регистра 184.

Следующий логичный этап — по отдельному разрешению владельца точечная проверка
оставшихся регистров (по одному запросу на регистр) с обязательной сверкой с
дисплеем и обновлением уровней достоверности карты.
