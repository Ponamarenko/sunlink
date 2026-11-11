# ARCHITECTURE.md — архітектура SunLink

## 1. Огляд (ASCII-схема)

```
[Інвертор Deye 192.168.25.15:502]
            |
            |  Modbus TCP (holding registers, slave_id/device_id=1)
            v
[deye_gateway.py 192.168.25.117:8800]  (FastAPI, /api/live/raw, /live, /status, ...)
            |
            |  TCP :8800 (LAN)
            v
[MikroTik NAT :8800, ether1 dst-nat] -> 192.168.25.117:8800
            |
            |  Інтернет (публічний домен)
            v
[ha4a.pp.ua:8800]
            |
            |  HTTP (JSON)
            v
[Android SunLink v2.22.0]  (Dashboard, Debug/Сирі дані, fallback на LAN)
```

**Супутні вузли:** сервер SvitGrid v2.0.0 **`192.168.25.20:8000`** (`/api/v1/inverters`, `/data`, `/system/stats`) — джерело агрегованих даних по інверторах, але без повного набору PV-рядків/батареї/мережі в тих самих полях, що віддає gateway; застосунок може використовувати його як fallback/джерело.

## 2. Gateway: HTTP API та формат `/api/live/raw`

Сервіс: `sunlink-svitgrid/server/deye_gateway.py` (FastAPI + uvicorn, слухає `0.0.0.0:8800`).

### Основні ендпоінти
- `GET /api/live/raw` — **плоский JSON** для Android (сумісність зі SvitGrid-style полями). Рекомендований для дашборду.
- `GET /live` — live-дані: вкладений `data` (реєстри з `value/unit/timestamp`), `status`, а також верхньорівневі `p_sun/p_load/p_batt/p_grid/timestamp`.
- `GET /status`, `GET /registers`, `POST /read`, `POST /write`, `POST /emergency-stop`, `GET /health` тощо.

### Поля `/api/live/raw` (плоский формат)
Формуються у `_flat_live(...)` (розрахунки + значення з Modbus) і доповнюються сирими значеннями реєстрів:

| Поле | Опис |
|---|---|
| `p_sun` | Потужність PV, Вт (з `power_dc` або сума рядків; якщо 0 — сума `p_string1..3`) |
| `p_string1`, `p_string2`, `p_string3` | Потужність MPPT/PV рядків 1–3, Вт (`pv1_power..pv3_power`) |
| `pv1_voltage`, `pv1_current` … `pv3_voltage`, `pv3_current` | Напруга/струм рядків PV |
| `p_batt` | Потужність батареї, Вт (`battery_power`, знак залежить від напрямку) |
| `batt_soc` | SOC батареї, % |
| `battery_voltage`, `battery_current`, `battery_temp` | Параметри батареї |
| `p_grid` | Потужність мережі, Вт (з `grid_ct_power`, інакше баланс `p_load - p_sun - p_batt`) |
| `grid_voltage_l1`, `grid_voltage` | Напруга мережі L1, В |
| `p_load` | Навантаження, Вт (`total_load_power` або `power_ac`) |
| `timestamp` | Час формування відповіді (ISO) |
| `connection_status` | Стан з’єднання з інвертором (Modbus) |
| `error` | (опційно) повідомлення, якщо Modbus недоступний/немає даних |
| інші ключі | Додаткові «сирі» значення реєстрів у плоскому вигляді (name → value), якщо не перекриті вище |

При помилці читання: можливий `{ "error": "...", "timestamp": "...", "connection_status": false }`.

## 3. Карта регістрів Modbus (gateway)

Визначена в `DeyeRegisterMap._load_register_map()` (Deye SG02LP1/standard, скориговано під поточну інсталяцію). Читання: `read_holding_registers(..., device_id=slave_id)`; 32-бітні значення — два регістри (low + high<<16).

