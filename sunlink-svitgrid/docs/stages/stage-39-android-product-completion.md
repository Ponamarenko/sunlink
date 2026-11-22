# Stage 39 — Android Product Completion & Data Integration

**Статус: PASS (Android-изменения, тесты, сборка, Release). Production не изменялся.**

## 1. Исходное состояние
HEAD `5f69e1d63260898c1ba3885df149ba774d663dcc` (= `origin/main`), versionName 2.38.0 / versionCode 59. Working tree — только untracked мастер-ТЗ.

## 2. Карта API → DTO → UI
Backend REST `/api/telemetry/full` → `TelemetryFullDto` → `MainViewModel.fullTelemetry` → `TelemetryScreen`, `BatteryDetailScreen`, `PvDetailScreen`, `GridDetailScreen`, `LoadDetailScreen`, `EnergyFlowScreen`.
WebSocket `/ws` → `LiveDataMessageDto` → `onLiveDataMessage()` → `InverterUiState` + (новое) `LiveDataFullMapper` → `fullTelemetry`.
History `/api/measurements/history` → `MeasurementsHistoryDto` → `HistoryUiState` → History/Statistics charts.

## 3. Интегрированные поля
Доступно физически: `battery_soc` (reg 184, `source=deye_modbus`) — SOC на Dashboard и Battery. Все остальные категории (PV/Grid/Load/Battery V-I-P/T/Inverter) остаются в модели как `null` и честно отображаются «Немає даних»/«—». Расчётное `pv.total_power` маркируется `calculated`.

## 4. исправленные проблемы
1. **WS не обновлял категориальную модель** — экраны Battery/PV/Grid/Load не получали новые данные без REST. Добавлен `LiveDataFullMapper` с явным whitelist соответствий (`WsToFullFieldMap`); неизвестные поля игнорируются.
2. **REST fallback при WS-reconnect обновлял только общий UI state** — теперь дополнительно вызывается `loadFullTelemetry()`.
3. **История не сортировалась и содержала дубликаты** — добавлены `sortedBy` + `distinctBy` по timestamp.
4. **Нет индикации загрузки полной телеметрии** — добавлен `fullTelemetryLoading`, экраны показывают CircularProgressIndicator, затем ошибку с кнопкой повтора.
5. **`onFullTelemetryLoaded` не сбрасывал ошибку** — исправлено.

## 5. WebSocket
Один клиент, `startLiveUpdates()` вызывается один раз в `SvitGridApp`; WS-кадр теперь обновляет и `uiState`, и `fullTelemetry`. Reconnect/backoff и REST fallback сохранены из Stage 29/32. Дублирующихся подписок нет (проверено тестами клиента Stage 30).

## 6. History/Statistics
Сортировка по времени, дедупликация, честные пустые состояния, периоды 1h/6h/24h/7d/30d (History) и 24h/7d/30d/365d (Statistics). Графики не соединяют пропуски.

## 7. Тесты
| Проверка | Статус |
|---|---|
| Backend `unittest discover` | 330 PASS (контракт не менялся) |
| Android `testDebugUnitTest` | **135 PASS** (5 новых Stage 39) |
| Android lint | PASS |
| `assembleDebug` | PASS |
| `assembleDebugAndroidTest` | PASS (компиляция UI-тестов Stage 38) |
| `connectedDebugAndroidTest` | NOT RUN — нет устройства/эмулятора |

Новые тесты: merge WS→full telemetry, VM обновляет оба состояния, сортировка/дедупликация истории, stats из пустой выборки, null ≠ 0.

## 8. APK
Package `com.sunlink.svitgrid`, versionName **2.39.0**, versionCode **60**, debug, без production signing.

## 9. Production safety
Не изменялись: active map `{184}`, poller, endpoint, `.env`, systemd, SQLite, TLS/DNS/NAT/firewall, Ebyte, инвертор. Modbus read/write — NOT RUN. Deployment — NOT RUN.
