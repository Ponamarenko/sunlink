# Stage 49 — Android UX/UI Redesign

Статус: код/тесты в процессе. Production deployment НЕ выполняется и не планируется.

## Цель
Профессиональный тёмный энергетический интерфейс на реальной производственной телеметрии,
без изменений backend, БД, сети, опроса и регистров.

## Реализовано
- Новая дизайн-система `ui/redesign/DesignSystem.kt`:
  - `SunLinkScreen` с градиентным фоном и заголовком,
  - `SunLinkCard` с акцентной полосой,
  - `MetricCard`, `StatusPill`, `ArcGauge`, `Sparkline`,
  - `EnergyFlowCanvas` с импульсами только для подтверждённых значений; направление потока
    не показывается там, где сервер его не подтверждает.
- `ui/redesign/TelemetryHelpers.kt`:
  - `telemetryNumber()` — строго `normalized_value` из `/api/telemetry/full`,
  - `isConfirmed()` — только `verified`/`corroborated`/`calculated`, не stale/unavailable,
  - `seriesSegments()` — разрывы на `null`/NaN/Inf/gap вместо нулей.
- Экраны: Home (Dashboard), Energy, Battery, Solar, Grid, Load, Analytics,
  Diagnostics, More. Все читают один общий `MainViewModel`/StateFlow.
- Навигация в `SvitGridApp.kt`: 5 вкладок Home / Energy / Battery / Solar / More,
  групованный drawer (`drawer_monitoring`, `drawer_analytics`, `drawer_system`, `drawer_other`),
  все прежние экраны сохранены как вложенные маршруты.
- `MainActivity`: edge-to-edge (status bar / nav bar) с системной темой.
- Версия: versionName `2.49.0`, versionCode `70`.
- Зависимость `material-icons-extended` для иконок вместо эмодзи.

## Ограничения и честные состояния
- Отсутствующие поля отображаются как «—» / «Немає даних», никогда как 0.
- `provisional`, `verified`, `corroborated`, `calculated`, `stale`, `unavailable`
  не смешиваются; статус берётся из DTO.
- Графики строятся только по `/api/history/series`; пропуски — разрывами.
- Имена параметров истории сверены с production `/api/telemetry/catalog` и реальными
  ответами `/api/history/series`: `grid_power` и `battery_power` не имеют точек,
  поэтому Grid использует `grid_voltage_l1`, а Energy не запрашивает неподдерживаемые ряды.
- Температуры/аварии инвертора, ток/мощность/импорт сети,конфликт reg 190 —
  остаются недоступными и не активируются.
- Подтверждение направления потоков сервером отсутствует — анимация показывает
  только активность узла, не стрелку направления.

## Тесты
- `app/src/test/java/com/sunlink/svitgrid/ui/redesign/Stage49RedesignTest.kt`:
  mapping/null/stale/quality/gap, ViewModel `loadSeriesFor`, навигация, версия.
- Все предыдущие Android unit-тесты сохраняются.
- UI на устройстве/эмуляторе: NOT VERIFIED до появления доступного устройства.

## Deploy
Не выполняется. APK публикуется в GitHub Releases как debug/prerelease.
