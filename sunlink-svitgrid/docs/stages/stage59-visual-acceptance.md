# Stage 59 — Visual acceptance of Stitch UX v2

Статус: визуальный результат подготовлен, ожидает приёмки пользователем. APK/Release не публикуются до приёмки.

## 1. Источники
- Stitch MCP: project `17811413119272333045`, 12 UX v2 HTML nodes, CSS classes, SVG, DOM structure.
- Per-screen Stitch PNG недоступны (`screenshot: {}`); фоновое фото Home требует Google sign-in и не было получено.
- Paparazzi 1.3.1: 12 реальных PNG-рендеров Compose UI (Pixel 5) в `app/src/test/snapshots/images/`.

## 2. Preview fixtures
Изолированные демонстрационные данные в `src/test` повторяют Stitch-макеты:
PV 148 W / PV1 148 W / PV2 standby / PV3 unavailable, SOC 99%, Load 525 W, Grid unavailable, Analytics 06:00–10:00, peak 610 W. Production-телеметрия не изменяется.

## 3. Таблица соответствия

| Экран | Stitch -> Compose | Статус |
|---|---|---|
| Home | tune-шапка, Fresh/WS, 332px cross, 3 метрики, daily 3×2 | PARTIAL (нет фонового фото Stitch) |
| Energy | coverage 28%, 4 потока, 6 daily | PARTIAL |
| Battery | SOC 99%, 2×2 параметры, daily, area SOC | PARTIAL |
| Solar | PV1 confirmed, PV2 standby, PV3 unavailable, area 610 W peak | PARTIAL |
| Grid | unavailable banner, 4 строки, exchange, Diagnostics | PARTIAL |
| Load | 525 W hero, 4 статы, area history | PARTIAL |
| Analytics | День/Тиждень/Місяць/Рік, 3 summary, bar 06:00–10:00, cycles | PARTIAL |
| Diagnostics | connection/quality/faults, REST/provenance | PARTIAL |
| More | tune header, primary + detail monitoring | PARTIAL |
| Settings | 4 секции, read-only server | PARTIAL |
| Inverter | identity/connection/firmware unavailable | PARTIAL |
| All Parameters | search, 7 категорий | PARTIAL |

MATCH не присвоен: нет Stitch PNG для покадрового сравнения и нет физического устройства.

## 4. Проверки
`recordPaparazziDebug`: BUILD SUCCESSFUL, 12 snapshot-тестов PASS. APK/Release не публикуются до визуальной приёмки.

Production, backend, Modbus, poller, сеть и TLS не изменялись.
