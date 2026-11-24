# Stage 52 — Information architecture & screen quality

Статус: PASS (код/тесты/APK). Production не изменялся.

## 1. Найденные дубли и неудачные решения
| Экран/функция | Задача | Повтор | Решение |
|---|---|---|---|
| `ForecastScreen` | «Прогноз погоды» | Не имеет weather API: emoji, `15-22°C`, `0` вместо температур, `power_dc/ac` из старого `toMap()` | Удалён из навигации и из исходников; маршрут → home |
| `AboutScreen` | Версия, описание, проверка обновлений | Settings уже показывает версию/сборку и использует `UpdateManager`; About делал второй raw GitHub-запрос | Удалён; версия/лицензия перенесены в Settings; маршрут → settings |
| `DebugScreen` | RAW-данные для разработчиков | Был обычным пользовательским пунктом More | Оставлен в коде, показан только при `BuildConfig.DEBUG`; в release → settings |
| Energy (4 MetricCard + 2 power-графика) | Детальные W-значения | Полностью повторяли Home и Analytics | Удалены; Energy оставлен как баланс kWh + поток + переход в Analytics |
| Home/Energy Daily Energy | Дневная сводка | Home — компактные карточки, Energy — подробные строки kWh | Оставлены оба: разная глубина детализации |
| All Parameters / Telemetry | Сырые значения | Telemetry уже убран из навигации в Stage51 | Alias → all-parameters сохранён |
| Drawer / TopAppBar / 5 старых маршрутов | — | Устранены в Stage51 | Без изменений |

## 2. Итоговая структура
- 5 вкладок: Home / Energy / Battery / Solar / More.
- More (7 пунктов): Grid, Load, Analytics, Diagnostics, Settings, Inverter Details, All Parameters.
  Группы: моніторинг, аналітика, система, технічна інформація.
- Старые маршруты, включая `forecast` и `about`, безопасно перенаправляются.
- Debug доступен только в debug-сборке через прямой маршрут.

## 3. Содержание экранов
- Home — оперативная сводка и кликабельные карточки.
- Energy — подтверждённый поток, дневной баланс kWh, переход к истории/статистике.
- Battery — SOC, доступные электрические параметры, заряд/разряд, история.
- Solar — PV1/2/3, генерация, история.
- Grid — сеть, импорт/экспорт, история `grid_voltage_l1`.
- Load — текущая мощность (сохранён знак), накопленная энергия, история.
- Analytics — 4 периода, реальные графики и статистика.
- Diagnostics — Fresh/Stale/Unavailable, связь, backend, качество, polling.
- Settings — подключение, обновления, версия/сборка/лицензия.

## 4. Сохранённое
MainViewModel, REST/WS, DTO, Fresh/Stale/Unavailable, null-обработка, gap-aware графики,
подтверждённые направления потоков, отрицательная мощность Load без `abs()`.
Тёмная Hybrid Premium палитра и системные панели не изменялись.

Production, backend, Modbus, SQLite и сетевая инфраструктура не затрагивались.


## 5. Проверки и релиз
- Android unit tests: 215/215 PASS.
- Compose UI tests, `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest`: PASS.
- APK SHA-256: `636aa6c5860926699fabe7f8230265d178ea163e519ee70ee3a8d4060f72cf3e`.
- Версия 2.52.0 (75).
- Экран телефона: NOT VERIFIED (нет ADB/эмулятора); скриншоты не создавались.
