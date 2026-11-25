# Stage 56 — direct Stitch-to-Compose port

Статус: реализация в процессе. Production не изменяются.

## 1. Отказ от Stage 55
Пользователь отверг универсальные `HeroPanel`/`StatTile`/`DataPanel`/`InlineMetric`. Stage 56 удаляет эти абстракции из 11 экранов и портирует каждый экран напрямую из Stitch HTML (`node_...` в `projects/17811413119272333045`).

## 2. Источник
Stitch MCP: `get_project`, `list_screens`, `get_screen`, `download_file`. Скриншоты через MCP недоступны (`screenshot: {}`), HTML — основной источник. Фоновое изображение Home (`solar_home_dusk.jpg`) через MCP не выдаётся, прямой Googleusercontent URL требует входа; вместо него сохранена градиентная-подложка, что зафиксировано как ограничение.

## 3. Порт по экранам
Home — заголовок с tune-иконкой, отдельная строка Fresh/WS, схема 332dp с узлами 0.22/0.5/0.77 и DEYE-хабом, 3 компактные метрики, панель «За сьогодні» 3×2.
Energy — coverage bar, 4 потока, 6 daily-строк.
Battery — SOC hero, 2×2 параметры, daily rows, SOC area chart.
Solar — PV hero, PV1/PV2/PV3, generation area chart.
Grid — unavailable status, 4 параметра, import/export, Diagnostics link.
Load — hero W, 4 статы, load area chart.
Analytics — 4 периода, 3 summary, bar chart, cycles.
Diagnostics — connection/quality/faults + REST/provenance.
More — tune header, primary rows, detail monitoring.
Settings — 4 секции, read-only server, persistence.
Inverter — identity/connection/firmware unavailable, Diagnostics link.
AllParameters — search, 7 категорий, provenance/freshness.

## 4. Данные
Только существующие `MainViewModel`/`DataRepository`/REST/WS. Отсутствующие значения — «—»/«Недоступно», знак Load без `abs()`, направление потоков только подтверждённое.Random/demo нет.

Production, backend, Modbus и сеть не изменяются.


## 5. Проверки и релиз
- Android unit tests: 254/254 PASS; Compose UI tests, `lintDebug`, `assembleDebug`,
  `assembleDebugAndroidTest`: PASS.
- APK SHA-256: `93a150b47f806e4429c4559b39b797ed1c28c841de666746e7b266908d4b32e7`.
- Версия 2.56.0 (81).
- Визуальная проверка на устройстве: NOT VERIFIED (нет ADB/эмулятора).
