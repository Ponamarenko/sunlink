# Stage 50 — Hybrid Premium design-to-Android implementation

Статус: PASS (код/тесты/APK). Production не изменялся.

## 1. Обязательный аудит (до изменений кода)

### Git и исходные материалы
- `HEAD = origin/main = fbe852660e54150adb8662529c71cfc9f2c41589`, ветка `main`, рабочее дерево чистое
  (кроме пользовательского файла `мобильное приложение.txt`, который не трогается).
- В рабочем окружении **нет** ZIP-экспорта Google Stitch, `DESIGN.md` и скриншота с девятью макетами.
  Единственный найденный скриншот — `Screenshot_20261120-172917_SunLink.png` от 20.11: это
  светлый экран диагностики **до Stage49**, а не макет Stitch и не установленный Stage49.
- Точное попиксельное соответствие Stitch поэтому **не может быть подтверждено** без архива.
  Реализация опирается на текстовую спецификацию Hybrid Premium; доступный скриншот используется
  только как подтверждение проблемы со светлой темой.

### Причина визуального несоответствия Stage49
Установлена, а не предположена:

1. `SettingsRepository.themeModeName` по умолчанию `SYSTEM`. На устройстве со светлой системной
   темой `SvitGridTheme` выбирает `LightColorScheme` и `sunLinkLightColors()` — поэтому «тёмная
   дизайн-система» Stage49 на таком устройстве физически не применялась.
2. `AndroidManifest.xml` жёстко задаёт `@android:style/Theme.Material.Light.NoActionBar` и для
   `<application>`, и для `<activity>`. Даже при edge-to-edge платформенное окно, статус-бар и
   системная навигация остаются светлыми; до первого кадра Compose виден белый фон.
3. В Stage49 не было принудительного тёмного дефолта: пользователь должен был вручную выбрать
   «Тёмная» в настройках. Скриншот 20.11 демонстрирует именно светлый вариант интерфейса.
4. Визуальные компоненты Stage49 были функциональными, но не премиальными: `EnergyFlowCanvas`
   рисовал четыре круга с тонкими линиями, `SunLinkCard` — плоские карточки, нижняя навигация —
   стандартный `NavigationBar`, типографика MaterialTheme была моноширинной. Это не Hybrid Premium.

### Карта девяти экранов
| Экран | Сейчас (Stage49) | Требуется Stitch / Hybrid Premium | Изменение |
|---|---|---|---|
| Home | 4 круга, 180 dp, плоские карточки, стандартная навигация | Крупный Energy Flow с хабом, цветными линиями и подписями источников, крупные значения | Новый hero-блок, premium-карточки, кастомная нижняя навигация |
| Battery | SOC-дуга, 8 метрик, 3 графика | Крупный круговой SOC, заряд/разряд, история, четкие unavailable | Анимированный SOC, сгруппированные заряд/разряд, freshness |
| Solar | Hero + 3 PV-карточки + 3 графика | Отдельные PV1/2/3, напряжение/ток, генерация, сводка | Premium-карточки каналов, единые графики и статусы |
| Grid | Подписанная мощность,voltage/current/frequency, импорт/экспорт | Фазы (если доступны), частота, импорт/экспорт, история | Группировка по фазам, electric-blue семантика, freshness |
| Load | Hero мощность, cards, история | Разделение текущей мощности и накопленной энергии | Усиленная иерархия, warm-orange акцент, аналитическая сводка |
| Energy | Flow + 4 метрики + дневная сводка | Поступление и распределение с историей | Полноценный поток и баланс с подтверждёнными данными |
| Analytics | 3 Sparkline + stats, тёмная тема | Светлая аналитическая тема, Daily/Weekly/Monthly/Yearly | Локальная светлая тема, 4 периода, ANALYTICS-графики |
| Diagnostics | Список InfoRow | Fresh/Stale/Unavailable, backend, регистры, ошибки | StatusIndicator, группировка, явные состояния |
| More | Список маршрутов | Настройки/диагностика/инвертор/доп. разделы | Premium-список с иконками и актуальным маршрутом |

### Архитектура, сохраняемая без изменений
`MainViewModel`, `DataRepository`, Retrofit/OkHttp, WebSocket, DTO, все маршруты и legacy-экраны
остаются на месте. Stage50 — только Android UI/theme/navigation/tests.

## 2. Реализованные основы (до экранов)

