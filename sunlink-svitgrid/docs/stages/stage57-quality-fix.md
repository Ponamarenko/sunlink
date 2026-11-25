# Stage 57 — Data quality and visual defect fixes

Статус: реализация в процессе. Production не изменяется.

## 1. Исправленные ошибки телеметрии
- `battery_soc` history с production содержит выбросы 6634.8% и 6511.2%. Раньше они попадали в `max` и вызывали «Макс 6634%». Добавлены `isValidSoc()` и `sanitizedSocSeries()`: значения вне 0..100%, NaN/Inf и `gap` превращаются в пропуски, а не обрезаются по границе и не попадают в min/max.
- Solar PV1/PV2/PV3 статусы были захардкожены. Теперь `pvVariant()` вычисляет статус из реальных полей `pv{pv1,pv2,pv3}_power/voltage/current` с учётом confidence и stale. Состояния «Підтверджено / Очікування / Недоступно» отражают реальную телеметрию.
- Общая PV-мощность остаётся значением `/api/telemetry/full` (`calculated`), не подменяется суммой и не исправляется искусственно.

## 2. Визуальные исправления
- Удалены огромные полупрозрачные круги в `PremiumEnergyFlow`; вместо них тонкий horizon-gradient, не перекрывающий схему. Energy Flow остаётся центральным элементом Home.
- Сохранена индивидуальная Stitch-композиция 12 экранов из Stage 56.

## 3. Проверка и ограничения
Добавлены unit-тесты на физически невозможный SOC, gaps, динамические PV-статусы и отсутствие декоративных кругов. ADB/эмулятор недоступны (`emulator-5554 offline`, AVD нет), поэтому реальные Android-скриншоты получить нельзя. Pixel-perfect и визуальная проверка на телефоне не заявляются.

Production, backend, Modbus, poller, сеть и TLS не изменяются.


## 4. Проверки и релиз
- Android unit tests: 258/258 PASS; Compose UI tests, `lintDebug`, `assembleDebug`,
  `assembleDebugAndroidTest`: PASS.
- APK SHA-256: `bcf101acd7c4a98d1062bfda805fd216ece08bc96edb4ca12eb9d0af01748154`.
- Версия 2.57.0 (82).
- Визуальная проверка на устройстве: NOT VERIFIED (нет ADB/эмулятора).
