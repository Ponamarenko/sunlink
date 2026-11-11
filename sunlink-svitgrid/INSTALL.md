# INSTALL.md — розгортання SunLink з нуля

Покрокова інструкція для розгортання **deye_gateway.py** (Python/FastAPI + Modbus TCP) та **Android-застосунку SunLink**.

## 1. Вимоги

### Gateway (сервер/ПК у тій самій LAN, що й інвертор)
- **Python 3.10+** (перевірено також на 3.13)
- Доступ по TCP до інвертора Deye (порт Modbus, зазвичай `502`)
- Мережевий доступ до машини gateway з пристроїв, де працює застосунок (або публічний проброс)
- Пакети Python: `fastapi`, `uvicorn[standard]`, `pymodbus` **3.x** (код використовує `device_id` у викликах `read_holding_registers`/`write_register`, сумісно з pymodbus 3.6+), `pydantic` та ін. (див. `sunlink-svitgrid/server/requirements.txt`)

### Android-застосунок
- **Android Studio** / Android SDK + JDK 17+ (або сумісний з Gradle-проєктом)
- **Gradle** (використовується wrapper `./gradlew`, у проєкті Gradle 8.9)
- Пристрій Android або емулятор для встановлення APK

## 2. Налаштування інвертора Deye

1. Призначте інвертору **статичний IP** у локальній мережі (через налаштування роутера/DHCP reservation або в інтерфейсі логера/інвертора), щоб адреса не змінювалася.
   - Робоча адреса в поточній інсталяції: **`192.168.25.15`**.
2. Увімкніть/переконайтесь у доступності **Modbus TCP**:
   - **порт `502`** — для прямого LAN Modbus TCP (типово для WiFi/LAN логера в режимі Modbus).
   - **порт `8899`** — у деяких конфігураціях Solarman/WiFi-dongle (якщо у вас саме dongle з іншим портом, вкажіть його в конфігурації gateway).
3. Переконайтесь, що машина gateway може з’єднатися з інвертором: `nc -zv 192.168.25.15 502` (або `telnet`).

## 3. Встановлення та запуск gateway (`deye_gateway.py`)

### 3.1. Розміщення та залежності
```bash
cd /root/opt/sunlink-svitgrid/sunlink-svitgrid/server/
pip install -r requirements.txt
```
> За потреби використовуйте `venv`: `python3 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt`.

### 3.2. Конфігурація Modbus
У файлі `deye_gateway.py` перевірте/налаштуйте блок `CONFIG`:
```python
CONFIG = {
    "modbus": {
        "host": "192.168.25.15",  # IP інвертора Deye
        "port": 502,              # 502 (Modbus TCP) або 8899 (деякі dongle)
        "timeout": 2,
        "slave_id": 1             # device_id (unit id), зазвичай 1
    },
    "server": {
        "host": "0.0.0.0",
        "port": 8800,
        ...
    },
    ...
}
```

### 3.3. Запуск
```bash
cd /root/opt/sunlink-svitgrid/sunlink-svitgrid/server/
nohup python3 deye_gateway.py > /tmp/deye_gateway.log 2>&1 &
```
Перевірка процесу/порту:
```bash
ps aux | grep deye_gateway
ss -tlnp | grep 8800
```

### 3.4. Перевірка API
```bash
curl -s http://127.0.0.1:8800/api/live/raw | head
```
Очікується JSON з полями `p_sun`, `p_string1..3`, `batt_soc`, `p_grid`, `p_load`, `connection_status` тощо. Якщо Modbus недоступний — `connection_status:false` та/або нульові/порожні дані (gateway повертає fallback без аварійного падіння).

Лог: `/tmp/deye_gateway.log` (можливі попередження pymodbus про `transaction_id` на деяких Deye/WiFi-логерах; за наявності валідних відповідей це не завжди критично).

## 4. Налаштування MikroTik (проброс порту назовні)

Приклад для RouterOS: проброс вхідного TCP `8800` на інтерфейсі `ether1` до хоста gateway `192.168.25.117:8800`:
```routeros
/ip firewall nat add chain=dstnat in-interface=ether1 protocol=tcp dst-port=8800 \
    action=dst-nat to-addresses=192.168.25.117 to-ports=8800
```
Додатково переконайтесь, що firewall дозволяє forward/dstnat до `192.168.25.117:8800`, а публічний DNS/домен (`ha4a.pp.ua`) вказує на зовнішню IP роутера. Перевірка ззовні: `curl http://ha4a.pp.ua:8800/api/live/raw`.

## 5. Збірка Android-застосунку

З кореня репозиторію:
```bash
cd /root/opt/sunlink-svitgrid
./gradlew :app:assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`

(Опційно встановлення на підключений пристрій: `adb install -r app/build/outputs/apk/debug/app-debug.apk`)

## 6. Налаштування застосунку

1. Відкрийте **Налаштування** в застосунку SunLink.
2. Вкажіть сервер: **адреса** `ha4a.pp.ua`, **порт** `8800` (або локальний `192.168.25.117:8800` / `192.168.25.20:8800` / `192.168.25.20:8000` залежно від доступного джерела).
3. Збережіть і перевірте дашборд/екран **«Відладка / Сирі дані»** (оновлення ~5 с) та статус з’єднання.

> За замовчуванням для нових інсталяцій: `ha4a.pp.ua:8000` (див. `SettingsRepository`); для роботи з gateway змініть порт на `8800`. Застосунок має fallback-логіку на LAN-ендпоінти (див. `ARCHITECTURE.md`).
