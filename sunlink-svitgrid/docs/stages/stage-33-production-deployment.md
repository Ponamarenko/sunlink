# Stage 33 — Production deployment (authorized)

**Статус: PASS.** HTTPS endpoint работает извне, REST и WebSocket отдают реальные физические данные инвертора.

## 1. Разрешение и границы

Владелец дал явное разрешение на deployment после установки hotfix APK `v2.35.0`. Изменения производились только на сервере `192.168.25.20`; MikroTik/NAT, DNS-записи, firewall и конфигурация Ebyte/инвертора не менялись. Токен Cloudflare был запрошен через приватную карточку, записан на сервере в `/root/.secrets/certbot/cloudflare.ini` (0600) и нигде не выводился.

## 2. Сертификат

- Установлены `certbot 4.0.0` и `python3-certbot-dns-cloudflare`.
- Порт 80 извне закрыт → выбран DNS-01 (`sunlink.pp.ua` делегирован на Cloudflare NS `amber/terry.ns.cloudflare.com`).
- `certbot certonly --dns-cloudflare ... -d sunlink.pp.ua` → `fullchain.pem`/`privkey.pem`, срок до **2027-02-19**, SAN `sunlink.pp.ua`, проверка deploy-гейта `openssl -checkend 86400` PASS.
- Auto-renewal настроен certbot'ом; deploy hook из bundle пока не устанавливался.

## 3. Backup и гейты

- Полный backup: `/root/sunlink-backup/2026-11-21-054952` (systemd units, env, rules, SQLite + SHA-256).
- Первая попытка деплоя остановилась гейтом «нет SQLite»: на сервере не было application DB. Создана **пустая** БД со схемой `/opt/sunlink/backend-stage25b1/storage/measurements.db` (без потери данных), после чего backup-гейт прошёл.
- Вторая попытка остановилась на копировании env (баг deploy.sh: локальный путь в `remote cp`) — скрипт исправлен на `cat > env < local`.
- Третий запуск: сервис поднялся, но упал из-за отсутствующих зависимостей (`pymodbus`, `httpx`) — установлены `pymodbus==3.6.2` и `httpx` в `/opt/sunlink/venv`.

## 4. Развёрнутый код и конфигурация

- Каталог: `/opt/sunlink/backend-stage25b1` (новый, legacy-файлы не перезаписаны).
- Установлен **актуальный код Stage 27/28** из репозитория, а не старый bundle: `deye_gateway.py` теперь в `if __name__ == "__main__"` собирает `DataSourceConfig.from_env()` и через `build_production_poller()` + `attach_to_gateway()` подключает **один** read-only poller.
- Причина: старый pymodbus-клиент из bundle не читал регистры (`isError=True`), хотя тот же FC03 через `ModbusTcpTransport` с сервера работал (raw `0x004C` = 76 %).
- Environment (`/etc/sunlink/sunlink-https.env`, 0600): `SUNLINK_DATA_SOURCE=deye_modbus`, `SUNLINK_ENABLE_MODBUS=1`, host `192.168.25.15:502`, unit 1, timeout 3, physical interval 30, HTTP 8800 / HTTPS 9443.
- Активный регистр только `184` (`battery_soc`); остальные не опрашиваются. Write-команды не выполняются.
- Юнит `sunlink-https.service` (Type=simple, один процесс). Легаси `svitgrid.service` на `:8000` оставлен работающим (симулятор, приложением не используется) — удаление/остановка не согласовывались.

## 5. Проверки

### Health-check (локально на сервере) — все PASS
```
HTTP 8800 listener                     PASS
HTTPS 9443 listener                    PASS
LAN HTTP /status                       PASS
LAN HTTPS /status (verify ON)          PASS
sunlink-https.service                  PASS
exactly one gateway process            PASS
```

### Внешние запросы (без `-k`)
```
GET https://sunlink.pp.ua:9443/status
→ HTTP 200, ssl_verify_result=0, connection_status=true, source=deye_modbus

GET /api/live/raw
→ batt_soc=76.0, raw не-нулевой, остальные поля null,
  timestamp=2026-11-21T04:05:06Z, source=deye_modbus

GET /api/measurements/latest
→ count=1, register 184, raw=76, normalized=76.0, quality=valid,
  confidence=corroborated, source=deye_modbus

WS wss://sunlink.pp.ua:9443/ws
→ {"type":"live_data","data":{"battery_soc":{"raw_value":77,...}},
   "status":{"connection_status":true,"source":"deye_modbus"}}
```

Значение SOC меняется между опросами (76 → 77), то есть данные действительно физические, а не фикстура.

### Тесты
- Backend `python3 -m unittest discover -s tests`: **276 PASS**.
- Android hotfix-тесты: **115 PASS** (без изменений в этом этапе).
- Новый APK не собирался — Android-код не менялся; актуальный APK `v2.35.0` из GitHub Releases остаётся правильным.

## 6. Откат

`bash /root/sunlink-backup/.../rollback.sh <BACKUP_DIR>` восстанавливает прежние unit/env/DB; SQLite при деплое не удалялась. Сертификат и токен при откате не удаляются.

## 7. Изменённые файлы в Git

- `sunlink-svitgrid/server/deye_gateway.py` — подключение единого production pipeline из env.
- `deploy/https/sunlink-https.env.example` — явный physical read-only профиль вместо DEMO.
- `README.md`, `CHANGELOG.md`, этот отчёт.

## 8. Осталось не проверенным

- UI на физическом телефоне после деплоя (владелец проверит сам; скриншот diagnostics до деплоя показывал `UnknownHostException`, публичный DNS при этом резолвит корректно — вероятно временный сбой мобильного DNS).
- Долговременная стабильность: не проверялась. `certbot renew --dry-run` — **PASS** (simulated renewal success).
- Другие регистры (кроме 184) по-прежнему не подтверждены и не отображаются.
- Легаси `svitgrid.service`/`:8000` не трогался.
