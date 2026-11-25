# Stage 58 — Design-first visual reconstruction

Статус: PASS (код/тесты/APK/screenshot-рендеры). Production не изменяется.

## 1. Визуальная приёмка
Подключён Paparazzi 1.3.1 (совместим с Kotlin 1.9), изолированные preview-фикстуры в `src/test`, 12 snapshot-тестов. Получены реальные PNG рендеров Compose UI в `app/src/test/snapshots/images/`. Это не физическое устройство; статусыосновано на HTML Stitch + actual Compose render comparison.

| Экран | Stitch эталон | Compose рендер | Статус |
|---|---|---|---|
| Home | Заголовок/Fresh/WS, крест 332px, 3 метрики, daily 3×2 | Сохранена композиция, DEYE-хаб, без огромных кругов | PARTIAL — нет фонового фото Stitch |
| Energy | Coverage, 4 потока, 6 daily | Coverage + 4 потока + 6 daily | PARTIAL |
| Battery | SOC ring, 2×2, daily, area history | SOC ring, 2×2, daily, area history | PARTIAL |
| Solar | PV1/2/3 статусы, area history | 3 канала со статусами из телеметрии, area | PARTIAL |
| Grid | unavailable, 4 строки, exchange, Diagnostics | Аналогично | PARTIAL |
| Load | W hero, 4 статы, area | Аналогично | PARTIAL |
| Analytics | tabs, 3 summary, bar chart, cycles | Аналогично | PARTIAL |
| Diagnostics | connection/quality/faults | Аналогично | PARTIAL |
| More | tune header + 2 группы | Аналогично | PARTIAL |
| Settings | 4 секции, read-only server | Аналогично | PARTIAL |
| Inverter | identity/firmware/link | Аналогично | PARTIAL |
| All Parameters | search + 7 групп | Аналогично | PARTIAL |

MATCH не присвоен ни одному экрану: оригинальные Stitch PNG для покадрового сравнения через MCP недоступны (`screenshot: {}`), а Home background photo требует Google sign-in.

## 2. Исправления, найденные рендерами
- `isConfirmed()` не признавал `physically_verified` —PV1/2/3 и voltage показывались «Очікування»/недоступно. Теперь признаётся.
- `PvInputCard` брал префикс из статуса, поэтому все три PV-карточки читали `pv1`. Теперь префикс передаётся явно.
- Energy coverage падал при 0% PV (`weight(0)`); веса ограничены 1..99.
- Settings requires SharedPreferences in JVM screenshot; previewRepo injected only in tests.
- SOC outliers filtered in Stage57.

Production, backend, Modbus и сеть не изменяются.


## 3. Итоговые проверки
- Unit tests: 272/272 PASS (включая 12 Paparazzi snapshot-тестов).
- `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest`: PASS.
- 12 PNG-рендеров Compose UI сохранены в `app/src/test/snapshots/images/`.
- APK SHA-256: `35d20aac473a9081b054a849404c8263ce879e232d92d5c8940ca9bb651fab30`.
- Версия 2.58.0 (83).
