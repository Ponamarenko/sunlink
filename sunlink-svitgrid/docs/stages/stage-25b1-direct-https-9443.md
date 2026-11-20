# Stage 25B.1 — Direct HTTPS port migration (8443 → 9443) + local HTTP 8800

**Статус: PASS (программная подготовка); production deployment НЕ выполнялся.**
**Initial HEAD:** `0ebba908dc45133f1fee8b35cb2883ad4cb61b30`.

## 1. Исходное состояние Stage 25B
Stage 25B подготовил прямой HTTPS на `https://sunlink.pp.ua:8443/` и DEMO-доступ; порт 8800 в WAN не открывался. Фактический production backend/entrypoint не проверялся (нет разрешения).

## 2. Новая архитектура портов

| Порт | Протокол | Назначение | Публичный? |
|---:|---|---|---|
| 8800 | HTTP | LAN, локальная диагностика, совместимость внутренних компонентов | **нет** |
| 9443 | HTTPS | основной адрес Android, TLS в Uvicorn | да |
| 8443 | — | больше не используется | нет |

Один FastAPI application, два Uvicorn Server в одном процессе: HTTP с `lifespan="on"` (единственный startup/shutdown), HTTPS с `lifespan="off"` и отключёнными signal handlers. Это исключает двойной вызов shutdown, двойной PollingService и конкуренцию за SQLite.

## 3. HTTP 8800
Host `SUNLINK_HTTP_HOST=0.0.0.0`, port `SUNLINK_HTTP_PORT=8800`. API-контракт не изменён: те же `/status`, `/api/live/raw`, `/api/measurements/*`. Если TLS-сертификаты отсутствуют, HTTP продолжает работать, HTTPS не запускается (ошибка логируется без секретов).

## 4. HTTPS 9443
Host `SUNLINK_HTTPS_HOST=0.0.0.0`, port `SUNLINK_HTTPS_PORT=9443`. TLS завершается непосредственно в Uvicorn через `ssl_certfile`/`ssl_keyfile`. Reverse Proxy не используется.

## 5. Почему 8443 больше не используется
Новый публичный порт согласован владельцем как 9443. Все активные defaults, примеры конфигурации и Android URL обновлены. Упоминания 8443 в историческом отчёте Stage 25B остаются как архивная запись; в новых файлах 8443 встречается только как `LEGACY_HTTPS_PORT` для миграции.

## 6. Почему Reverse Proxy не нужен
Существующий FastAPI/Uvicorn поддерживает TLS штатно; дополнительный nginx/Caddy создал бы второй процесс, усложнил бы завершение соединений и потребовал бы изменений, запрещённых владельцем.

## 7. Uvicorn TLS
`tls_config.py` проверяет пару сертификат/ключ, существование файлов, корректность port/host и разные порты для HTTP/HTTPS. `listener_config.py` строит два `uvicorn.Config` для одного app и запускает их через `asyncio.gather`.

## 8–10. Cloudflare DNS-01, Certbot, защита токена
Cloudflare DNS-only, записи уже существуют, не менялись. DNS-01 через `python3-certbot-dns-cloudflare`; токен с Zone:DNS:Edit + Zone:Zone:Read, ограничен зоной `sunlink.pp.ua`, хранится в `/etc/sunlink/cloudflare.ini` с режимом `0600`, owner root. Не Global API Key, не в Git, не в `.env`, не в Android, не в логах.Certbot и API в этом этапе **не запускались**.

## 11. Защита private key
`/etc/letsencrypt/live/sunlink.pp.ua/privkey.pem` — `0600` (владелец sunlink) или `0640` root:sunlink с членством сервисного пользователя в группе. Проверка через `check_sunlink_certificate.sh`. `chmod 777` не используется.

## 12. Android default URL
`https://sunlink.pp.ua:9443/` (`BackendDefaults.HOST/PORT/BASE_URL`). Локальный хост `192.168.25.20:8800` сохранён для LAN.

