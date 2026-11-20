# Stage 25B — Direct HTTPS, Public Access, Android Migration & Release

**Статус: PASS (программная часть подготовлена; production deployment НЕ выполнялся).**
**HEAD до:** `aff74cf9b84b1803d4980e456b89feb357fdde3b`.

## A. Git
Initial HEAD `aff74cf9b84b1803d4980e456b89feb357fdde3b`; final commit создаётся поверх него; push в `origin/main`, `HEAD = origin/main`.

## B. Server (audit)

| Вопрос | Результат |
|---|---|
| Entrypoint в репозитории | `sunlink-svitgrid/server/deye_gateway.py` (FastAPI + Uvicorn); imperative `simple_svitgrid.py` / `main.py` / `full_main.py` / `svitgrid_server.py` / `deye_handler.py` в репозитории не найдены |
| Текущий bind/port (код) | `CONFIG["server"]["host"] = "0.0.0.0"`, port `8800` —历史上; фактический production процесс не проверялся (нет разрешения на доступ) |
| TLS сегодня | в коде отсутствует; добавлена штатная поддержка Uvicorn `ssl_certfile`/`ssl_keyfile` через env |
| systemd | подготовлен example (не применялся): `deploy/systemd/sunlink-https.service.example` |
| SQLite | схема не менялась; API/Data Layer совместимы; активный polling map остаётся `{184}` |
| Локальные потребители 8800 | не проверялись на сервере; в документе推荐 оставить 8800 только на loopback, WAN не открывать |

## C. TLS

- Домен: `sunlink.pp.ua`, публичный IP `91.226.253.235` (данные владельца).
- Cloudflare — DNS only; записи уже есть, не создавать дубликаты, не включать proxy.
- Стратегия: Let's Encrypt **DNS-01** через Cloudflare API token с минимальными правами Zone:DNS:Edit; token в защищённом файле `0600`, не в Git/`.env`/логах; не Global API Key.
- Пути: `/etc/letsencrypt/live/sunlink.pp.ua/fullchain.pem`, `privkey.pem` (mode 0600, owner `sunlink`).
- Продление: `certbot renew --deploy-hook 'systemctl restart sunlink-https'`.
- Android проверяет сертификат стандартным trust store; TrustAll/permissive TrustManager не используются.

## D. MikroTik (только команды-примеры)

```
/ip firewall nat
add chain=dstnat action=dst-nat protocol=tcp dst-address=91.226.253.235 dst-port=8443 \
    to-addresses=192.168.25.20 to-ports=8443 comment="SunLink HTTPS"
/ip firewall filter
add chain=forward action=accept protocol=tcp dst-address=192.168.25.20 dst-port=8443 \
    connection-state=new comment="SunLink HTTPS forward"
```
Откат: `remove [find comment="SunLink HTTPS"]`. Не менять порядок существующих правил, не открывать 8800/502/SSH. **MikroTik не изменялся.**

## E. Android

| Изменение | Результат |
|---|---|
| Default URL | `https://sunlink.pp.ua:8443/` (`BackendDefaults.HOST=HOST`, `PORT=8443`, `BASE_URL`) |
| Миграция | `settings_migration_version=2`; известные публичные хосты `ha4a.pp.ua`, `k4a.pp.ua`, `sunline.pp.ua` → новый URL + HTTPS; пользовательские адреса сохраняются; идемпотентно |
| Валидация | HTTPS обязателен для публичных хостов; HTTP только для local/LAN dev (127.0.0.1, localhost, 10.0.2.2, 192.168.25.20); IP с TLS запрещён; пустой/malformed/порт/whitespace отклоняются |
| Settings | поле адреса, HTTPS-чекбокс, «Перевірити з'єднання» + блок диагностики (URL, host, port, HTTPS, state, response time, last success, понятное сообщение) |
| Reconnect | сохранён существующий `DelegatingApiService` (реальная смена endpoint) и stale/offline-логика ViewModel; новых бесконечных loop нет |
| Security config | `network_security_config.xml`: cleartext=false базово, только local/LAN домены; глобальный debug-override cleartext убран; TrustAll нет |
| Security audit | нет HTTP fallback для публичного, нет TrustAll/hostname-verifier, нет секретов/TLS-ключей в Git, нет write/control Modbus endpoint |

## F. Tests

- Backend: **256 PASS** (`python3 -m unittest discover -s tests`) — добавлены 10 Stage 25B TLS-config тестов (port 8443 default, cert/key pair, missing files, invalid port, CA, запрет insecure-режимов).
- Android: **72 PASS** (`./gradlew :app:testDebug`) — новые тесты default URL, валидации HTTPS-public, миграции всех legacy-хостов, idempotency, сохранения custom URL, классификации DNS/TLS/timeout/HTTP ошибок.
- Local integration: MockWebServer/Retrofit/Repository/ViewModel/Dashboard-state проверены существующими Stage 12–24 тестами; TLS MockWebServer-сертификат не использовался — диагностика тестируется на уровне классификации ошибок.
- Emulator/device: **NOT VERIFIED** — устройства нет.

## G. APK

- `com.sunlink.svitgrid`, versionName `2.32.0`, versionCode `52`.
- Размер: 12 876 920 bytes.
- SHA-256: `e8bb98f912f4bb6b94070045826e8006cafd128c219b57395b8395ecb82f4e23`.
- Prerelease: https://github.com/Ponamarenko/sunlink/releases/tag/android-v2.32.0-stage25b
- APK asset: https://github.com/Ponamarenko/sunlink/releases/download/android-v2.32.0-stage25b/app-debug.apk
- Checksum: https://github.com/Ponamarenko/sunlink/releases/download/android-v2.32.0-stage25b/app-debug.apk.sha256
- Скачанный asset проверен: размер и SHA-256 совпали.
- Debug APK, не production-signed.

## H. Production safety

Production backend, `.env`, systemd, TLS-сертификаты, Nginx, Home Assistant, MikroTik, Cloudflare DNS, firewall/NAT — **не изменялись**. Порт 8800 в WAN не открывался. Физические Modbus-запросы не выполнялись, polling не активировался, активный map остаётся `{184}`. Внешние проверки `https://sunlink.pp.ua:8443/status` не выполнялись без отдельного разрешения.

## I. Следующий шаг — deployment plan (после согласования)

1. Получить сертификат DNS-01 для `sunlink.pp.ua` (credentials file `0600`).
2. Установить fullchain/privkey, права `0600`, owner `sunlink`.
3. Включить TLS в backend через env/systemd example, запустить один процесс.
4. Локально проверить `curl --resolve sunlink.pp.ua:8443:127.0.0.1 https://sunlink.pp.ua:8443/status` и `check_sunlink_ports.sh`.
5. Добавить dstnat + forward rule на MikroTik только для TCP 8443.
6. Внешняя проверка: DNS, TCP, TLS SAN/expiry, HTTP 200 `/status`.
7. Откат: остановить сервис, удалить правила по comment, вернуть прежний listener.

## Файлы этапа

- Android: `BackendDefaults.kt`, `SettingsRepository.kt`, `network_security_config.xml`, debug manifest, `SvitGridApp.kt`, `MainViewModel.kt`, `build.gradle.kts`, тесты.
- Backend: `tls_config.py`, `deye_gateway.py`, `tests/test_stage25b_tls_config.py`.
- Deploy: `deploy/https/README.md`, `deploy/https/sunlink-https.env.example`, `deploy/systemd/sunlink-https.service.example`, `deploy/scripts/check_sunlink_{https,certificate,ports}.sh`.
- Docs: этот отчёт, README, CHANGELOG.