### Исправление причины светлой темы
- `SettingsRepository.themeModeName` по умолчанию `DARK` (был `SYSTEM`).
- `AndroidManifest.xml` использует `@style/SunLinkPlatformTheme` вместо светлого
  `Theme.Material.Light.NoActionBar`; добавлены `res/values/colors.xml` и `themes.xml`
  с тёмным `windowBackground`, `statusBarColor`, `navigationBarColor` и
  `windowLightStatusBar=false`.
- `MainActivity` сохраняет `enableEdgeToEdge()`; системные панели теперь тёмные с первого кадра.
- Динамические цвета Material You не используются: палитра полностью детерминированная.

### Hybrid Premium токены и компоненты
- Палитра: Deep Navy `#070B14`, Dark Blue Surface `#0F1726`, Emerald Battery `#10B981`,
  Solar Gold `#F5B942`, Electric Blue Grid `#3B82F6`, Warm Orange Load `#F59E0B`.
- `PremiumTokens` — единые радиусы и отступы; `SunLinkTypography` остаётся с tabular figures.
- `PremiumCard`, `PremiumSectionHeader`, `PremiumMetricCard` (через `MetricCard(emphasize)`),
  `DataFreshnessIndicator` + `freshnessOf`, `PremiumEnergyFlow`/`EnergyFlowCanvas`,
  `BatterySocIndicator`, `AnalyticsChart`, `PowerPill`, `EmptyState`,
  `PremiumBottomNavigation`. Старые имена компонентов сохранены как алиасы.
- Направления потоков вычисляются только из подтверждённого знака мощности:
  `pvFlowDirection`, `gridFlowDirection`, `loadFlowDirection`; батарея — `UNKNOWN`
  из-за конфликта масштаба reg. 190.
- `PremiumAnalyticsTheme` — локальная светлая тема только для Analytics.

### Навигация
Пять вкладок Home / Energy / Battery / Solar / More через `PremiumBottomNavigation`
с анимированным выделением и системными отступами; все прежние маршруты сохранены
в drawer и в `NavHost`. Переходы между экранами — fade.

## 3. Состояние экранов после реализации
| Экран | Реализовано |
|---|---|
| Home | PremiumEnergyFlow с хабом, цветными линиями, подписями PV/Battery/Grid/HOME и направлением только для подтверждённых PV/Grid/Load; Fresh/Stale индикатор; крупные PV и SOC карточки; Battery/Grid/Load; дневная энергия |
| Battery | Анимированный SOC-кольцо 196dp; электрические параметры; заряд/разряд с kWh и unavailable; SOC/voltage/current history; 24h/7d/30d |
| Solar | Hero total power; отдельные PV1/PV2/PV3 карты с voltage/current/power и confidence; per-channel history; 24h/7d/30d |
| Grid | Electric-blue семантика; импорт/экспорт; voltage_l1/frequency/current; дневная и общая энергия; история `grid_voltage_l1`; 24h/7d/30d |
| Load | Hero power; раздел W и kWh; voltage/current; история `load_power`; 24h/7d/30d |
| Energy | PremiumEnergyFlow с хелперами направлений; баланс; дневная энергия; gap-aware история PV/load |
| Analytics | Локальная светлая тема; Daily/Weekly/Monthly/Yearly; ANALYTICS-графики с сеткой; statistics; пропуски разрывами |
| Diagnostics | Fresh/Stale/Unavailable/Offline индикаторы; connection/backend/quality/registers/events; polling cycles/failed/missed сохранены |
| More | Премиум-список всех маршрутов с иконками, секциями и подсветкой текущего |

## 4. Ограничения
- ZIP-экспорт Google Stitch и `DESIGN.md` в окружении отсутствуют; скриншот от 20.11 — это
  светлый интерфейс до Stage49. Попиксельное соответствие макетам **NOT VERIFIED**.
- ADB/эмулятор недоступны; инструментальные Compose-тесты компилируются, но на устройстве
  не запускались. Финальная визуальная проверка выполняется пользователем через APK.
- Backend, БД, Modbus, systemd и сетевая конфигурация не изменялись.


## 5. Проверки и релиз
- Android unit tests: 187/187 PASS; `lintDebug`, `assembleDebug`,
  `assembleDebugAndroidTest`: PASS. Compose UI tests добавлены для всех девяти экранов.
- APK SHA-256: `5253c94ce75f1c83300e984aa4af3651266be2a9f90221098379d4554f7e2a4a`.
- Релиз: `android-v2.50.0-stage50-hybrid-premium`, debug/prerelease.
- Визуальное совпадение со Stitch и поведение на устройстве: NOT VERIFIED
  (нет экспорта Stitch и нет ADB/эмулятора).
