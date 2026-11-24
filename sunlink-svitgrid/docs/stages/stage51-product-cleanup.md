# Stage 51 — Product cleanup, navigation consolidation, information architecture

Статус:инвентаризация завершён, реализация в процессе. Production не изменяется.

## 1. Инвентаризация (фактический код)

### Основные экраны (redesign)
| Экран | Назначение | Решение |
|---|---|---|
| Home | Оперативная сводка + Energy Flow | Оставить,карточки кликабельны |
| Energy | Энергетический баланс, потоки, история PV/load | Оставить |
| Battery | SOC, параметры, заряд/разряд, история | Оставить |
| Solar | PV1/PV2/PV3, генерация, история | Оставить |
| Grid | Сеть, voltage_l1, частота, импорт/экспорт | Оставить в More |
| Load | Мощность и энергия нагрузки, история | Оставить в More |
| Analytics | 4 периода, 6 графиков, статистика | Оставить в More |
| Diagnostics | Соединение, backend, качество, регистры | Оставить в More |
| More | Единственный список дополнительных разделов | Оставить |

### Найденные дубли/старые маршруты
| Старый маршрут | Что дублировал | Действие |
|---|---|---|
| `dashboard` | Home (Lottie/старый дизайн) | Alias → `home` |
| `pv` | Solar | Alias → `solar` |
| `energy-flow` | Energy | Alias → `energy` |
| `telemetry` | All Parameters (сырые поля) | Alias → `all-parameters` |
| `statistics` | Analytics | Alias → `analytics` |
| `history` | Analytics | Alias → `analytics` |
| `inverter-status` | Inverter Details | Alias → `inverter-details` |
| `live` | Home/телеметрия | Alias → `home`; функция удалена из SvitGridApp |
| Drawer | Повторял 5 вкладок и весь More | Удалён полностью |
| TopAppBar | Дублировал заголовок экрана | Удалён; статус-бар через Scaffold insets |

Итого: 9 дублирующих/устаревших пунктов навигации устранено, 8 старых маршрутов
сохранены как безопасные alias (deep links не ломаются).

### More после очистки
Мониторинг: Grid, Load. Аналитика: Analytics. Система: Diagnostics, Settings, About.
Дополнительно: Inverter Details, All Parameters, Forecast, Debug. Пункты из нижней
навигации в More не повторяются.

## 2. Изменения
- Нижняя навигация: ровно Home / Energy / Battery / Solar / More.
- `HomeScreen(onNavigate)` — карточки PV/Battery/Grid/Load и блок Energy Flow открывают
  соответствующие детальные экраны; дубликаты экранов не создаются.
- Удалены `ModalNavigationDrawer` и `TopAppBar`; системные отступы статус-бара остаются
  через `Scaffold(contentWindowInsets = systemBars.only(Top))`, нижняя панель использует
  `navigationBarsPadding()`.
- Удалена устаревшая функция `LiveScreen` из `SvitGridApp.kt`.
- Исходные файлы старых экранов сохранены для существующих инструментальных тестов,
  но выведены из пользовательской навигации.

## 3. Сохранённое
MainViewModel, REST/WS, DTO, реальные источники, Fresh/Stale/Unavailable, обработка null,
gap-aware графики, подтверждение направлений потоков, знак Load (без `abs()`),
все маршруты-алиасы. Backend и production не трогались.


## 4. Проверки и релиз
- Android unit tests: 207/207 PASS.
- Compose UI tests: навигация из карточек Home и доступность Daily Energy прокруткой.
- `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest`: PASS.
- APK SHA-256: `40524c8177336bccab6ea6ce838dd888d3be67a2764d3030154233ab8cb89551`.
- Версия 2.51.0 (74).
- Внешний вид на устройстве пользователь подтвердил для Stage50.2; Stage51 меняет только
  навигацию/информационную архитектуру, тёмная тема не переделывалась.
