# Stage 31 — Premium Design System & Professional Dashboard

**Итог: `STAGE 31 — PASS`** (реализация, тесты, сборка, prerelease). Device Verification: **NOT VERIFIED** — физического устройства/эмулятора нет; владелец проверяет APK самостоятельно.

## 1. Git

- Исходный HEAD: `a6894c12e435c02d5b44f1ea243c0c7bead236e4` (= `origin/main`, Stage 30 report).
- Коммит кода: `0ae3a41221bac75d916d64693bab63cc393a95f2` — `Stage 31: premium design system and dashboard redesign`.
- Отчёт добавляется следующим docs-коммитом; push без force, `HEAD = origin/main`.
- untracked `мобильное приложение.txt` (мастер-ТЗ) не изменялся и в коммит не включался.

## 2. Изменённые файлы

**Design System (новые):** `ui/designsystem/SunLinkColors.kt`, `SunLinkTypography.kt`, `SunLinkDimensions.kt`, `DashboardState.kt`, `Components.kt`.

**UI:** `ui/theme/SvitGridTheme.kt` (System/Light/Dark), `MainActivity.kt` (ThemeController), `SettingsRepository.kt` (+ `theme_mode`), `ui/SvitGridApp.kt` (нижняя навигация + переключатель темы), `ui/dashboard/DashboardScreen.kt` (полная переработка), `ui/dashboard/DashboardPreviews.kt` (6 Preview).

**Ресурсы (новые):** `res/values/strings.xml` (uk), `res/values-en/strings.xml` (en).

**Тесты:** `ui/designsystem/Stage31DesignSystemTest.kt` (новый), `data/SettingsRepositoryTest.kt` (+ тема).

**Прочее:** `app/build.gradle.kts` (versionCode 55 / versionName 2.34.0), `README.md`, `CHANGELOG.md`, этот отчёт. APK и build-артефакты в Git не добавлены.

## 3. Созданные компоненты Design System

Все 17 функциональных элементов присутствуют: `SunLinkBackground`, `SunLinkTopBar`, `EnergyMetricCard`, `PowerValue`, `StatusIndicator`, `ConnectionStatusBadge`, `BatterySOCIndicator`, `EnergyFlowDiagram`, `EnergyFlowNode`, `EnergyFlowConnection`, `SectionHeader`, `TelemetryValue`, `LastUpdatedLabel`, `EmptyTelemetryState`, `LoadingState`, `OfflineState`, `ErrorState`.

Централизованы: цвета (24 семантические роли в двух палитрах), типографика (`display/headline/title/metric/body/label/caption`, числовые стили с `fontFeatureSettings="tnum"`), отступы 4/8/12/16/24 dp, радиусы 10/16/22 dp, elevation, размеры иконок, 48 dp touch target, длительность анимации.

## 4. Реализованные элементы Dashboard

- Верхняя панель: знак/логотип SunLink, название, подтверждённая модель инвертора, бейдж состояния, время обновления.
- Блок системного статуса: SOC-кольцо, состояние инвертора, генерация и нагрузка — только из реальных полей.
- Энергетическая схема с пятью узлами (Solar/Inverter/Battery/Grid/Home).
- Карточки PV/Battery/Grid/Load с иконками из `Icon/`, мощностью и «Немає даних» при `null`.
- Детальная телеметрия: PV1–3, V/I/P батареи, температуры; provisional помечается `*`.
- Состояния: Loading, Online, Offline, Stale, Connection error, Connecting, Unknown + пустое состояние.
- Время последнего обновления — только из серверной метки времени.

Удалены все выдуманные показатели (погода, «без света 16 мин», фиктивный timeline) — в новом Dashboard их нет.

## 5. Dark / Light / System

- Отдельные палитры, а не инверсия: Deep graphite (`#0B1014`) для Dark, светло-нейтральный (`#F3F6F8`) для Light.
- Semantic energy colors одинаковы по смыслу в обеих темах (solar/battery/grid/load/error/warning).
- `ThemeMode.SYSTEM/LIGHT/DARK` хранится в SharedPreferences; `ThemeController` применяет тему без перезапуска через `CompositionLocalProvider`.
- Material3 colorScheme для обеих тем, общие Typography/Shapes.

## 6. Схема энергетических потоков

`EnergyFlowDiagram` — BoxWithConstraints + Canvas. Пять узлов позиционируются долями ширины/высоты, поэтому схема читается на 320/360/430 dp (карточки на <360 dp перестраиваются в два ряда). Соединения рисуются между узлами.

