# Stage 50.1 — Visual correction after on-device Stage50

Статус: PASS (код/тесты/APK). Production не изменялся.

## 1. Аудит установленного Stage50
- `HEAD = origin/main = 6cf7e9aaadc591c6efe65bb88b592ad5bef4d7e7`, `main` синхронизирован.
- `versionName = 2.50.0`, `versionCode = 71`; release tag
  `android-v2.50.0-stage50-hybrid-premium` указывает на тот же commit. APK,
  опубликованный в релизе, собран из этого состояния (SHA-256 совпадает с локальной сборкой).
- В рабочем окружении нет нового скриншота с телефона; доступен только старый скриншот
  от 20.11 со светлым интерфейсом до Stage49. Исправления выполнены по текстовому
  описанию фактических симптомов от пользователя.

## 2. Точная причина светлого интерфейса на устройстве
1. `SettingsRepository.themeModeName` читает сохранённое значение из SharedPreferences.
   На устройстве со старой установкой там осталось `"SYSTEM"` (или некорректное значение),
   потому что новый дефолт `"DARK"` применяется только к свежей установке.
2. `MainActivity` читает именно сохранённое значение:
   `runCatching { ThemeMode.valueOf(settings.themeModeName) }.getOrDefault(ThemeMode.SYSTEM)`.
   При `"SYSTEM"` и светлой системной теме Android `SvitGridTheme` выбирал
   `LightColorScheme` + `sunLinkLightColors()` — поэтому карточки, нижняя навигация
   и весь контент Home были светлыми, хотя манифест уже был тёмным.
3. Дополнительно `SvitGridTheme` имел дефолтный параметр `ThemeMode.SYSTEM` и
   `LocalThemeController = SYSTEM`, то есть даже при отсутствии сохранённого значения
   существовал путь к светлой палитре.
4. Манифест и window background были исправлены ещё в Stage50, но Compose-палитра
   перекрывала их для контента — отсюда «светлый интерфейс в тёмном окне».

Вывод: дело не в том, что пользователь «включил светлую тему», а в миграции сохранённой
настройки темы и в дефолтах `SYSTEM` на трёх уровнях.

## 3. Исправления
- `SettingsRepository.migrateLegacyThemeMode()`: `SYSTEM` и некорректные значения
  один раз при запуске заменяются на `DARK`; явный выбор `LIGHT` сохраняется.
- `MainActivity`: использует миграцию, fallback `DARK`, принудительно задаёт светлые
  иконки системных статус/навигации через `WindowCompat.getInsetsController`.
- `SvitGridTheme`: дефолт `DARK`, `ThemeMode.SYSTEM -> true` (не может вернуть светлую
  палитру), `LocalThemeController = DARK`.
- `values-v27/themes.xml`: `windowLightNavigationBar=false`.
- `TopAppBar` в `SvitGridApp`: один рядок заголовка — устранено обрезание.
- `formatTelemetryTimestamp()`: `23 ноя, 13:12:06` вместо ISO; точный timestamp остаётся в модели.
- `DataFreshnessIndicator(compact=true)` и компактный WS-индикатор на Home.
- `SunLinkScreen`: нижний content spacer увеличен до 32 dp; `PremiumBottomNavigation`
  сохраняет `navigationBarsPadding()` и тёмный фон.
- Analytics остаётся светлой; Diagnostics получил отдельную светлую техническую тему
  через `PremiumDiagnosticsTheme`.

## 4. Сохранённое
MainViewModel, API client, REST/WS, DTO, все маршруты, History/Stats, Fresh/Stale/Unavailable,
обработка null, графики с разрывами — без изменений. Backend и production не трогались.


## 5. Проверки
- Android unit tests: 194/194 PASS.
- `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest`: PASS.
- Новые тесты: миграция темы из SYSTEM,-human timestamp, тёмные системные панели,
  отсутствие SYSTEM fallback, светлые темы Analytics/Diagnostics, заголовок Home,
  нижние отступы, версия 2.50.1.
- APK SHA-256: `ea9d1ac0ab9df7abf5b64e82182ca662addc026653f658853e11ae32e1663c66`.
- Внешний вид на физическом устройстве: NOT VERIFIED (ADB/эмулятор недоступны).