## 13. Миграция URL
Версия миграции повышена до 3. Старые публичные хосты (`ha4a`, `k4a`, `sunline`) → новый URL; старый default `sunlink.pp.ua:8443` → порт 9443; кастомные пользовательские адреса и порты не перезаписываются; миграция идемпотентна и сохраняет остальные настройки.

## 14. Проверки TLS
`network_security_config.xml`: cleartext=false глобально, HTTP только для localhost/127.0.0.1/10.0.2.2/192.168.*/10.*/172.16–31.*. TrustAll и hostname-verifier «принимать всё» отсутствуют. HTTPS-проверка сертификата стандартная.

## 15. Systemd example
`deploy/systemd/sunlink-https.service.example`: один процесс `python deye_gateway.py`, EnvironmentFile с 8800/9443 и TLS-путями, Restart on-failure, базовый hardening, зависимость от наличия сертификата, корректный restart после renewal. Реальный unit не менялся.

## 16. MikroTik — рекомендуемая схема (не применена)
| WAN TCP | Server IP | Server TCP |
|---:|---|---:|
| 9443 | 192.168.25.20 | 9443 |
| 8800 | 192.168.25.20 | 8800 |

Пользователь сообщил, что пробросы уже созданы, но правила **не проверялись**. Перед любыми изменениями — просмотр точного содержимого; удаление только после отдельного разрешения.

## 17. Проверка локального HTTP
`curl -sS http://127.0.0.1:8800/status`; `deploy/scripts/check_sunlink_ports.sh`.

## 18. Проверка удалённого HTTPS
`curl -sS https://sunlink.pp.ua:9443/status` (без `-k`), проверка SAN/срока/issuer через `check_sunlink_https.sh` и `check_sunlink_certificate.sh`. После согласования.

## 19. Рекомендация по WAN 8800
Если TCP 8800 проброшен с WAN — закрыть внешний доступ и оставить только LAN. Не выполнять изменение правила без согласования и просмотра точного правила.

## 20. Rollback
Остановить новый сервис, вернуть прежний listener/порт, удалить добавленные правила по comment, вернуть прежний сертификат. Не удалять существующие GitHub release/tag.

## 21. Известные ограничения
- Фактический production entrypoint, bind и NAT-правила не проверялись.
- Эмулятор/устройство отсутствуют — device validation NOT VERIFIED.
- Два listener реально не запускались на production; протестирована конфигурация и один app object.
- Старый сертификат ha4a истёк; новый для sunlink.pp.ua не выпускался.

## 22. Что НЕ выполнено на production
Production backend, `.env`, systemd, сертификаты, Certbot, Cloudflare API/DNS, MikroTik NAT/firewall, Debian firewall, HA, Nginx — не изменялись. Физический Modbus не использовался, polling не активировался, активный map остаётся `{184}`.

## APK и Release
`com.sunlink.svitgrid`, versionName `2.32.1`, versionCode `53`, размер 12 877 408 bytes,
SHA-256 `dd2be6bc26cfc1ae4e0845e7327b2c6e064d82537f0be1d21aa941771bd8fa57`.
Prerelease https://github.com/Ponamarenko/sunlink/releases/tag/android-v2.32.1-stage25b1
APK: https://github.com/Ponamarenko/sunlink/releases/download/android-v2.32.1-stage25b1/app-debug.apk
Checksum: https://github.com/Ponamarenko/sunlink/releases/download/android-v2.32.1-stage25b1/app-debug.apk.sha256
Скачанный asset проверен: размер и SHA-256 совпали.

Backend **258 PASS**, Android **74 PASS**, `verify_sunlink_release.sh` — PASS.

## Изменённые файлы
Backend: `tls_config.py`, `listener_config.py`, `deye_gateway.py`, тесты.
Android: `BackendDefaults.kt`, `SettingsRepository.kt`, `SvitGridApp.kt`, `network_security_config.xml`, тесты, `build.gradle.kts`.
Deploy: env example, systemd example, check-скрипты, README.
Docs: этот отчёт, README, CHANGELOG.
