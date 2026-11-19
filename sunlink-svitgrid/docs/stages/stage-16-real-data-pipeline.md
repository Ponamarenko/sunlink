# Stage 16 — Подготовка реального pipeline телеметрии Deye

**Статус:** выполнено. Физический Modbus **не запускался**; оборудование не трогалось.
**HEAD на начало:** `5f05ae2da6766ed3443d323befc9d9b00c6e9339`.

## 1. Архитектура до изменений

```
DeyeReadonlyAdapter (своя карта регистров, 24 адреса)
   -> ModbusPollingService -> normalize_register -> SQLite -> deye_gateway API -> Android
DeyeGateway (другая карта — REGISTER_MAP, 9 адресов) -> /api/live/raw -> Android
demo_backend.py (monkeypatch gateway.connect на фейкового Modbus-клиента)
```

Проблемы, найденные аудитом:

1. **Две расходящиеся карты регистров.** `deye_readonly_adapter.py` и
   `domain/register_metadata.py` по-разному описывают одни и те же адреса:
   PV-мощность `U_WORD` vs `S_WORD`; температуры без offset `-100` vs с ним;
   reg 190 помечен как подтверждённый vs `provisional`; адаптер содержит grid
   167/169 и energy DWORD, которых нет в активной карте Data Layer.
2. **DEMO-режим реализован через monkeypatch** `gateway.connect`, а не через
   явный источник — simulated и physical пути не разделены на уровне архитектуры.
3. **`config.yaml` — устаревший шаблон** с write/TOU/email-password/старым IP и портом 8000.
4. **Нет конфигурации выбора источника** — физический Modbus мог активироваться
   неявно (через `DeyeGateway.connect()`).
5. `source` для физического пути жёстко `modbus_register`, а не явная метка `deye_modbus`.

Хорошее, что сохранили: нормализация — чистые функции; timestamp присваивается
в момент чтения; SQLite-репозиторий потокобезопасен; read-only запрет уже есть.

## 2. Аудит карты регистров и уровни достоверности

Официальный мануал Deye (этап 9.2) **не содержит карты регистров**, поэтому
статус `VERIFIED` для AM3 не присвоен ни одному адресу.

| Адрес | Параметр | Тип | Scale | Ед. | Знак | Источник сведений | Статус |
|---:|---|---|---:|---|---|---|---|
| 183 | battery_voltage | U_WORD | 0.01 | V | unsigned | SG02LP1 family + HA log | CORROBORATED |
| 184 | battery_soc | U_WORD | 1.0 | % | unsigned | SG02LP1 family + HA log | CORROBORATED |
| 186 | pv1_power | S_WORD | 1.0 | W | signed | SG02LP1 family | CORROBORATED |
| 187 | pv2_power | S_WORD | 1.0 | W | signed | SG02LP1 family | CORROBORATED |
| 188 | pv3_power | S_WORD | 1.0 | W | signed | SG02LP1 family (10/12 kW) | CORROBORATED |
| 191 | battery_current | S_WORD | 0.01 | A | signed | SG02LP1 family + HA | CORROBORATED |
| 190 | battery_power | S_WORD | 1.0 | W | signed | local adapter vs Sunsynk — знак конфликтует | PROVISIONAL |
| 90 | dc_temperature | S_WORD | 0.1, −100 | °C | signed | Sunsynk/kbialek, нет верификации дисплея | PROVISIONAL |
| 91 | ac_temperature | S_WORD | 0.1, −100 | °C | signed | Sunsynk/kbialek, нет верификации дисплея | PROVISIONAL |

Не включены без отдельного решения: 166/167/169/172/175–178/182/192/194,
76–84 (энергия), 248–279 (TOU), а также 109–114 (PV voltage/current) —
они есть только в старом адаптере и не подтверждены для AM3.

По величинам:

- `p_sun` (сумма PV) — вычисляется из 186/187/188, не прямое измерение;
- `p_batt` — прямой reg 190, знак не подтверждён (отрицание допустимо, смысл — гипотеза);
- `p_grid`, `p_load`, AC voltage, frequency, inverter state — **нет достоверных регистров**,
  остаются `null` и отображаются в Android как `—`;
- SOC, battery voltage/current — прямые измерения;
- температуры — косвенные (offset), из-за этого provisional.

## 3. Новая архитектура источников

```
TelemetrySource (ABC, только read/close — write-методов нет)
  ├─ DemoTelemetrySource      -> source=demo_simulator, локально, без сети
  ├─ MockTelemetrySource      -> source=mock, MockFc03Transport (FC03, без сети)
  └─ ModbusTelemetrySource    -> source=deye_modbus, подготовленный, БЕЗ физического запуска
            |
      TelemetrySnapshot (inverter_id, source, timestamp UTC, connection_ok, values[])
            |
      DeyeGateway.read_all_registers() -> Data Layer (SQLite, source-метка) -> API -> Android DTO
```

Новые файлы:

- `domain/telemetry.py` — `TelemetryValue`, `TelemetrySnapshot`, `TelemetrySource`;
- `data_source_config.py` — безопасный выбор источника;
- `services/telemetry_sources.py` — три реализации;
- `modbus/mock_fc03_transport.py` — программный FC03-транспорт с режимами отказов.

