# Stage 26 — Production HTTPS Deployment (Preflight + Plan)

**Статус: PREFLIGHT ONLY — deployment НЕ выполнялся.**
**HEAD проверен:** `3a1796f1eb5ed532bb362c3560648f08d6d3d388` (= `origin/main`).

## 1. Фаза A — что проверено локально

### 1.1 Git
- Ветка `main`, HEAD `3a1796f...`, совпадает с `origin/main`.
- Рабочее дерево: только заранее существовавшие build-артефакты (`.gradle/`, `app/build/`) — не трогались.
- Remote: `git@github.com:Ponamarenko/sunlink.git` (SSH, без credentials в URL).

### 1.2 Программный аудит (все пункты подтверждены)

| Проверка | Результат |
|---|---|
| HTTP listener | `SUNLINK_HTTP_PORT=8800`, `tls_config.py` |
| HTTPS listener | `SUNLINK_HTTPS_PORT=9443`, TLS в Uvicorn |
| Один FastAPI app | `build_configs(app, ...)` использует один и тот же объект для двух `uvicorn.Config` |
| Один lifespan | HTTP `lifespan="on"`, HTTPS `lifespan="off"`; signal handlers только у HTTP |
| Один poller | Один shutdown hook `@app.on_event("shutdown")`, `gateway.disconnect()` + `_repository.close()` вызываются один раз |
| Одна SQLite | Нет второго процесса/пула; активный map `{184}` |
| TLS только при валидной паре | `TlsConfig.from_env()` требует оба файла и их существование; `https_kwargs()` без TLS выбрасывает ошибку |
| TrustAll / HTTP fallback | В Android main-исходниках отсутствуют; `network_security_config.xml` всегда проверяет сертификат |
| Android default URL | `https://sunlink.pp.ua:9443/` (`BackendDefaults.HOST/PORT/BASE_URL`) |
| Порт 8443 | Только константа `LEGACY_HTTPS_PORT` для миграции и исторические документы; в активных defaults нет |

### 1.3 DNS (read-only)

| Запись | Результат |
|---|---|
| `sunlink.pp.ua` A через `1.1.1.1` | `91.226.253.235` — совпадает с ожидаемым |
| AAAA | нет |
| CNAME | нет |
| Локальный resolver (`192.168.25.1`) | `192.168.25.20` (split-horizon DNS; `/etc/hosts` не содержит записи) |

DNS-проверка **не** подтверждает NAT, firewall или доступность сервиса.

### 1.4 Локальное окружение
- SSH-ключ `~/.ssh/sunlink_server` существует; алиас в `~/.ssh/config` не настроен.
- **Подключение к `192.168.25.20` НЕ выполнялось** — отдельное разрешение пользователя не получено.
- Debian version, hostname, IP-интерфейсы, процесс backend, занятость 9443, systemd unit, пользователь службы, SQLite path, `.env`, firewall rules, Certbot, `/etc/letsencrypt`, состояние HA/Nginx — **НЕ ПРОВЕРЕНЫ (NOT VERIFIED)**.

### 1.5 Подготовленные файлы
`deploy/https/README.md`, `deploy/https/sunlink-https.env.example`, `deploy/systemd/sunlink-https.service.example`, `deploy/scripts/check_sunlink_{https,certificate,ports}.sh`, `docs/stages/stage-25b1-direct-https-9443.md` — прочитаны и соответствуют архитектуре 8800/9443.

## 2. Фаза B — точный план (НЕ применять без подтверждения)

### 2.1 Backup (до любых изменений)
```bash
sudo install -d -m 700 /root/sunlink-backup/$(date +%F)
sudo cp /etc/systemd/system/<current-unit>.service /root/sunlink-backup/$(date +%F)/
sudo cp <backend-config> /root/sunlink-backup/$(date +%F)/
sudo systemctl cat <current-unit> > /root/sunlink-backup/$(date +%F)/unit-cat.txt
sudo nft list ruleset > /root/sunlink-backup/$(date +%F)/nftables.txt   # если используется nft
# MikroTik: /export file=sunlink-before-2026-11-20  (выполнить вручную, сохранить файл вне устройства)
```
Не копировать приватный ключ и Cloudflare token в backup.

### 2.2 Cloudflare token и credentials
- Token: Zone → DNS:Edit + Zone → Zone:Read, только зона `sunlink.pp.ua`; не Global API Key.
- Создание файла со скрытым вводом (НЕ выполнять до подтверждения):
```bash
sudo install -d -m 700 /root/.secrets/certbot
sudo install -m 600 /dev/null /root/.secrets/certbot/cloudflare.ini
sudo bash -c 'read -rsp "Cloudflare API token: " T; echo; printf "dns_cloudflare_api_token = %s\n" "$T" > /root/.secrets/certbot/cloudflare.ini; unset T'
sudo chown root:root /root/.secrets/certbot/cloudflare.ini
sudo chmod 600 /root/.secrets/certbot/cloudflare.ini
```
Токен не запрашивается в чате, не в Git, не в `.env`, не в логах.

