# Stage 13 — Git credential security + Release Verification

**Статус:** выполнено. **Ветка:** `main`. **HEAD на начало:** `21f654e` (= `origin/main`).
**Production, HA, Redis, systemd, firewall, Ebyte, инвертор — не затрагивались.**

## 1. Исходная проблема

Локальная Git-конфигурация remote `origin` содержала GitHub Personal Access Token
непосредственно в URL (HTTPS basic-auth: `https://<credentials>@github.com/Ponamarenko/sunlink.git`).
Такой токен виден всем, кто может прочитать `.git/config`, попадает в вывод команд и рискует
утечь в логи/отчёты. **Значение токена в этом отчёте и в терминале не приводится.**

Тип обнаруженной проблемы: GitHub PAT, встроенный в URL remote (не в файлах репозитория).

## 2. Выполненные действия

1. Проверено реальное состояние: HEAD `21f654e`, ветка `main`, локальный `main` = `origin/main`.
   Рабочее дерево содержало только несвязанные build-артефакты (`.gradle/`, `app/build/`) — они сохранены.
2. Проверен SSH-доступ к GitHub **без раскрытия private key**:
   `ssh -T -o BatchMode=yes git@github.com` → успешная аутентификация (`Hi Ponamarenko!`).
   SSH-ключ был создан ранее; новый ключ не создавался.
3. `origin` переключён на SSH: `git@github.com:Ponamarenko/sunlink.git`.
4. Проверено, что URL больше не содержит credentials (fetch и push URL), а `origin/main`
   доступен через `git ls-remote` (hash совпадает с локальным).
5. Никакие посторонние credentials и настройки не изменялись. Не выполнялись reset, rebase, force push.

**Важно:** очистка URL **не отзывает** ранее использованный PAT.

## 3. Что должен сделать пользователь

**Old PAT считается НЕ отозванным — подтверждения пользователя нет.**

Самостоятельно откройте GitHub → **Settings → Developer settings → Personal access tokens**
(and/or Tokens (classic)) и отзовите старый токен. После этого подтвердите в чате, что отзыв выполнен —
до этого момент считать отзыв подтверждённым нельзя.

Для дальнейшей работы уже настроен безопасный SSH-доступ. Альтернатива — безопасный credential manager
(например, `git config --global credential.helper libsecret` на Linux / Keychain на macOS / Credential Manager
на Windows), но настройка plaintext credential store (`store` с файлом в открытом виде) не рекомендуется.

## 4. Release verification script

Создан `scripts/verify_sunlink_release.sh` (исполняемый). Запуск:

```bash
./scripts/verify_sunlink_release.sh
```

Скрипт автоматически:
1. Определяет git-корень и каталог Android-проекта (проект лежит в подкаталоге `sunlink-svitgrid/`).
2. Проверяет наличие backend/Android компонентов.
3. Запускает backend unit tests (`python3 -m unittest discover -s tests`).
4. Запускает Android unit tests (`./gradlew :app:testDebug`).
5. Выполняет `./gradlew assembleDebug`.
6. Проверяет существование APK и его размер (sanity-диапазон; ориентир ~13 MB).
7. Извлекает метаданные через `apkanalyzer` (если недоступен — безопасная альтернатива:
   `output-metadata.json`) и сверяет их между APK и метаданными сборки.
8. Проверяет package `com.sunlink.svitgrid`, непустой versionName и положительный versionCode.
9. Завершается ненулевым кодом при любой ошибке; выводит результат каждого шага.

Скрипт **не** жёстко требует старые числа тестов/версию: он проверяет успешность тестов и фактические
метаданные APK. При желании конкретную версию можно проверить через env:
`EXPECT_VERSION_NAME=2.30.0 EXPECT_VERSION_CODE=50 ./scripts/verify_sunlink_release.sh`.

### Фактический результат запуска

| Проверка | Результат |
|---|---|
| Компоненты | все найдены |
| Backend tests | **159 PASS** |
| Android tests | **49 PASS** |
| assembleDebug | BUILD SUCCESSFUL |
| APK | `app/build/outputs/apk/debug/app-debug.apk`, 12 896 126 bytes (~12.3 MiB) |
| package | `com.sunlink.svitgrid` |
| versionName / versionCode | `2.30.0` / `50` |
| Metadata | совпадает с `output-metadata.json` |
| **ИТОГО** | **RELEASE VERIFICATION: PASS** |

## 5. Ограничения автоматической проверки

- Скрипт проверяет только JVM/backend тесты и сборку; установка APK на устройство не выполняется
  (эмулятор `emulator-5554` в статусе offline, бинарник `emulator` отсутствует).
- Размер APK проверяется по sanity-диапазону, а не по точному значению.
- Скрипт не проверяет подлинность подписи release-сборки (собирается debug APK).
- Скрипт не обращается к сети/оборудованию и не зависит от production `.env`.

## 6. Изменённые файлы

- `scripts/verify_sunlink_release.sh` — создан, chmod +x.
- `docs/stages/stage-13-git-security-and-release-verification.md` — создан.
- `README.md` / `CHANGELOG.md` — добавлены ссылка Stage 13 и команда запуска.

Git remote изменён локально (в `.git/config`, не в файлах репозитория) — в коммит не входит.
