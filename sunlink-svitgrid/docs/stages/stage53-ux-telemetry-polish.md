# Stage 53 — Real-device UX polish & telemetry presentation

Статус: PASS (код/тесты/APK). Production не изменялся.

## 1. DNS-ошибка на Battery — локализация
- Источник строки: `MainViewModel.loadFullTelemetry()` сохраняет `Result.Error.exception.message`
  в глобальный `fullTelemetryError` (общий для Home/Battery/Solar).
- Battery показывает этот общий `fullTelemetryError` как красную ошибку страницы.
- Отдельного HTTP-клиента,альтернативного base URL, второго WS или второго poller у Battery нет:
  `loadFullTelemetry()` и `loadSeriesFor()` идут через тот же `DataRepository`/Retrofit.
- Причина на устройстве: transient DNS-сбой при REST-запросе `/api/telemetry/full`.
  WebSocket продолжает доставлять свежие кадры; `onFullTelemetryLoaded()` очищает ошибку,
  но следующий неудачный REST снова её выставляет. На Home в момент скриншота ошибки не было,
  на Battery — была. Это не сетевая инфраструктура и не backend, а способ показа общей ошибки.

### Исправление
- `telemetryErrorNote(error, hasFreshTelemetry)`: если свежая телеметрия есть, показывается
  компактное сообщение «REST тимчасово недоступний · дані з WebSocket» вместо глобальной красной
  ошибки; если данных нет — исходное сообщение остаётся ошибкой.
- raw-сообщение и технический `provenance` перенесены в Diagnostics.
- `Fresh` не показывается, если `full.stale` или UI-state stale.

## 2. Пользовательские подписи
- `telemetryLabel()` расширена: physically_verified → «Підтверджено вимірюванням»,
  model_compatible → «Сумісне з моделлю», verified → «Перевірено», и т.д. Уровень достоверности
  не повышается: model_compatible не называется физически подтверждённым.
- `freshnessLabel()` на украинском: «Дані актуальні», «Дані застаріли», «Попередні дані»,
  «Немає даних», «Немає з'єднання».
- Технические идентификаторы доступны в Diagnostics через `telemetryProvenance()`.

## 3. Единое время
`formatTelemetryTimestamp()` используется в `DataFreshnessIndicator` для всех экранов и явно
в Battery. Home/Energy/Battery/Solar/Grid/Load больше не показывают ISO. Diagnostics оставляет
сырой ISO как техническую подробность.

## 4. Home
- Убран технический счётчик «28/28 parameters»; количество параметров осталось в Diagnostics.
- При неполном наборе PV/Battery/Grid/Load подпись потока становится «Баланс частково доступний».
- Направления по-прежнему только для подтверждённых PV/Grid/Load; батарея — UNKNOWN.
- Нули не подставляются, `abs()` не применяется.

## 5. Сохранённое
Пять вкладок, тёмная Hybrid Premium палитра, MainViewModel, REST/WS, DTO, gap-aware графики,
все маршруты-алиасы. Production, backend, Modbus, сеть и инвертор не трогались.


## 6. Проверки и релиз
- Android unit tests: 223/223 PASS; Compose UI tests, `lintDebug`, `assembleDebug`,
  `assembleDebugAndroidTest`: PASS.
- APK SHA-256: `ca1a95ad917b6d61ffd8edc3008f05068d79a0156c27474d28aa9058c3e04b61`.
- Версия 2.53.0 (76).
- Реальный экран этого APK: NOT VERIFIED (нет ADB/эмулятора).
