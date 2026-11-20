# SunLink direct HTTPS deployment (Stage 25B.1)

**Только план. Production deployment НЕ выполнялся.**

Схема: `Android → https://sunlink.pp.ua:9443 → 91.226.253.235:9443 → MikroTik dst-NAT → 192.168.25.20:9443 → SunLink backend (TLS завершается здесь)`.
Локально: `http://192.168.25.20:8800/` — HTTP для LAN/диагностики.

Один процесс, один FastAPI app, два listener: HTTP `0.0.0.0:8800` (lifespan on) и HTTPS `0.0.0.0:9443` (lifespan off, TLS в Uvicorn). Это исключает двойной startup/shutdown и двойной polling.

Запрещено: Reverse Proxy/Nginx/Caddy, Cloudflare Proxy/Tunnel, публичный HTTP, self-signed cert, отключение проверки сертификата, открытие 8800/502 в WAN. Порт **8443 больше не используется**.

## 1. Предпосылки

- Cloudflare: `sunlink.pp.ua` A → `91.226.253.235`, `*.sunlink.pp.ua` A → `91.226.253.235`, режим **DNS only**. Записи уже есть — не создавать дубликаты.
- Минимальный Cloudflare API token: Zone:DNS:Edit + Zone:Zone:Read, ограничен зоной `sunlink.pp.ua`; не Global API Key. Хранить в отдельном файле `0600`, не в Git, не в `.env`.
- Root/административный доступ к Debian и MikroTik — только по согласованию.

## 2. Получение сертификата (DNS-01)

```bash
sudo apt install -y certbot python3-certbot-dns-cloudflare
sudo install -m 600 /dev/null /etc/sunlink/cloudflare.ini
# Заполнить /etc/sunlink/cloudflare.ini вручную (dns_cloudflare_api_token = ...),
# токен НЕ выводить в терминал и НЕ сохранять в репозиторий.
sudo certbot certonly \
  --dns-cloudflare \
  --dns-cloudflare-credentials /etc/sunlink/cloudflare.ini \
  --dns-cloudflare-propagation-seconds 30 \
  -d sunlink.pp.ua \
  --agree-tos -m admin@sunlink.pp.ua --non-interactive
```

Файлы: `/etc/letsencrypt/live/sunlink.pp.ua/fullchain.pem`, `privkey.pem` (mode `0600` или `0640` root:sunlink). Проверка: `deploy/scripts/check_sunlink_certificate.sh`.

## 3. Backend и systemd

`deploy/systemd/sunlink-https.service.example` запускает `python deye_gateway.py`, который читает `SUNLINK_HTTP_PORT=8800`, `SUNLINK_HTTPS_PORT=9443` и TLS-пути из EnvironmentFile. Не запускать второй экземпляр.

Локальная проверка на сервере:
```bash
deploy/scripts/check_sunlink_ports.sh
curl -sS http://127.0.0.1:8800/status
curl -sS --resolve sunlink.pp.ua:9443:127.0.0.1 https://sunlink.pp.ua:9443/status
```

Если сертификат отсутствует, HTTPS listener не запускается, но локальный HTTP 8800 продолжает работать; ошибка конфигурации логируется без секретов.

## 4. MikroTik — только рекомендуемая схема (не применялось)

| WAN TCP | Server IP | Server TCP | Назначение |
|---:|---|---:|---|
| 9443 | 192.168.25.20 | 9443 | Публичный HTTPS |
| 8800 | 192.168.25.20 | 8800 | Локальный HTTP (желательно только LAN) |

Пользователь сообщил, что пробросы 8800 и 9443 уже созданы, но фактические правила **не проверялись**. Перед изменениями просмотреть точное содержимое правил. Если TCP 8800 проброшен с WAN — подготовить отдельное согласование на ограничение его доступом из LAN. Не удалять правила без просмотра и разрешения.

## 5. Debian firewall

Определить активный механизм (`nft list ruleset`, `iptables -S`, `ufw status`) и разрешить TCP 9443; TCP 8800 оставить для LAN; 502 — только внутренний. Не открывать админку/SSH. Проверить, что существующий SSH-доступ сохранён.

## 6. Обновление и откат

Обновление: `certbot renew --deploy-hook 'systemctl restart sunlink-https'`. Откат: остановить сервис, вернуть прежний listener, удалить добавленные правила по comment, вернуть прежний сертификат. Не удалять существующие release/tag.

## 7. Внешняя проверка (только после согласования)

```bash
curl -sS -m 10 -w '%{http_code} %{ssl_verify_result}\n' https://sunlink.pp.ua:9443/status
dig +short sunlink.pp.ua @1.1.1.1
```
Ожидается: валидный SAN `sunlink.pp.ua`, HTTP 200, JSON `/status`. Не использовать `curl -k` как доказательство корректного TLS.
