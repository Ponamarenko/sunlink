# Stage 54.2 — Stitch visual fidelity fix

Статус: PASS (код/тесты/APK). Production не изменялся.

## 1. Что исправляется
Stage 54 перенёс структуру и содержание 12 экранов, но визуальный язык остался упрощённым:линейные графики без заливки, line chart вместо Stitch bar chart, круглый хаб вместо квадратного DEYE, сплошные карточки без border/blur.

## 2. Источник
Stitch MCP (`get_project`, `get_screen`, `download_file`, `list_design_systems`) подтвердил доступ к проекту `17811413119272333045` и 12 UX v2 экранам. Оригинальный проект не изменяется. HTML Stitch использован как источник SVG-геометрии, градиентов, обводок и типографики.

## 3. Реализованные общие компоненты
- `Charts.kt`: `AreaChart` (gradient fill 0.28→0, dashed gridlines, endpoint marker, gap-aware segments) и `AnalyticsBarChart` + `alignBarBuckets` (парные rect-бары, timestamp alignment, dashed placeholder).
- `DesignSystem.kt`: `PremiumEnergyFlow` переписан по Stitch — прямые линии, arrowheads только для подтверждённых направлений, квадратный хаб DEYE, узлы Сонце/Батарея/Мережа/Будинок. `PremiumCard` — полупрозрачная поверхность, 1px border, тень, без верхней 3px полосы.
- Шрифты Mona Sans / Commissioner / Red Hat Mono загружены с официальных Google Fonts URL, указанных в Stitch HTML, и добавлены в `res/font`.

## 4. Экраны
Battery / Solar / Load — AreaChart с цветами `#10B981` / `#F5B942` / `#F59E0B`.
Analytics — AnalyticsBarChart вместо двух линий.
Grid — кликабельная ссылка на Diagnostics.
Home — новый поток через обновлённый PremiumEnergyFlow.

Сохраняются реальные данные, Fresh/Stale/Unavailable, gap-aware обработка, знак Load без `abs()`, тёмная тема и пять вкладок.

Production, backend, Modbus и сеть не изменяется.


## 5. Проверки и релиз
- Android unit tests: 244/244 PASS; Compose UI tests, `lintDebug`, `assembleDebug`,
  `assembleDebugAndroidTest`: PASS.
- APK SHA-256: `949a3fb206b7c251c7672d3b13bf562cb9122025fa1c42116d342fe8013379d8`.
- Версия 2.54.1 (79).
- Визуальная проверка на устройстве: NOT VERIFIED (нет ADB/эмулятора).