### 2.3 Certbot + DNS-01
Сначала проверить, что уже установлено (`certbot --version`, `python3 -c "import certbot_dns_cloudflare"`). Не смешивать apt и snap.

Вариант apt:
```bash
sudo apt update
sudo apt install -y certbot python3-certbot-dns-cloudflare
sudo certbot certonly --dns-cloudflare \
  --dns-cloudflare-credentials /root/.secrets/certbot/cloudflare.ini \
  --dns-cloudflare-propagation-seconds 30 \
  -d sunlink.pp.ua --agree-tos -m admin@sunlink.pp.ua --non-interactive
```
Ожидаемые файлы: `/etc/letsencrypt/live/sunlink.pp.ua/fullchain.pem`, `privkey.pem`.

### 2.4 Права к private key
Проверить фактического пользователя службы. Безопасные варианты:
- `root:sunlink 0640` + пользователь службы в группе `sunlink`;
- либо `sunlink:sunlink 0600` на копии ключа, обновляемой deploy-hook.
Не `chmod 777`, не менять права всего `/etc/letsencrypt`.

### 2.5 Systemd + backend
- Backup текущего unit.
- Установить `deploy/systemd/sunlink-https.service.example` с `EnvironmentFile=/etc/sunlink/sunlink-https.env`.
- Проверить `systemd-analyze verify`.
- Проверить свободность 9443: `ss -ltnp | grep ':9443'`.
- Перезапуск только после успешной проверки конфигурации и сертификата.
- Проверить логи без секретов.

### 2.6 Debian firewall
Определить активную систему (`nft`, `iptables`, `ufw`). Цель: 9443 публично, 8800 только LAN, SSH сохранён, HA/другие сервисы не затронуты. Правила подготовить под фактическую систему; не очищать ruleset.

### 2.7 MikroTik (RouterOS 7.25.1)
Сначала `/export` и просмотр точных правил. Подготовить:
- WAN 9443 → `192.168.25.20:9443`;
- WAN 8800 → либо отсутствует, либо ограничен LAN (рекомендация);
- проверить `in-interface-list`, `src-address`, `chain=forward`, порядок filter;
- rollback по сохранённым исходным правилам.
Не удалять правила вслепую.

### 2.8 Проверки после deployment
1. LAN HTTP: `curl -sS http://192.168.25.20:8800/status` → 200 + JSON.
2. LAN HTTPS (hostname + SNI): `curl --resolve sunlink.pp.ua:9443:192.168.25.20 https://sunlink.pp.ua:9443/status` — **без `-k`**.
3. API: `/api/inverters`, `/api/live/raw`, `/api/measurements/latest`, `/history`.
4. Внешняя проверка из мобильной сети: `https://sunlink.pp.ua:9443/status`, проверка SAN/expiry/issuer, HTTP 200.
5. Внешний TCP 8800 должен быть недоступен.
6. Логи: нет двойного startup, нет двойного poller, нет ошибок SQLite.
7. Android: установка APK Stage 25B.1 (только с разрешения), проверка URL/TLS/API/reconnect.

### 2.9 Renewal
Deploy-hook: проверить 존재ствие fullchain/privkey, SAN и срок, перезапустить **только** SunLink service, логировать без секретов. `certbot renew --dry-run` после настройки credentials.

## 3. Rollback

| Компонент | Rollback |
|---|---|
| Certbot credentials | Удалить созданный файл, вернуть прежний (если был) |
| TLS config | Вернуть прежний EnvironmentFile/пути |
| systemd | `systemctl disable --now <new-unit>`, восстановить backup unit, `daemon-reload`, запустить прежний сервис |
| Listener | Вернуть прежние порты/host из backup; при сбое HTTPS оставить локальный HTTP 8800 |
| Debian firewall | Восстановить ruleset из backup |
| MikroTik | Импортировать/применить сохранённый export или удалить только добавленные правила по comment после просмотра |
| Android | При необходимости вернуть previous URL через Settings (сохраняется в SharedPreferences) |

Rollback не должен удалять данные SQLite или пользовательские настройки Android.

## 4. Риски

- Локальный DNS возвращает LAN IP — внешняя доступность должна проверяться извне (мобильная сеть), иначе hairpin NAT может дать ложный результат.
- Неизвестно текущее состояние HA/Nginx/других проектов на сервере: требуется read-only проверка после разрешения.
- Неизвестно, проброшен ли WAN 8800: при положительном ответе требуется отдельное согласование закрытия.
- Двойной запуск сервиса приведёт к двойному polling и конкуренция за SQLite — запускать только один unit.
- Обновление сертификата без deploy-hook может оставить backend со старым сертификатом.

## 5. Требуемое подтверждение пользователя

Перед Фазой C необходимо явное подтверждение:
1. Разрешение на read-only SSH-аудит `192.168.25.20`.
2. Разрешение на конкретный план: установка certbot/plugin, создание credentials, выпуск сертификата, права к ключу, новый systemd unit, firewall, MikroTik NAT.
3. Разрешение на внешнюю проверку и (при наличии устройства) установку APK.

**До получения подтверждения ничего из перечисленного не выполнялось.**
