# Этап 5 — Исследование модели Sunsynk и сравнение с картой Deye SG02LP1-AM3

**Статус:** выполнено. Исследование проведено 17 ноября 2026.  
**Объект:** кроссплатформенная библиотека Sunsynk (kellerza/sunsynk, GitHub main) — сравнение модели датчиков и адресов регистров с read-only адаптером Deye SUN-12K-SG02LP1-EU-AM3.  
**Записьだけ чтение:** никаких write-операций, код не изменён, физический опрос не повторялся (используются результаты этапов 3 и 4).

## 1. Модель датчика Sunsynk (src/sunsynk/sensors.py)

### `Sensor.reg_to_value(regs)`
```python
val = unpack_value(regs, signed=self.factor < 0)
val = int_round(float(val) * abs(self.factor))
```

Ключевой вывод: **`factor` отвечает одновременно за масштаб и знак**:
- `factor > 0`: регистр декодируется как **unsigned**, значение = raw × factor
- `factor < 0`: регистр декодируется как **signed** (two's complement), значение = signed_raw × |factor|

То есть отрицательный `factor` у Sunsynk — это не инверсия результата, а приведение регистра к знаковому 16-битному декодированию.

### `TempSensor` (используется для температур)
```python
class TempSensor(Sensor):
    offset: int = 100
    def reg_to_value(self, regs):
        return int_round((float(regs[0]) * abs(self.factor)) - self.offset)
```
Температура у Sunsynk всегда декодируется как `raw × factor − 100` (offset по умолчанию 100).

### `SensorDefinitions`
Сборник датчиков профиля инвертора. Поддерживает `copy()`, `__add__`, `override()`. DWORD-регистры передаются кортежем адресов, например `Sensor((63, 64), ...)`.

## 2. Сопоставление адресов: Sunsynk single_phase.py vs Deye SG02LP1 adapter

Sunsynk `single_phase.py` — профиль однофазного гибрида (Dye-like, 5/8 кВт). Адреса совпадают с нашей картой SG02LP1 в большинстве случаев.

| Рег | Sunsynk single_phase.py | Sunsynk micro_hybrid_ess.py | Deye SG02LP1 adapter (этапы 3-4) | Совпадение адреса | Совпадение масштаба/типа |
|-----|-------------------------|-----------------------------|-----------------------------------|-------------------|--------------------------|
| 90 | TempSensor "DC transformer temperature", CELSIUS, 0.1, offset=100 | — | S_WORD "dc_temperature", 0.1 °C | ✓ | ✗ (offset) |
| 91 | TempSensor "Radiator temperature", CELSIUS, 0.1, offset=100 | — | S_WORD "ac_temperature", 0.1 °C | ✓ | ✗ (offset) |
| 108 | Day PV energy, KWH, 0.1 | — | U_WORD "daily_pv_production", 0.1 kWh | ✓ | ✓ |
| 109 | PV1 voltage, VOLT, 0.1 | — | U_WORD "pv1_voltage", 0.1 V | ✓ | ✓ |
| 110 | PV1 current, AMPS, 0.1 | — | U_WORD "pv1_current", 0.1 A | ✓ | ✓ |
| 111-114 | PV2-PV3 voltage/current | — | U_WORD PV2-PV3 V/A, 0.1 | ✓ | ✓ |
| 167 | Grid LD power, WATT, -1 | — | S_WORD "grid_ct_l1_power", 1.0 W | ✓ | ≈ (scale ×1 vs ×10 для 169) |
| 168 | Grid L2 power | — | (не в карте Deye) | ✗ | — |
| 169 | Grid power, WATT, -1 | — | S_WORD "grid_total_power", 10.0 W | ✓ | ≈ (×10 vs ×1) |
| 172 | Grid CT power | — | (не в карте Deye) | ✗ | — |
| 175 | Inverter power | — | (не в карте Deye) | ✗ | — |
| 176-178 | Load L1/L2/Total power | — | (не в карте Deye) | ✗ | — |
| 182 | Battery temperature, TempSensor 0.1 | — | **Исключён** (unconfirmed для AM3) | ✓ | — |
| 183 | Battery voltage, VOLT, 0.01 | Battery voltage, VOLT, 0.01 | U_WORD "battery_voltage", 0.01 V | ✓ | ✓ |
| 184 | Battery SOC, "%" | Battery SOC, "%" | U_WORD "battery_soc", 1.0 % | ✓ | ✓ |
| 186 | PV1 power, WATT, -1 | — | U_WORD "pv1_power", 1.0 W | ✓ | ≈ (znak) |
| 187 | PV2 power, WATT, -1 | — | U_WORD "pv2_power", 1.0 W | ✓ | ≈ (znak) |
| 188 | PV3 power, WATT, -1 | — | U_WORD "pv3_power", 1.0 W | ✓ | ≈ (znak) |
| 189 | PV4 power | — | (не в карте Deye) | ✗ | — |
| 190 | Battery power, WATT, -1 | Battery power, WATT, -1 (разряд=+) | S_WORD "battery_power", 1.0 W | ✓ | ≈ (znak) |
| 191 | Battery current, AMPS, -0.01 | Battery current, AMPS, -0.01 (заряд=−) | S_WORD "battery_current", 0.01 A | ✓ | ≈ (znak) |
| 72-73 | Total battery charge, KWH, 0.1 | Total battery charge, KWH, 0.1 | U_DWORD (96-97 для PV) | ✓ | ✓ |
| 74-75 | Total battery discharge, KWH, 0.1 | Total battery discharge, KWH, 0.1 | U_DWORD | ✓ | ✓ |
| 96-97 | Total PV energy, KWH, 0.1 | — | U_DWORD "total_pv_production", 0.1 kWh | ✓ | ✓ |
| 166 | Gen power | — | **Исключён** (этап 4) | ✓ | — |
| 76-82 | Day grid import/export, Month energies | — | **Исключён** (этап 4) | ✓ | — |
| 248-279 | Program timers (Prog) | Program timers | **Исключён** (этап 4) | ✓ | — |

**Вывод:** все 19 адресов из проверенной карты Deye SG02LP1 имеют прямые аналоги в Sunsynk `single_phase.py` (и/или `micro_hybrid_ess.py`). Адреса 166, 182, 76–82, 248–279 — реально присутствуют у Sunsynk, но корректно исключены из опроса etapa 4 по инструкции.

## 3. Отличия в знаковых конвенциях (важное)

Sunsynk использует `factor < 0` для знакового декодирования регистра, но **не инвертирует** результат (умножение на |factor|):

| Рег | Sunsynk single_phase | Sunsynk комментарий (micro_hybrid_ess) | Deye adapter (этап 4) | Различие |
|-----|---------------------|----------------------------------------|----------------------|----------|
| 190 Battery power | WATT, -1 | "positive = discharging" | S_WORD ×1.0, +434 W = "заряд" | **Знак противоположен**: у Deye + = заряд, у Sunsynk + = разряд |
| 191 Battery current | AMPS, -0.01 | "negative = charging" | S_WORD ×0.01, +0.81 A | Знак не уточнён у Deye |
| 169 Grid power | WATT, -1 | (отрицательно = экспорт) | S_WORD ×10.0, 0 W | Аналогично |

Пример с данными этапа 4 (reg 190 = raw 434 = 0x01b2):
- Sunsynk `reg_to_value`: signed=434, value = 434 × 1 = **+434 W** → по конвенции Sunsynk это **разряд**.
- Deye adapter: +434 W интерпретировалось как **заряд** (положительное = заряд).

**Вывод:** знак Battery Power требует уточнения против дисплея инвертора. На этапе 4 экран показывал "Battery Discharge Power = 2426 W" при Modbus +434 W — существенное расхождение, время снимка не синхронизировано.

## 4. Температура reg 90/91 — подтверждение offset −100

Sunsynk `single_phase.py`:
```python
TempSensor(90, "DC transformer temperature", CELSIUS, 0.1),   # raw × 0.1 − 100
TempSensor(91, "Radiator temperature", CELSIUS, 0.1),        # raw × 0.1 − 100
```

Данные этапа 4 (reg 90 = 1365, reg 91 = 1424):

| Рег | Raw | Sunsynk TempSensor (×0.1 − 100) | Deye adapter (×0.1) | Más реалистично |
|-----|-----|---------------------------------|---------------------|-----------------|
| 90 DC | 1365 | 136.5 − 100 = **36.5 °C** | 136.5 °C | 36.5 °C (радиатор на солнце) |
| 91 AC | 1424 | 142.4 − 100 = **42.4 °C** | 142.4 °C | 42.4 °C (радиатор на солнце) |

**Вывод:** модель Sunsynk подтверждает вариант `raw × 0.1 − 100` (kbialek offset). Значения 36.5/42.4 °C физически реалистичны для радиаторов инвертора; вариант без offset (136.5/142.4 °C) нереалистичен. Поля экрана инвертора (батарейный бокс 20.8 °C, отдельный экран 22.2 °C) относятся к другим датчикам, не к радиаторам DC/AC.

## 5. DWORD и порядок слов

Sunsynk передаёт 32-битные регистры кортежем адресов, например `Sensor((63, 64), "Total active energy", KWH, 0.1)` и `Sensor((72, 73), "Total battery charge", KWH, 0.1)`. Внутренний декод `unpack_value(regs)` использует `struct.unpack("<2H", ...)` — **младшее слово zuerst (LW, HW)**.

Это подтверждает порядок LW,HW в нашем `read_dword_pair` (reg 96-97, 72-73, 74-75), независимо проверенного на этапе 3 через kbialek/deye-inverter-mqtt.

## 6. Подтверждение исключённых регистров

Sunsynk `single_phase.py` реально содержит адреса, которые мы исключили:
- 166 — Gen power (AUX port)
- 182 — Battery temperature
- 76-82 — Day grid import/export, Month grid/load/PV energy
- 248-279 — Program timers (Prog1-Prog6 time/power/capacity)

**Вывод:** исключение этих регистров из этапа 4 соответствует практике Sunsynk (они читаются отдельно или не во всех профилях). Никаких изменений в карте не требуется.

## 7. Рекомендации

1. **Temperature offset:** применить `raw × 0.1 − 100` для reg 90/91 (соответствует Sunsynk `TempSensor` с `offset=100` и kbialek). Требует сверки с температурой радиатора на дисплее инвертора.
2. **Battery Power sign:** уточнить знак (+434 W = заряд или разряд) против дисплея в момент синхронного измерения. Sunsynк конвенция: положительное = разряд; наша текущая интерпретация: положительное = заряд.
3. **Battery Current sign:** аналогично, требует сверки (0.81 A vs экран 0.88 A).
4. **Grid Power semantics:** reg 169 = 0 W при нагрузке дома 785 W на экране — семантика不明, требует уточнения.
5. **Код не менять:** адаптер и карта регистров остаются без изменений до получения подтверждения от пользователя.

## 8. Источники

- https://github.com/kellerza/sunsynk — `src/sunsynk/sensors.py` (модель Sensor, TempSensor, reg_to_value)
- https://github.com/kellerza/sunsynk — `src/sunsynk/definitions/single_phase.py` (определения однофазного гибрида)
- https://github.com/kellerza/sunsynk — `src/sunsynk/definitions/micro_hybrid_ess.py` (определения Deye micro hybrid ESS)
- https://github.com/kellerza/sunsynk — `src/sunsynk/definitions/__init__.py` (COMMON, ALL_DEFS, SensorDefinitions)
- Локальный `sunlink-svitgrid/server/deye_readonly_adapter.py` (карта регистров Deye SG02LP1)
- `docs/stages/stage-3-first-real-read.md`, `docs/stages/stage-4-modbus-display-verification.md` (результаты физических опросов)

## 9. GitHub

- Ветка: `main` (origin/main)
- Локальный HEAD: `4fa01e6113` = remote HEAD (пуш не требуется, новых файлов нет)
- Изменённые файлы: только `docs/stages/stage-5-sunsynk-research.md` (новый)
- Секреты в коммите: отсутствуют
- Тесты: 22/22 проходят (код не менялся)