| Адреса | Ім’я | Тип | Scale | Од. | R/W | Призначення |
|---:|---|---|---:|---|:--:|---|
| 182 | `battery_temp` | U_WORD | 0.1 | °C | R | Температура батареї |
| 183 | `battery_voltage` | U_WORD | 0.01 | V | R | Напруга батареї |
| 184 | `battery_soc` | U_WORD | 1.0 | % | R | SOC |
| 186 | `pv1_power` | U_WORD | 1.0 | W | R | Потужність PV1 |
| 187 | `pv2_power` | U_WORD | 1.0 | W | R | Потужність PV2 |
| 188 | `pv3_power` | U_WORD | 1.0 | W | R | Потужність PV3 |
| 190 | `battery_power` | S_WORD | 1.0 | W | R | Потужність батареї |
| 191 | `battery_current` | S_WORD | 0.01 | A | R | Струм батареї |
| 109 | `pv1_voltage` | U_WORD | 0.1 | V | R | Напруга PV1 |
| 110 | `pv1_current` | U_WORD | 0.1 | A | R | Струм PV1 |
| 111 | `pv2_voltage` | U_WORD | 0.1 | V | R | Напруга PV2 |
| 112 | `pv2_current` | U_WORD | 0.1 | A | R | Струм PV2 |
| 113 | `pv3_voltage` | U_WORD | 0.1 | V | R | Напруга PV3 |
| 114 | `pv3_current` | U_WORD | 0.1 | A | R | Струм PV3 |
| 150 | `grid_voltage` | U_WORD | 0.1 | V | R | Напруга мережі L1 |
| 151 | `grid_frequency` | U_WORD | 0.01 | Hz | R | Частота мережі |
| 166 | `grid_inverter_power` | S_WORD | 1.0 | W | R | Потужність мережа/інвертор |
| 167 | `grid_l1_power` | S_WORD | 1.0 | W | R | Потужність мережі L1 |
| 172 | `grid_ct_power` | S_WORD | 1.0 | W | R | Потужність CT (мережа) |
| 175 | `total_load_power` | U_WORD | 1.0 | W | R | Загальне навантаження |
| 176 | `load_l1_power` | U_WORD | 1.0 | W | R | Навантаження L1 |
| 60 | `total_pv_production` | U_DWORD | 0.1 | kWh | R | PV виробництво (всього) |
| 63 | `daily_pv_production` | U_DWORD | 0.1 | kWh | R | PV за день |
| 74 | `daily_buy_energy` | U_DWORD | 0.1 | kWh | R | Куплено за день |
| 76 | `daily_sell_energy` | U_DWORD | 0.1 | kWh | R | Продано за день |
| 78 | `total_buy_energy` | U_DWORD | 0.1 | kWh | R | Куплено всього |
| 80 | `total_sell_energy` | U_DWORD | 0.1 | kWh | R | Продано всього |
| 84 | `daily_load_energy` | U_DWORD | 0.1 | kWh | R | Споживання за день |
| 85 | `total_load_energy` | U_DWORD | 0.1 | kWh | R | Споживання всього |
| 90 | `inverter_temp` | U_WORD | 0.1 | °C | R | Температура інвертора |
| 103, 104 | `fault_code_1/2` | U_WORD | 1.0 | — | R | Коди помилок |
| 230 | `max_solar_power_limit` | U_WORD | 1.0 | W | W | Ліміт PV потужності |
| 232 | `max_charge_current` | U_WORD | 1.0 | A | W | Макс. струм заряду |
| 233 | `max_discharge_current` | U_WORD | 1.0 | A | W | Макс. струм розряду |
| 248 | `tou_enable` | U_WORD | 1.0 | — | W | TOU увімкн. |
| 250,256,257,258 | `tou_*` | U_WORD | 1.0 | — | W | TOU слот 1 (час/SOC/потужність/заряд з мережі) |

> Примітка: карта/масштаби можуть потребувати уточнення для конкретної моделі/прошивки Deye (див. відомі застереження в `CHANGELOG.md`/README).

## 4. Android-застосунок (SunLink)

### Джерела даних і fallback
- **Налаштований base URL** формується з `SettingsRepository` (`serverAddress:serverPort`, типово `ha4a.pp.ua:8000` для нових інсталяцій) і застосовується через `RetrofitClient`.
- `MainViewModel.loadLive()`:
  1. Пробує налаштований сервер: відносні `api/live/raw`, `live`, а також `/api/v1/*` (інвертори/`{id}/data`, агрегація/мердж, `system/stats`).
  2. Якщо даних немає — fallback на **`http://192.168.25.20:8000/`** (SvitGrid v2.0.0).
  3. Якщо даних немає або бракує «багатих» полів (PV-рядки/батарея/мережа/навантаження) — додатковий fallback на **`http://192.168.25.20:8800/`** (`/live`, `/api/live/raw`) з підмішуванням (merge/overlay) значень.
- `checkConnection()` послідовно перевіряє `api/live/raw`, `live`, `api/v1/health`, `api/v1/inverters` на налаштованому та LAN `:8000/:8800`.

### Парсинг
- Плоскі відповіді (`/api/live/raw`) та live-відповіді нормалізуються у map; вкладені структури `/live` (`data.{reg}.value`) розгортаються у пласкі поля (див. `MainViewModel`/утиліти мапінгу).
- Поля з SvitGrid v2.0.0 (`/api/v1/inverters/{id}/data`, напр. `power_dc/power_ac/battery_soc/...`) мапляться/агрегуються у ключі, які очікує UI (`p_sun`, `p_load`, `batt_soc` тощо). Відсутні значення відображаються як `—`.

### UI/візуалізація
- **Dashboard** (`DashboardScreen.kt`, Material3, темна тема): PNG/Lottie вузли (PV/інвертор/батарея/мережа/дім), потоки енергії — **Bezier-криві** (`Canvas`, `Path.cubicTo`, `PathMeasure`) з анімованими частинками (`rememberInfiniteTransition`), напрямок/колір залежить від знаку/типу потоку; bottom sheet з деталями вузла, drawer-меню.
- **Debug / Сирі дані** (`DebugScreen.kt`): сирі ключі по інверторах/системі, автооновлення ~5 с.
- Статус з’єднання: `Є зв'язок` / `Немає зв'язку` з деталями URL/помилки. Файл `RetrofitClient.kt`/`ApiService.kt` описують HTTP-доступ; логіка діагностики — тег `SunLinkAPI`.
