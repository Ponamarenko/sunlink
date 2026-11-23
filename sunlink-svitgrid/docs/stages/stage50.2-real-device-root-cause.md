# Stage 50.2 — Real-device screenshot root-cause fix

Статус: PASS (код/тесты/APK). Production не изменялся.

## 1. Что подтверждено
- `HEAD = origin/main = e704082e04aaa0c962c4312ba0634ed892da3567`, версия 2.50.1 (72).
- Релизный APK `android-v2.50.1-stage50.1-visual-correction` скачан из GitHub и его
  SHA-256 совпадает с локальной сборкой: `ea9d1ac0...`. В dex присутствуют
  `migrateLegacyThemeMode`, `formatTelemetryTimestamp`, `PremiumDiagnosticsTheme`.
  То есть пользователь действительно получил код Stage50.1, а не старый APK.
- Нового файла скриншота в рабочем окружении нет; признаки-light берутся из текста задания.

## 2. Почему Stage50.1 не изменил скриншот
Stage50.1 мигрировал только `SYSTEM` и некорректные значения. Любое значение `LIGHT`,
сохранённое на устройстве ранее (например, выбор чипа «Светлая» в настройках), оставалось
`LIGHT`. `SvitGridTheme` всё ещё имел ветку `ThemeMode.LIGHT -> false`, поэтому:
- `MaterialTheme.colorScheme = LightColorScheme`,
- `LocalSunLinkColors = sunLinkLightColors()`,
- TopAppBar, Scaffold, PremiumCard, MetricCard и PremiumBottomNavigation рисовали светлые поверхности.

Корневая причина шире, чем «старый SYSTEM»: **корневая тема зависела от пользовательской
настройки вообще**. Пока ветка `LIGHT -> false` существовала, любой сохранённый light-флаг
гарантированно давал светлый Home.

Дополнительно проверено:
- второго `MaterialTheme`/провайдера `LocalSunLinkColors` в runtime нет (только вложенные
  `PremiumAnalyticsTheme` и `PremiumDiagnosticsTheme` на своих экранах);
- `DashboardPreviews` с `ThemeMode.LIGHT` — только previews, в APK на Home не влияет;
- `migrate()` в `SettingsRepository` тему не трогает;
- `enableEdgeToEdge()` и тёмный манифест корректны, но не могут перекрыть светлую Compose-палитру.

## 3. Исправление Stage50.2
- `resolveRootDarkTheme()` возвращает `true` для всех `ThemeMode`; корневая тема физически не
  может стать светлой. `sunLinkLightColors()` остаётся только для Analytics/Diagnostics.
- `MainActivity` создаёт `ThemeController(DARK)` и вызывает миграцию для очистки сохранённых
  `SYSTEM`/`LIGHT`/invalid → `DARK`.
- `SettingsRepository.migrateLegacyThemeMode()` теперь приводит **любое** не-`DARK` значение к `DARK`.
- Токоны приведены к спецификации: border `#24324D`, muted text `#94A3B8`.
- WS: `transportLabel()` показывает `WS OPEN` только для состояния `OPEN`; `CONNECTING`/`RECONNECTING`
  остаются честными и отделены от Freshness телеметрии.
- Load: reg.175 — `S_WORD`, `is_signed=true`, `scale=1.0`. Отрицательное значение является
  подлинным знаковым измерением; `abs()` НЕ применяется, знак сохраняется с пояснением в UI.
- Bottom: сохранён `navigationBarsPadding()` в нижней навигации и увеличенный нижний spacer;
  добавлен Compose-тест прокрутки до карточки Daily Energy.

## 4. Сохранённое
Все маршруты, MainViewModel, REST/WS, DTO, история, графики, Fresh/Stale/Unavailable
и обработка null остаются без изменений. Backend/production не трогались.


## 5. Проверки
- Android unit tests: 200/200 PASS; `lintDebug`, `assembleDebug`,
  `assembleDebugAndroidTest`: PASS. Compose UI test проверяет доступность Daily Energy
  прокруткой.
- APK SHA-256: `d708164441837c5ac264aec5519d364f813aa5e9529c18b6a49da85132176ad9`.
- Версия 2.50.2 (73).
- Реальный экран телефона: NOT VERIFIED (ADB/эмулятор недоступны).
