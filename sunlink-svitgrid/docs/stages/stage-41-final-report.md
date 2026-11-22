# Stage 41 — Real Deye Telemetry Expansion & Full Android Monitoring

**Статус: PARTIAL** — программная реализация, тесты, APK и Release выполнены; физическая FC03-проверка **BLOCKED** ограничениями единственного Modbus master.

## 1. Git
Initial HEAD `f00514e50f60eedd7b5a531ec10a29cb5f93598d` (= `origin/main`). Final HEAD — в финальном ответе. Working tree до начала — только untracked мастер-ТЗ.

## 2. STOP gate физических чтений
Изучены `extended_register_map.py`, `extended_decoder.py`, `build_read_plan()`, `ModbusTcpTransport`, `DeyePollingService`, `deye_gateway.py`.
- Работающий poller — единственный владелец Modbus transport, интервал 30 с, active map `{184}`.
- `POST /read` создаёт второй pymodbus-клиент (небезопасно) и может записать измерение в production SQLite — запрещено.
- Механизма безопасной инъекции разовых запросов в работающий poller нет.
- Остановка `sunlink-https.service` для Stage 41 не разрешена.
**Решение:** ни одного физического запроса не выполнено. Ничего не остановлено, второй master не создан. Карта `{184}` сохранена.

## 3. Реализовано в backend
- `services/extended_polling.py`: `ExtendedPollingConfig.from_env()` — expansion po умолчанию выключен (`SUNLINK_EXTENDED_POLLING=0`). Даже при включении `resolve_addresses()` возвращает только `production_allowed` адреса (сейчас 184). Явно запрошенные 183/186/190 игнорируются (fail closed).
- `build_extended_poller()` создаёт poller только при явном enabled + физическом source; не стартует его; не меняет работающий poller.
- `to_full_dict()` расширена до 10 категорий (добавлены status/warnings/errors/system), всего 80 полей.
- `/api/telemetry/quality` — доступность/качество параметров, active registers, by_confidence; ошибки не маскируются нулями.
- Старые REST/WS контракты сохранены.

## 4. Реализовано в Android
- `AllParametersScreen`: grouping, поиск, фильтры (all/available/verified/unverified/unavailable/stale), value/unit/confidence/timestamp; маршрут `all-parameters`.
- Diagnostics: карточка качества с known/available/unavailable/active registers из `/api/telemetry/quality`.
- `TelemetryQualityDto`, ApiService/Delegating/Repository/ViewModel (`telemetryQuality` state).
- uk/en локализация новых экранов; паритет ключей проверен тестом.

## 5. Состояние данных
Физически подтверждён по-прежнему только `184` (SOC). Все остальные поля в API/UI остаются `null` и честно показываются как «Немає даних». Direct: SOC. Calculated: `pv.total_power` (явная метка).

## 6. Тесты
| Проверка | Статус |
|---|---|
| Backend `unittest discover` | **352 PASS** (7 новых Stage 41) |
| Android `testDebugUnitTest` | **144 PASS** (5 новых) |
| Android lint | PASS |
| `assembleDebug` | PASS |
| `assembleDebugAndroidTest` | PASS (компиляция) |
| `connectedDebugAndroidTest` | NOT RUN (нет устройства) |
| Физические FC03 | **BLOCKED — см. §2** |

## 7. APK
Package `com.sunlink.svitgrid`, versionName **2.41.0**, versionCode **62**, debug, без production signing.

## 8. Production safety
Active map `{184}`, poller, endpoint, `.env`, systemd, SQLite, TLS/DNS/NAT/firewall, MikroTik, Ebyte, инвертор — без изменений. Modbus read/write физически — **NOT RUN**. Deployment — **NOT RUN**.

## 9. План активации расширенного мониторинга (НЕ выполнялся)
1. Уже в коде: 49 register descriptions, decoders, read plan,ExtendedPollingConfig, REST/WS/quality, Android экраны.
2. Физически подтверждён: только 184.
3. Android умеет показывать все 80 полей, но GetCurrent values приходят только для SOC.
4. Из production пока не поступают: 183/190/191/182/186–188/109–114/150/151/169/192 и energy.
5. Для включения нужно отдельное разрешение на maintenance window с остановкой service и последующим FC03-тестом приоритетных адресов.
6. Затем — точечное добавление `production_allowed=True` и обновление env/кода с backup и rollback.
7. Активация только после сверки с HMI и повторного согласования.

## 10. Ограничения
Compose UI-тесты не запускались на устройстве. Визуальная проверка пользователем — через Release APK.