Ключевое правило честности: `EnergyFlowConnection.isActive = power != null && directionConfirmed && abs(power) > 0.1f`. Поскольку backend пока не подтвердил семантику знака для `p_batt`/`p_grid`, `directionConfirmed = false` — направление **не угадывается**, потоки нейтральны и не анимируются. Анимация частиц включается только при подтверждённом направлении, ненулевой мощности, состоянии ONLINE и выключенном Reduce Motion (`ANIMATOR_DURATION_SCALE`). При OFFLINE/STALE анимация остановлена.

## 7. Неизвестные значения

`null` остаётся `null`: `PowerValue` печатает «—», карточки — «Немає даних», `hasMeasurements == false` показывает отдельное состояние. Нигде нет подмены `null → 0`. Старые данные при потере связи показываются с бейджем Stale и не выдаются за свежие.

## 8. Сохранённая интеграция

Не изменялись: `TelemetryWebSocketClient`, `WebSocketDto`, `MainViewModel` (ONLINE только после реального обмена), REST fallback, reconnect/backoff, stale detection. Stage 30 тесты (WebSocket/REST/стали) проходят без изменений. Android не обращается к Modbus/Ebyte; второго poller нет.

## 9. Lottie

Все 12 Lottie-ассетов Stage 30 остаются в APK. В production-дашборде намеренно **не используется ни одна** анимация: нет подтверждённой семантики знака мощности, поэтому заряд/разряд и направление потоков рисовать нельзя; декоративная анимация была бы ложным индикатором. Lottie остаются доступными для будущих состояний с подтверждёнными данными. reduce motion поддерживается для потоковой анимации.

## 10. Тесты

| Набор | Результат |
|---|---|
| Backend `python3 -m unittest discover -s tests` | **276 PASS** |
| Android `./gradlew :app:testDebugUnitTest` | **105 PASS**, 0 failures/errors/skipped |
| `assembleDebug` | BUILD SUCCESSFUL |
| Compose Preview | 6 штук: Dark Online, Light Online, Offline, Stale, Loading, неполная телеметрия — все с явно изолированными `PREVIEW_ONLY` данными |

Новые тесты (12): палитры Dark/Light и все семантические роли, tabular figures, все состояния маппера, `null` ≠ `0`, сохранение реальных значений, потоки не угадывают знак / не анимируют ноль и неизвестное, ONLINE только после обмена, три режима темы, паритет uk/en ключей, отсутствие `Random`/mock в production-исходниках, изоляция preview-данных, сохранение темы в настройках.

Compose UI-тесты не добавлены: в текущей Gradle-конфигурации нет `androidx.compose.ui:ui-test-junit4`, добавление новой зависимости не требуется заданием; визуальная проверка выполнена через Preview + локальный HTML-предпросмотр.

## 11. Сборка и APK

- Package ID: `com.sunlink.svitgrid`
- VersionCode: **55**, VersionName: **2.34.0**
- Размер: **13 090 723** bytes
- SHA-256: `1094abf918628006fd6d99b2d16ee7dbbba7fcc0a044b6bb121a99a92c87c825`
- Сборка: debug, prerelease, не production-signed.
- GitHub Release: `SunLink Stage 31 — Premium Dashboard (Prerelease)`, tag `android-v2.34.0-stage31-debug`, prerelease.
- URL: https://github.com/Ponamarenko/sunlink/releases/tag/android-v2.34.0-stage31-debug
- APK asset: https://github.com/Ponamarenko/sunlink/releases/download/android-v2.34.0-stage31-debug/app-debug.apk

## 12. Device Verification

**NOT VERIFIED.** ADB-устройство и эмулятор отсутствуют (`adb devices` пусто, AVD/system image нет). APK публикуется для самостоятельной установки владельцем. Реальный UI на телефоне и живой скриншот не проверялись и не заявляются.

## 13. Ограничения / следующий этап

- Направление заряда/разряда и импорта/экспорта остаётся «неизвестным», пока backend не подтвердит семантику знаков; тогда можно включить анимированные потоки.
- Частота сети, суточная генерация/потребление, уведомления и полноценный экран «Керування» — вне объёма Stage 31.
- Compose UI-тесты требуют добавления `ui-test-junit4` отдельным этапом.
- Production-инфраструктура не изменялась: `.env`, systemd, БД, MikroTik, NAT/firewall/DNS, TLS, Ebyte, инвертор, Modbus write, второй poller, deployment — всё без изменений.

**Итог: `STAGE 31 — PASS`.**
