# SunLink direct HTTPS deployment (Stage 25B)

**Только план. Production deployment НЕ выполнялся в Stage 25B.**

Целевая схема: `Android → https://sunlink.pp.ua:8443 → 91.226.253.235:8443 → MikroTik dst-NAT → 192.168.25.20:8443 → SunLink backend (TLS завершается здесь)`.

Запрещено: Reverse Proxy/Nginx, Cloudflare Proxy/Tunnel, публичный HTTP, self-signed cert, отключение проверки сертификата, открытие 8800/502 в WAN.

## 1. Предпосылки

- Cloudflare: `sunlink.pp.ua` A → `91.226.253.235`, `*.sunlink.pp.ua` A → `91.226.253.235`, режим **DNS only**. Записи уже существуют — не создавать дубликаты.
- Root/административный доступ к Debian `192.168.25.20` и MikroTik — только по согласованию.
- Минимальный Cloudflare API token с правами **Zone:DNS:Edit** только для зоны `pp.ua`; не Global API Key. Хранить в отдельном защищённом credentials-файле с режимом `0600`, не в Git, не в `.env`, не в логах.

## 2. Получение сертификата (DNS-01)

Пример с certbot + cloudflare DNS plugin (устанавливать только после согласования):

```bash
sudo apt install -y certbot python3-certbot-dns-cloudflare
sudo install -m 600 /dev/null /etc/sunlink/cloudflare.ini
# Заполнить /etc/sunlink/cloudflare.ini (dns_cloudflare_api_token = ...) вручную,
# токен НЕ выводить в терминал и НЕ сохранять в репозиторий.
sudo certbot certonly \
  --dns-cloudflare \
  --dns-cloudflare-credentials /etc/sunlink/cloudflare.ini \
  --dns-cloudflare-propagation-seconds 30 \
  -d sunlink.pp.ua \
  --agree-tos -m admin@sunlink.pp.ua --non-interactive
```

Ожидаемые файлы:
- `/etc/letsencrypt/live/sunlink.pp.ua/fullchain.pem`
- `/etc/letsencrypt/live/sunlink.pp.ua/privkey.pem` (mode `0600`, owner `sunlink`)

Проверить: `deploy/scripts/check_sunlink_certificate.sh`.

## 3. Backend TLS

Backend — существующий FastAPI app `deye_gateway.py`, ASGI-сервер Uvicorn. Прямой TLS обеспечивается штатными флагами:

```bash
python deye_gateway.py   # читает SUNLINK_HTTPS_PORT / SUNLINK_TLS_CERTFILE / SUNLINK_TLS_KEYFILE
```
или через systemd example (`deploy/systemd/sunlink-https.service.example`) с `--ssl-certfile` / `--ssl-keyfile`.

Проверить конфигурацию до запуска: `python -c "from tls_config import TlsConfig; print(TlsConfig.from_env().enabled)"`.

Локальная проверка после запуска (на сервере):
```bash
curl -sS --resolve sunlink.pp.ua:8443:127.0.0.1 https://sunlink.pp.ua:8443/status
./check_sunlink_ports.sh
```

### Локальный HTTP 8800

Если локальные потребители используют `http://127.0.0.1:8800`, оставить слушатель **только на loopback** (`--host 127.0.0.1`) и закрыть его firewall для WAN. Публичный трафик идёт только через 8443/TLS. Не запускать два независимых production-процесса и не допускать двойного polling.

## 4. MikroTik — NAT и filter (подготовить, не применять без согласования)

```routeros
/ip firewall nat
add chain=dstnat action=dst-nat protocol=tcp dst-address=91.226.253.235 dst-port=8443 \
    to-addresses=192.168.25.20 to-ports=8443 comment="SunLink HTTPS"

/ip firewall filter
add chain=forward action=accept protocol=tcp dst-address=192.168.25.20 dst-port=8443 \
    connection-state=new comment="SunLink HTTPS forward"
```

Проверить порядок: existing `established,related,untracked accept` должен идти раньше общих drop; не добавлять широкие accept; не менять существующие правила. Просмотр counters: `/ip firewall nat print stats`, `/ip firewall filter print stats`.

Откат: удалить добавленные правила по `comment="SunLink HTTPS"` (`remove [find comment="SunLink HTTPS"]`).

Hairpin NAT: если внутренние клиенты обращаются к публичному имени, добавить отдельное rule только после проверки; внешнему клиенту достаточно dstnat + forward.

## 5. Debian firewall

Определить активный механизм (`nft list ruleset`, `iptables -S`, `ufw status`, `firewall-cmd --state`) и добавить только TCP 8443. Не открывать 8800/502/SSH/админку. Пример (nftables):
```bash
nft add rule inet filter input tcp dport 8443 ct state new accept
```
Проверить, что существующий SSH-доступ и локальные сервисы сохранены. Откат — `nft delete rule ... handle <handle>`.

## 6. Обновление и откат

Обновление сертификата: `certbot renew --deploy-hook 'systemctl restart sunlink-https'` (после согласования unit). Проверка срока: `check_sunlink_certificate.sh`.

Откат: остановить новый сервис, вернуть прежний listener/порт, удалить NAT/filter правила, при необходимости вернуть прежний сертификат. Не удалять существующие релизы/теги.

## 7. Внешняя проверка (только после согласования)

```bash
curl -sS -m 10 -w '%{http_code} %{ssl_verify_result}\n' https://sunlink.pp.ua:8443/status
dig +short sunlink.pp.ua @1.1.1.1
```

Ожидается: сертификат с SAN `sunlink.pp.ua`, действительный, HTTP 200, JSON `/status` SunLink. Не выполнять сканирование портов.
