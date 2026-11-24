# Stage 53.1 — Analytics & Diagnostics dark theme fix

Статус: PASS (код/тесты/APK). Production не изменялся.

## 1. Точная причина
Оба экрана были обёрнуты в локальные светлые темы, созданные в Stage50 на основе
предположения о «светлой аналитической теме Stitch»:
- `AnalyticsScreen` вызывал `PremiumAnalyticsTheme { ... }`;
- `DiagnosticsScreen` вызывал `PremiumDiagnosticsTheme { ... }`;
- обе функции находились в `SvitGridTheme.kt`, подменяли `LocalSunLinkColors`
  на `sunLinkAnalyticsColors()` и создавали вложенный `MaterialTheme` с
  `lightColorScheme(background = #EDF2F9, surface = #FFFFFF)`.

Это и есть белый фон: не системная тема, не Scaffold и не карточки, а явная локальная
MaterialTheme подмена. Остальные экраны использовали корневую `SvitGridTheme`, поэтому
были тёмными.

## 2. Исправление
- Удалены `PremiumAnalyticsTheme` и `PremiumDiagnosticsTheme`.
- Удалена неиспользуемая палитра `sunLinkAnalyticsColors()` и светлый `LightColorScheme`
  из `SvitGridTheme.kt`.
- `AnalyticsScreen` и `DiagnosticsScreen` работают напрямую в корневой
  `SvitGridTheme` → `DarkColorScheme` + `sunLinkDarkColors()`.
- Третьей темы не создано: в приложении осталась одна Dark Hybrid Premium тема.

## 3. Сохранённое
Пять вкладок, все маршруты, MainViewModel, REST/WS, telemetрия, Fresh/Stale/Unavailable,
gap-aware графики, пользовательские подписи и формат времени из Stage53 — без изменений.
Production,backend, Modbus и сеть не трогались.


## 4. Проверки и релиз
- Android unit tests: 228/228 PASS; Compose UI tests, `lintDebug`, `assembleDebug`,
  `assembleDebugAndroidTest`: PASS.
- APK SHA-256: `55535689483cdc77025fe426214e0f74a952a66fb8de9c6bdbad7e54e5811bc5`.
- Версия 2.53.1 (77).
- Экран телефона: NOT VERIFIED (нет ADB/эмулятора).
