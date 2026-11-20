# Stage 25A — SunLink Connectivity & HTTPS Diagnostic

**Статус:** диагностика выполнена; исправления и deployment НЕ выполнялись (отдельная согласованная часть Stage 25).
**HEAD начальный/конечный:** `5f62042b508d103d641ec4d409f969b30baef715`.
**Production, DNS, firewall, reverse proxy, сертификаты, Android-код — не изменялись.**

## 1. Исходная проблема

Android 2.31.0: `CLEARTEXT communication to k4a.pp.ua not permitted by network security policy`.
Проверка `https://k4a.pp.ua:8800/docs` — страница не открывается.

Одновременно выяснено: `k4a.pp.ua` **отсутствует во всех исходниках и документах репозитория**. Код использует `ha4a.pp.ua:8800` (`BackendDefaults`, `SettingsRepository`, `network_security_config.xml`). Следовательно, `k4a.pp.ua` — значение, введённое пользователем в Settings и сохранённое в SharedPreferences (ключ `server_address`), либо опечатка. При старте `SvitGridApp` вызываёт `RetrofitClient.setBaseUrl(repo.getBaseUrl())` — сохранённое значение переопределяет дефолт. Валидация хоста (`HOST_REGEX = ^[A-Za-z0-9._-]+$`) принимает любое синтаксически корректное имя, без списка разрешённых доменов.

## 2. Настройки Android (без изменений кода)

| Проверка | Результат |
|---|---|
| Дефолт | `ha4a.pp.ua`, порт `8800`, `http://` (`BackendDefaults.HOST/PORT/BASE_URL`) |
| Источник URL | SharedPreferences → `getBaseUrl()` → `RetrofitClient.setBaseUrl()` при старте и после Save |
| `network_security_config.xml` | `base-config cleartextTrafficPermitted="false"`; domain-config HTTP только для `localhost`, `127.0.0.1`, `10.0.2.2`, `ha4a.pp.ua` |
| Debug manifest | `usesCleartextTraffic="true"` + `tools:replace`; merged manifest содержит оба атрибута |
| Причина cleartext-ошибки | При наличии `networkSecurityConfig` политика задаётся ею; `k4a.pp.ua` не входит в domain-config → HTTP запрещён. Даже debug-флаг не спасает для неразрешённого домена |
| HTTPS | `useHttps` по умолчанию `false`; пользователь может включить, но валидного TLS на 8800 нет |

## 3. DNS diagnostics

Resolver'ы: `192.168.25.240` (не отвечает/недоступен), `192.168.25.1`, `1.1.1.1`.

| Запись | k4a.pp.ua | ha4a.pp.ua |
|---|---|---|
| A (локальный resolver `192.168.25.1`) | `192.168.25.1` | `192.168.25.1` |
| A (публичный `1.1.1.1`) | `91.226.253.235` | `91.226.253.235` |
| AAAA | нет | нет |
| CNAME | нет | нет |
| Статус ответа | NOERROR | NOERROR |

Оба имени указывают на один и тот же публичный IP. Локально оба разрешаются в адрес роутера — это split-horizon DNS или особенность локального resolver; поэтому локальные TCP-результаты для `:8800` не доказывают доступность сервиса извне.

## 4–5. TCP / HTTP / HTTPS / TLS

| Проверка | k4a.pp.ua | ha4a.pp.ua |
|---|---|---|
| TCP 8800 | FAIL (timeout, локально → роутер) | FAIL (timeout, локально → роутер) |
| TCP 443 | OPEN (~3133 мс) | OPEN (~3124 мс) |
| HTTP 8800 `/docs` | timeout | timeout |
| HTTPS 8800 `/docs` | timeout | timeout |
| HTTPS 443 `/docs` | HTTP 404 (nginx), сертификат невалиден | HTTP 404 (nginx), сертификат невалиден |
| HTTPS 443 `/status` | HTTP 404 | HTTP 404 |
| HTTPS 443 `/health` | HTTP 404 | HTTP 404 |
| HTTPS 443 `/` | Home Assistant frontend | Home Assistant frontend |
| TLS версия | TLSv1.3 | TLSv1.3 |
| Сертификат | `CN=ha4a.pp.ua`, SAN `DNS:ha4a.pp.ua` | то же |
| Hostname mismatch | **да** (k4a нет в SAN) | нет |
| Срок | notBefore 2026-08-15, **notAfter 2026-11-13 — истёк** (сегодня 2026-11-20) | то же |
| Issuer | Let's Encrypt `YE1` | Let's Encrypt `YE1` |
| Verify return code | 10 (certificate has expired) | 10 (certificate has expired) |

