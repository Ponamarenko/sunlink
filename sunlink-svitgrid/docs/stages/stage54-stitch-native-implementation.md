# Stage 54 — Stitch → native Android implementation

Статус: PASS (код/тесты/APK). Production не изменялся.

## 1. Экспорт Stitch
- Архив `stitch_sunlink_solar_monitoring_app(3).zip`: 40 записей, CRC без ошибок.
- Извлечено 19 HTML-файлов: 12 Ukrainian `sunlink_ux_v2_1..12` (основные экраны) и 7 English
  вариантов/дублей; плюс `sunlink_hybrid_energy_design_system.md` с design tokens.
- PNG/SVG отдельными файлами отсутствуют; графика в HTML — SVG/CSS, иконки Material Symbols.

## 2. Карта соответствия
| # | Stitch | Compose | Реализация |
|---|---|---|---|
| 1 | v2_1 Головна | HomeScreen | Stitch content |
| 2 | v2_3 Енергія | EnergyScreen | Stitch content |
| 3 | v2_2 Батарея | BatteryScreen | Stitch content |
| 4 | v2_4 Сонце | SolarScreen | Stitch content |
| 5 | v2_5 Мережа | GridScreen | Stitch content |
| 6 | v2_6 Навантаження | LoadScreen | Stitch content |
| 7 | v2_7 Аналітика | AnalyticsScreen | Stitch content |
| 8 | v2_8 Діагностика | DiagnosticsScreen | Stitch content |
| 9 | v2_9 Більше | MoreScreen | Stitch content |
| 10 | v2_10 Налаштування | SettingsScreen (SvitGridApp.kt) | Stitch content |
| 11 | v2_11 Інвертор | redesign/InverterDetailsScreen.kt | Stitch content |
| 12 | v2_12 Усі параметри | redesign/AllParametersScreen.kt | Stitch content |

## 3. Design tokens
- Colors: `#070B14`, `#0F1726`, `#24324D`, `#F8FAFC`, `#94A3B8`, Solar `#F5B942`,
  Battery `#10B981`, Grid `#3B82F6`, Load `#F59E0B`.
- Radii 6/12/16/20/28 px, spacing 4/8/12/16/24 px, page margin 16 px.
- Typography mapped to available Compose fonts: Mona Sans → SansSerif display,
  Commissioner → SansSerif body, Red Hat Mono → Monospace tabular metrics.
  Font files are not bundled in the export, so system font families are used as fallbacks.
- All 12 screens remain DARK. The Stitch markdown mentions light Analytics/Diagnostics,
  but Stage54 and the user's device acceptance require dark on all 12; the user's
  explicit requirement wins.

## 4. Architecture
MainViewModel, DataRepository, REST/WS, models, Fresh/Stale/Unavailable, provenance,
gap-aware charts and all legacy deep-link redirects are preserved. No fake telemetry.

Production, backend, Modbus, poller and network infrastructure are untouched.


## 5. Проверки и релиз
- Android unit tests: 235/235 PASS; Compose UI tests, `lintDebug`, `assembleDebug`,
  `assembleDebugAndroidTest`: PASS.
- APK SHA-256: `3c7174c1da55570e978101cc9cfef1f59883ab435047767791802ed65a483d03`.
- Версия 2.54.0 (78).
- Визуальное соответствие на устройстве: NOT VERIFIED (нет ADB/эмулятора).
