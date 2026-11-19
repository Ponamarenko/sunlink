# Stage 14 — Android APK: аудит, исправления, сборка и подготовка к установке

**Статус:** выполнено (установка на устройство — NOT VERIFIED).
**HEAD на начало:** `a53427a` (= `origin/main`). **Production/оборудование не затрагивались.**

## 1. Исходное состояние

- HEAD `a53427ad1a6717190ba327e56a0a3c96f025bad6`, ветка `main`, source tree чистый;
  незакоммиченные build-артефакты (`.gradle/`, `app/build/`, `*.pyc`) сохранены.
- APK предыдущей сборки: `com.sunlink.svitgrid`, `2.30.0` (50), ~12.3 MiB.
- 49 Android-тестов и 159 backend-тестов проходили; установка на устройство не подтверждалась.

## 2. Android environment (этап A)

| Инструмент | Версия / состояние |
|---|---|
| Java | OpenJDK 21.0.13 (Debian) |
| Gradle Wrapper | 8.9, Kotlin 1.9.23 |
| Android SDK | `/root/Android`, platforms `android-34`, `android-36` |
| Build Tools | 33.0.1 / 34.0.0 / 36.0.0 |
| ADB | 1.0.41 (37.0.1-15733141) |
| Emulator | бинарник `emulator` отсутствует; AVD нет |
| Устройства | `emulator-5554` — **offline**; физических устройств нет |
| Установка APK | невозможна без устройства и отдельного разрешения владельца |

## 3. Результаты аудита (этап B/C)

Что уже работало: Repository + `Result`, ViewModel/StateFlow, DTO по реальному API этапа 12,
Dashboard с `—` для отсутствующих значений, история, DEMO-маркировка, offline/stale,
настройка адреса (в UI), 159 backend + 49 Android тестов.

Найденные реальные проблемы:

1. **Смена адреса backend не действовала.** `RetrofitClient.setBaseUrl()` создавал новый
   `ApiService`, но `DataRepository` и `MainViewModel` хранили ссылку на старый — запросы
   продолжали идти на прежний адрес. Кнопка «Тест підключення» проверяла старый сервер.
2. **Падение при переходе на «Прогноз».** `ForecastScreen()` создавался без factory,
   а `MainViewModel` не имеет конструктора без аргументов → `IllegalArgumentException` в рантайме.
3. **Экран-заглушка «Графіки».** Статичные карточки без данных (дублировал History). Удалён.
4. **Нет валидации URL/порта.** Некорректный ввод сохранялся и мог сломать Retrofit.
5. **Адрес продублирован в трёх местах** (SettingsRepository, RetrofitClient, UI).
6. **Глобальный cleartext traffic** в манифесте — небезопасно для release.
7. **Body HTTP-логирование всегда включено** — лишняя утечка телеметрии в логи.
8. **Поля логина/пароля в Settings** сохранялись в plaintext, но нигде не использовались.

## 4. Выполненные исправления (этап E)

| Файл | Изменение |
|---|---|
| `data/BackendDefaults.kt` (новый) | Единый источник дефолтов: `ha4a.pp.ua`, `8800`, DEMO-адрес |
| `data/SettingsRepository.kt` | Парсинг полного URL (хост/порт/схема), валидация хоста и порта 1..65535, отказ перезаписывать корректные значения некорректными, сохранение `useHttps` (HTTPS для production), убраны мёртвые username/password |
| `network/DelegatingApiService.kt` (новый) | Стабильная шина: все 17 методов делегируют текущему сервису Retrofit |
| `network/RetrofitClient.kt` | `getApiService()` возвращает шину; `setBaseUrl()` отменяет активные запросы старого клиента и пересоздаёт сервис, возвращает `false` при некорректном URL; логирование BODY только в debug |
| `ui/SvitGridApp.kt` | Settings: валидация до сохранения, кнопка «Зберегти та застосувати», отображение текущего backend и ошибки, HTTPS-чекбокс; `ForecastScreen(vm = viewModel)`; удалён stub «Графіки» |
| `AndroidManifest.xml` | `usesCleartextTraffic="false"` + `networkSecurityConfig` |
| `res/xml/network_security_config.xml` (новый) | HTTPS везде; HTTP только для localhost/127.0.0.1/10.0.2.2/ha4a.pp.ua |
| `src/debug/AndroidManifest.xml` (новый) | Debug: HTTP разрешён для любого тестового backend (напр. `192.168.25.20:8801`) |

## 5. Backend integration (этап G)

Изолированный `demo_backend.py`: порт **8801**, SQLite `/tmp/sunlink-stage12-demo.db`,
источник `demo_simulator`. Перед запуском порт был свободен; конфликтующих экземпляров нет.
Реальные HTTP-ответы подтверждены:

- `/` → `read_only: true`;
- `/status` → `source: demo_simulator`, 9 registers, `connection_status: true`;
- `/api/inverters` → `count: 1`, `deye_sg02lp1_am3`;
- `/api/live/raw` → `source: demo_simulator`, `batt_soc: 85.0`, `p_sun: 449.0`,
  `p_grid: null`, `p_load: null`, `inverter_id: deye_sg02lp1_am3`;
- `/api/measurements/latest` → `count: 9`;
- `/api/measurements/history?parameter=battery_soc&...` → `count: 5`, отсортированы по времени.

Production backend, `.env`, systemd, Redis, firewall, HA, Ebyte, инвертор — не трогались.

## 6. UI

Проверено по коду и JVM-тестам: Dashboard (PV/battery/grid/load/SOC/voltage/current/temperature,
`—` и DEMO-баннер, offline/stale), History (реальные имена параметров, сортировка, пустая история),
Inverter Details, Diagnostics, Settings (адрес, порт, HTTPS, тест подключения), About.
Удалён нерабочий экран «Графіки». Устранено падение на «Прогноз».

## 7. Тесты (этап F)

| Набор | Результат |
|---|---|
| Backend (`python3 -m unittest discover -s tests`) | **159 PASS** |
| Android (`./gradlew :app:testDebug`) | **62 PASS** (49 + 13 новых Stage 14) |

Новые сценарии `Stage14IntegrationTest`: первый запуск/дефолты, смена адреса реально меняет endpoint
(два MockWebServer), успешное подключение (status/inverters/telemetry), HTTP 500 сохраняет последние
данные как stale, отсутствие сети без выдуманной телеметрии, восстановление связи, история
(параметр/сортировка/пусто), некорректный URL не ломает конфигурацию.
Дополнительно расширены `SettingsRepositoryTest` (парсинг, валидация, HTTPS, защита от плохого ввода).

## 8. APK (этап H)

| Параметр | Значение |
|---|---|
| Путь | `app/build/outputs/apk/debug/app-debug.apk` |
| Тип | debug |
| Размер | 12 924 277 bytes (~12.3 MiB) |
| SHA-256 | `f9b4a0e1306285969614d6a7e701c8977c891c4818abbd1bcfb2dbd5a30e2645` |
| package | `com.sunlink.svitgrid` |
| versionName / versionCode | `2.30.0` / `50` |
| minSdk / targetSdk | `26` / `34` |
| ABI | чистый Kotlin/Java, нативных библиотек нет → совместим со всеми ABI |
| output-metadata.json | совпадает |

APK отличается от предыдущей сборки по байтам: `BuildConfig` содержит время сборки (`BUILD_TIME`),
поэтому SHA-256 меняется при каждой пересборке даже без изменения исходников. Версия и versionCode
не менялись — это всё те же `2.30.0` (50).

Release APK не собирался: нет подписи, выдуманные ключи не создавались.

## 9. Проверка на устройстве (этап I)

**NOT VERIFIED.** Физического устройства нет; `emulator-5554` offline; бинарник `emulator` и AVD
отсутствуют. APK не устанавливался и не запускался — требуется устройство и явное разрешение владельца.
Успешная компиляция не считается подтверждением работы на устройстве.

## 10. Подготовка APK для владельца (этап J)

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```
В приложении: **Налаштування → Адреса сервера** ввести `192.168.25.20` (или `127.0.0.1:8801`
для эмулятора с пробросом), порт `8800` (или `8801` для демо), снять/поставить HTTPS,
«Зберегти та застосувати», затем «Тест підключення».

Для телефона в той же LAN: адрес должен быть IP хоста с demo backend в этой же сети, порт 8801
должен быть доступен по LAN. Порт в интернет не открывался, firewall не менялся.

Данные demo backend **симулируются** (`source: demo_simulator`) — это не показания физического инвертора.

## 11. Ограничения и оставшиеся задачи

- Установка/запуск на устройстве — NOT VERIFIED (нет устройства/разрешения).
- Release APK и подпись — не создавались (нужен ключ владельца).
- Forecast использует демо-погоду (реальный weather API не подключён) — честно помечено как DEMO.
- `p_grid`/`p_load` остаются `null` (нет регистров в карте этапа 7) — UI показывает `—`.
- Для проверки на телефоне владельцу нужно подтвердить LAN-доступность хоста с backend.

## 12. Следующий этап

Один логичный шаг: **установить debug APK на реальное Android-устройство и провести приёмочный
тест с изолированным backend** (настроить адрес, убедиться в получении телеметрии/истории,
проверить offline/recovery), после чего — подготовить подписанный release APK с ключом владельца.