`curl -k` использовался **только для идентификации сервиса** (nginx/Home Assistant), не для подтверждения безопасности. Стандартная проверка сертификата не отключалась в выводах валидации.

## 6. Фактическая топология (по доступным данным)

- Публичный IP обоих доменов: `91.226.253.235`.
- На `443` завершается TLS на **nginx, отдающем Home Assistant** — это не SunLink backend (ни один из `/docs`, `/status`, `/health` не отдаёт ответ SunLink; `/` — HA frontend).
- `8800` — порт SunLink `deye_gateway.py` по документам репозитория/NAT MikroTik, но из этой сети он недоступен (локальный DNS подменяет имя на роутер; возможен отсутствующий hairpin NAT). Внешнюю доступность 8800 подтвердить отсюда нельзя.
- Корректный домен по сертификату и репозиторию: **`ha4a.pp.ua`**. `k4a.pp.ua` в репозитории и в SAN сертификата отсутствует.
- Где должен завершаться TLS: на reverse proxy перед backend с валидным сертификатом для фактически используемого домена; либо на самом backend с валидным cert. Сейчас TLS на 443 завершается на HA и не relates к SunLink API.
- Локальный endpoint для LAN: `http://192.168.25.20:8800` (по документам, не проверен сетевыми запросами в этом этапе).

## 7. Матрица неопределённостей

Не заполняются предположениями:
- внешняя доступность `8800` (нужна проверка извне LAN);
- конфигурация MikroTik NAT и firewall;
- какой именно процесс слушает `8800` на backend сейчас;
- существует ли отдельный reverse proxy для SunLink (k4a/ha4a) или 8800 проброшен напрямую;
- настройки Nginx/Caddy на сервере.

## 8. Точная причина ошибки Android

1. На устройстве сохранён `k4a.pp.ua` → Retrofit использует `http://k4a.pp.ua:8800/`.
2. `network_security_config.xml` разрешает cleartext только для ha4a/localhost → для k4a HTTP запрещён → `CLEARTEXT communication not permitted`.
3. Даже при исправлении домена на ha4a: `https://ha4a.pp.ua:8800` не работает (TCP timeout отсюда; сертификат для 8800 неизвестен), а `https://ha4a.pp.ua:443` отдаёт Home Assistant с истёкшим сертификатом.

То есть ошибок две: **неверное/неподтверждённое имя хоста в настройках** и **отсутствие рабочего HTTPS-endpoint SunLink с валидным сертификатом**.

## 9. Рекомендованная архитектура HTTPS (для Stage 25, не применялось)

```
Android ──HTTPS(443)──> Reverse proxy (валидный cert, SAN = фактический домен)
                              └──> SunLink backend 127.0.0.1:8800 (HTTP внутри LAN)
```
Альтернатива: терминация TLS на backend с валидным cert и проброс 443. Требования: продлить/перевыпустить сертификат, включить правильный hostname в SAN, не использовать HA как backend, не открывать 8800 в интернет без необходимости.

## 10. Что потребуется изменить

**Сервер (после согласования):** DNS-запись для выбранного домена; валидный TLS-сертификат с SAN; reverse proxy → backend; доступность endpoint; проверить NAT/firewall MikroTik; убедиться, что `/status` и `/docs` backend отвечают через выбранный путь.

**Android (после согласования, НЕ в этом этапе):** добавить выбранный домен в domain-config или перейти на HTTPS (`useHttps=true`); добавить валидацию/подсказку адреса в Settings; убрать рекламу HTTP для неразрешённых доменов; не отключать проверку сертификатов.

## 11. Изменённые файлы и Git

Создан только `docs/stages/stage-25a-connectivity-diagnostic.md` + ссылка в README. Никаких исходников, конфигураций, production-файлов, сертификатов и сетевых настроек не изменялось.

## 12. Подтверждения

- Production backend, `.env`, systemd, Nginx/Caddy, DNS, firewall/MikroTik, сертификаты — не изменялись.
- Ebyte/инвертор/Modbus — не подключались, FC03 не выполнялись, активный map `{184}` не менялся.
- Никаких ping/nmap/сканирований: только перечисленные 4 endpoint и точечные DNS-запросы.