Изменённые: `domain/models.py` (`mock`, `deye_modbus`), `deye_gateway.py`
(принимает `telemetry_source`, сохраняет snapshot в SQLite), `demo_backend.py`
(явный DEMO-режим вместо monkeypatch), `config.yaml` (без write/TOU/паролей).

## 4. Конфигурация

```
SUNLINK_DATA_SOURCE=demo      # по умолчанию; demo | mock | modbus
SUNLINK_ENABLE_MODBUS=1       # обязательно для physical, иначе ошибка
SUNLINK_MODBUS_HOST/PORT/UNIT_ID/TIMEOUT
SUNLINK_POLL_INTERVAL
SUNLINK_REGISTER_PROFILE=stage7
```

Отсутствие конфигурации → demo. Неизвестное значение → `DataSourceConfigError`.
`modbus` без `SUNLINK_ENABLE_MODBUS=1` → отказ с понятным сообщением.
Production `.env` не читается.

## 5. Read-only ограничения

- разрешён только FC03 (`read_holding_registers`);
- write-методов нет в `TelemetrySource`, `MockFc03Transport` и адаптере;
- API не имеет `/write`, `/set-register`, `/control`, `/emergency-stop`;
- mock-транспорт выбрасывает `AttributeError` на любое `write*`;
- polling-сервис уже имел защиту от параллельного опроса (`threading.Lock`) и
  гарантированное закрытие соединения после цикла.

## 6. Результаты mock-тестирования

Новые тесты `tests/test_stage16_pipeline.py` — 20 тестов:

- конфигурация: demo по умолчанию, modbus только с явным флагом,_UNKNOWN → ошибка;
- DEMO/mock/modbus источники возвращают единый `TelemetrySnapshot`;
- набор отказов: timeout, connection refused, Modbus exception, неверный unit ID,
  неверная длина ответа, отсутствующий регистр, повторное подключение;
- отсутствующие значения остаются `None`, а не `0.0`;
- timestamp ISO UTC и свежий для каждого снимка;
- write-интерфейс отсутствует;
- snapshot → SQLite с меткой источника;
- API-контракт и отсутствие write-endpoint.

Физическая сеть/оборудование в тестах не использовались.

## 7. SQLite / API

Реальный HTTP-прогон с изолированным backend (порт 8801, `/tmp/sunlink-stage12-demo.db`):

- `/` → `read_only: true`;
- `/status` → `source: demo_simulator`, `connection_status: true`;
- `/api/live/raw` → `batt_soc=85`, `p_sun=449`, `p_grid=null`, `p_load=null`;
- `/api/inverters` → count 1;
- `/api/measurements/latest` → 9 измерений с `source=demo_simulator`;
- `/api/measurements/history` → точки с `source=demo_simulator`;
- после чтения `/api/live/raw` история выросла с 4 до 5 точек — сохранение работает.

Android DTO не менялись: API совместим. Старые тесты (159 backend, 62 Android) проходят.

## 8. Разделение DEMO / MOCK / PHYSICAL

Метки `demo_simulator`, `mock`, `deye_modbus` сохраняются в каждом измерении и
возвращаются в API; Android уже отображает DEMO-баннер. Нет автоматического
fallback physical → demo: при отказе physical source API честно сообщает об
отсутствии связи, а не подменяет данные симуляцией.

## 9. APK и GitHub Releases

Android-код не менялся, но обязательная проверка `verify_sunlink_release.sh`
собрала новый APK — по постоянному правилу он опубликован:

- `android-v2.30.0-stage16`, prerelease;
- https://github.com/Ponamarenko/sunlink/releases/tag/android-v2.30.0-stage16
- APK: https://github.com/Ponamarenko/sunlink/releases/download/android-v2.30.0-stage16/app-debug.apk
- checksum: https://github.com/Ponamarenko/sunlink/releases/download/android-v2.30.0-stage16/app-debug.apk.sha256
- SHA-256: `f7f61d02c2abffaa273a495f9fdc2628fee40d2be4d6356215f8d144b68e84b3`
- скачанный asset проверен по SHA-256.

## 10. Подготовка к физическому тесту и открытые вопросы RS485

До физического опроса необходимо подтвердить владельцу:

1. Остановлен ли Modbus-клиент Home Assistant (иначе может быть два master на одной шине).
2. Оотносятся ли `192.168.25.14:8899` (HA Sunsynk) и `192.168.25.15:502`
   (Ebyte) к одной физической RS485-шине.
3. Подтверждён ли Modbus unit ID 1 для текущей топологии.
4. Текущие IP/порт/baud Ebyte (заводские — `192.168.3.7:8887`, 115200 8N1).
5. Согласован ли знак reg 190 с показанием дисплея инвертора.
6. Подтверждены ли температуры 90/91 (offset −100) на дисплее AM3.

Пока ничего из этого не проверялось; физические подключения не выполнялись.

## 11. Следующий этап

После письменного подтверждения владельца по RS485 и разрешения на физический
опрос — осторожный read-only FC03-тест на изолированном сегменте с сверкой
9 регистров с дисплеем инвертора, корректировкой карты и отдельным release.
