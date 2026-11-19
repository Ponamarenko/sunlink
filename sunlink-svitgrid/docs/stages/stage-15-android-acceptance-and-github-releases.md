# Stage 15 — Android device acceptance & GitHub Releases

**Статус:** выполнено. Приёмка на устройстве: **NOT VERIFIED**. Публикация APK: **VERIFIED**.
**HEAD на начало/конец кода:** `ec9cf6726a662d79b56bcbd1296603e0302e6124`.
**Production/оборудование не затрагивались.**

## 1. Исходное состояние

- HEAD `ec9cf67` = `origin/main`, ветка `main`, источники чистые; build-артефакты сохранены.
- Remote `origin` — SSH (`git@github.com:Ponamarenko/sunlink.git`), без credentials в URL.
- APK Stage 14: `app-debug.apk`, `com.sunlink.svitgrid`, `2.30.0` (50), 12 924 277 bytes,
  SHA-256 `f9b4a0e1306285969614d6a7e701c8977c891c4818abbd1bcfb2dbd5a30e2645`.
- 159 backend + 62 Android тестов PASS; `verify_sunlink_release.sh` PASS.

## 2. Android environment и устройства

Java 21.0.13; Gradle 8.9 / Kotlin 1.9.23; Android SDK `/root/Android` (34/36); ADB 1.0.41.

`adb devices -l` → **список пуст**. Бинарник `emulator` отсутствует, AVD нет.
Физического устройства нет, поэтому установка и приёмка на устройстве невозможны
и требуют отдельного разрешения владельца. Все сценарии Stage D — **NOT VERIFIED**
(не имитировались как успешные).

## 3. Demo backend

Изолированный `server/demo_backend.py`: порт **8801**, SQLite `/tmp/sunlink-stage12-demo.db`,
источник `demo_simulator`. Порт перед запуском был свободен, конфликтующих экземпляров нет.

Проверено реальными HTTP-запросами:

| Endpoint | Результат |
|---|---|
| `/` | `read_only: true` |
| `/status` | `source: demo_simulator`, 9 регистров, `connection_status: true` |
| `/api/inverters` | `count: 1`, `deye_sg02lp1_am3`, DEMO |
| `/api/live/raw` | `batt_soc=85.0`, `p_sun=449.0`, `p_grid=null`, `source=demo_simulator` |
| `/api/measurements/latest` | `count: 9`, все с `source=demo_simulator` |
| `/api/measurements/history` | `count: 5`, `"DEMO sample … - not live data"` |

Это **симуляция**, не показания инвертора. Production backend, HA, Redis, systemd,
firewall, MikroTik, Ebyte и инвертор не трогались.

## 4. Проверка приложения

Поскольку устройства нет, реальная приёмка (D1–D7) не проводилась. Выполнены автоматические
эквиваленты: 62 Android unit/integration теста Stage 14 covering первый запуск/дефолты,
настройку и фактическую смену endpoint, успешное подключение, HTTP 500, отсутствие сети,
восстановление, историю и некорректный URL. Это не заменяет установку на устройство.

## 5. GitHub Releases — постоянное правило

**Каждый собранный APK публикуется в GitHub Releases `Ponamarenko/sunlink`; APK не добавляется в Git.**

Доступ: `gh` 2.60.0, аутентификация через `GH_TOKEN` (scope `repo`), токен в логи не выводился.
Remote остался SSH. Существующие релизы и теги (`v2.30.0` и др.) не изменялись.

### Опубликованный релиз Stage 14

| Параметр | Значение |
|---|---|
| Название | `SunLink Android 2.30.0 — Debug (Stage 14)` |
| Tag | `android-v2.30.0-stage14` (проверен как свободный до создания) |
| Статус | **prerelease** (debug НЕ production) |
| Ссылка | https://github.com/Ponamarenko/sunlink/releases/tag/android-v2.30.0-stage14 |
| APK asset | https://github.com/Ponamarenko/sunlink/releases/download/android-v2.30.0-stage14/app-debug.apk |
| Checksum asset | https://github.com/Ponamarenko/sunlink/releases/download/android-v2.30.0-stage14/app-debug.apk.sha256 |
| Commit в notes | `ec9cf6726a662d79b56bcbd1296603e0302e6124` |
| Размер APK | 12 924 277 bytes |
| SHA-256 | `f9b4a0e1306285969614d6a7e701c8977c891c4818abbd1bcfb2dbd5a30e2645` |

Release notes содержат версию, version code, тип debug, commit, результаты тестов,
ограничения, предупреждение о симулированных DEMO-данных и NOT VERIFIED для устройства.

### Проверка после загрузки

- Release существует, `isPrerelease=true`;
- assets: `app-debug.apk` (размер совпадает) и `app-debug.apk.sha256` (80 bytes);
- APK скачан из Releases, SHA-256 скачанного файла совпал с исходным;
- checksum asset скачан и содержит тот же hash;
- Сравнение с Stage 14: SHA-256 совпадает — опубликован тот же бинарник, что проверялся в Stage 14.

## 6. Механизм публикации

`scripts/publish_android_release.sh` (исполняемый). Запуск:

```bash
./scripts/publish_android_release.sh
# или с явными идентификаторами:
TAG=android-v2.30.0-stage14 TITLE="SunLink Android 2.30.0 — Debug (Stage 14)" \
  ./scripts/publish_android_release.sh
```

Скрипт: находит корень проекта; проверяет APK, package и версию через `apkanalyzer`;
вычисляет SHA-256 и создаёт checksum-файл; определяет тип сборки; через `gh` проверяет
аутентификацию, существование tag/release; создаёт уникальный tag и prerelease для debug;
загружает APK + checksum; проверяет статус, размеры, наличие assets, скачивает APK и сверяет
SHA-256; выводит прямые ссылки. Повторный запуск не удаляет и не перезаписывает релиз —
сообщает, что он уже опубликован, и выполняет проверку.

Переменные: `APK_PATH`, `REPO`, `TAG`, `TITLE`, `NOTES`. Токены в файлы не записываются.

### Правило для будущих этапов

После каждой сборки APK: тесты → сборка → метаданные → SHA-256 → GitHub Release →
загрузка APK и checksum → проверка assets → передать ссылку владельцу. Этап, создающий
новый APK, не считается завершённым без публикации (если нет документированной причины).

## 7. Подписанный release APK

Signing config в Gradle отсутствует, keystore в репозитории нет — фиктивный ключ не создавался,
production-подпись не имитировалась. Для signed release нужны от владельца: keystore (.jks),
store password, key alias, key password — через безопасный канал, не в чат и не в Git.
Пока используется debug prerelease.

## 8. Ограничения и оставшиеся задачи

- Установка/запуск на реальном устройстве — NOT VERIFIED (нет устройства и разрешения).
- Signed release APK — не собирался (нет ключа владельца).
- Дальнейшие сборки должны публиковаться в Releases по описанному правилу.

## 9. Следующий этап

При установке устройства и получении разрешения владельца — провести D1–D7 приёмку
debug APK с demo backend; затем, после предоставления ключа, подготовить подписанный
release APK и стабильный Release (не prerelease).
