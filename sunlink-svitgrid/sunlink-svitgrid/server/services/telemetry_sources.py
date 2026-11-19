"""
SunLink — реализации TelemetrySource (этап 16).

Три источника возвращают один формат TelemetrySnapshot:
  * DemoTelemetrySource  — демо-значения, без сети;
  * MockTelemetrySource  — mock FC03-транспорт, без сети;
  * ModbusTelemetrySource — подготовленный read-only FC03-источник,
    НЕ активируется автоматически и в этапе 16 работает только с mock-транспортом.

Все источники строго read-only. Отсутствующие значения остаются None.
"""

from __future__ import annotations

from typing import Dict, Optional

from domain.models import ConfidenceLevel, DataSource, MeasurementQuality
from domain.normalizer import normalize_register
from domain.register_metadata import REGISTER_MAP
from domain.telemetry import (
    TelemetrySnapshot,
    TelemetrySource,
    TelemetryValue,
    utc_now_iso,
)
from modbus.mock_fc03_transport import Fc03Error, MockFc03Transport

INVERTER_ID = "deye_sg02lp1_am3"

# Демо-значения тех же 9 регистров этапа 7. Это НЕ реальные показания.
DEFAULT_DEMO_REGISTERS: Dict[int, int] = {
    90: 1365,    # dc_temperature  -> 36.5 °C
    91: 1424,    # ac_temperature  -> 42.4 °C
    183: 5322,   # battery_voltage -> 53.22 V
    184: 85,     # battery_soc     -> 85 %
    186: 211,    # pv1_power       -> 211 W
    187: 131,    # pv2_power       -> 131 W
    188: 107,    # pv3_power       -> 107 W
    190: 1915,   # battery_power   -> 1915 W (provisional sign)
    191: 361,    # battery_current -> 3.61 A
}


class DemoTelemetrySource(TelemetrySource):
    """Демо-источник: значения генерируются локально, без сети."""

    def __init__(
        self,
        registers: Optional[Dict[int, int]] = None,
        inverter_id: str = INVERTER_ID,
    ) -> None:
        self._registers = dict(registers or DEFAULT_DEMO_REGISTERS)
        self._inverter_id = inverter_id
        self._last: Optional[TelemetrySnapshot] = None

    @property
    def source(self) -> DataSource:
        return DataSource.DEMO_SIMULATOR

    @property
    def connection_ok(self) -> bool:
        return True

    def read(self) -> TelemetrySnapshot:
        ts = utc_now_iso()
        values: Dict[str, TelemetryValue] = {}
        for address, meta in REGISTER_MAP.items():
            raw = self._registers.get(address)
            if raw is None:
                values[meta.name] = TelemetryValue(
                    parameter_name=meta.name,
                    normalized_value=None,
                    unit=meta.unit,
                    timestamp=ts,
                    confidence=meta.confidence.value,
                    quality=MeasurementQuality.UNAVAILABLE.value,
                    raw_value=None,
                    register_address=address,
                )
                continue
            norm = normalize_register(address, raw, meta)
            values[meta.name] = TelemetryValue(
                parameter_name=meta.name,
                normalized_value=norm.normalized_value,
                unit=meta.unit,
                timestamp=ts,
                confidence=meta.confidence.value,
                quality=norm.quality.value if norm.ok else MeasurementQuality.ERROR.value,
                raw_value=raw,
                register_address=address,
            )
        self._last = TelemetrySnapshot(
            inverter_id=self._inverter_id,
            source=self.source,
            timestamp=ts,
            connection_ok=True,
            values=values,
        )
        return self._last

    def close(self) -> None:
        self._last = None


class _Fc03TelemetrySource(TelemetrySource):
    """Общий read-only FC03-читатель для mock и подготовленного modbus."""

    def __init__(
        self,
        transport: MockFc03Transport,
        source: DataSource,
        inverter_id: str = INVERTER_ID,
        unit_id: int = 1,
    ) -> None:
        self._transport = transport
        self._source = source
        self._inverter_id = inverter_id
        self._unit_id = unit_id
        self._last: Optional[TelemetrySnapshot] = None
        self._connection_ok = False

    @property
    def source(self) -> DataSource:
        return self._source

    @property
    def connection_ok(self) -> bool:
        return self._connection_ok

    def read(self) -> TelemetrySnapshot:
        ts = utc_now_iso()
        values: Dict[str, TelemetryValue] = {}
        errors: list[str] = []

        if not self._transport.connected:
            try:
                self._transport.connect()
            except Fc03Error as exc:
                errors.append(str(exc))

        for address, meta in sorted(REGISTER_MAP.items()):
            quality = MeasurementQuality.VALID.value
            raw: Optional[int] = None
            normalized: Optional[float] = None
            try:
                result = self._transport.read_holding_registers(
                    address, count=1, device_id=self._unit_id
                )
                if result.isError() or not result.registers:
                    quality = MeasurementQuality.UNAVAILABLE.value
                else:
                    raw = result.registers[0]
                    norm = normalize_register(address, raw, meta)
                    if norm.ok:
                        normalized = norm.normalized_value
                    else:
                        quality = MeasurementQuality.ERROR.value
                        errors.append(norm.error or "normalization error")
            except Fc03Error as exc:
                quality = MeasurementQuality.ERROR.value
                errors.append(str(exc))

            values[meta.name] = TelemetryValue(
                parameter_name=meta.name,
                normalized_value=normalized,
                unit=meta.unit,
                timestamp=ts,
                confidence=meta.confidence.value,
                quality=quality,
                raw_value=raw,
                register_address=address,
            )

        self._connection_ok = len([v for v in values.values() if v.present]) > 0
        self._last = TelemetrySnapshot(
            inverter_id=self._inverter_id,
            source=self._source,
            timestamp=ts,
            connection_ok=self._connection_ok,
            values=values,
            error="; ".join(dict.fromkeys(errors)) if errors else None,
        )
        return self._last

    def close(self) -> None:
        self._transport.close()
        self._connection_ok = False


class MockTelemetrySource(_Fc03TelemetrySource):
    """Mock-источник: только программный транспорт, без сети."""

    def __init__(self, transport: MockFc03Transport, unit_id: int = 1) -> None:
        super().__init__(transport, DataSource.MOCK, unit_id=unit_id)


class ModbusTelemetrySource(_Fc03TelemetrySource):
    """Подготовленный физический источник (deye_modbus).

    В этапе 16 используется ТОЛЬКО с mock-транспортом. Физическое подключение
    требует явной конфигурации и отдельного разрешения владельца.
    """

    def __init__(self, transport: MockFc03Transport, unit_id: int = 1) -> None:
        super().__init__(transport, DataSource.DEYE_MODBUS, unit_id=unit_id)


# Этап 24: реалистичные DEMO-сценарии для проверки Android без инвертора.
# Все значения явно помечены demo_simulator; это НЕ реальные измерения.
DEMO_SCENARIOS: Dict[str, Dict[int, Optional[int]]] = {
    "charging": dict(DEFAULT_DEMO_REGISTERS),
    "discharging": {
        90: 1410,    # 41 C с candidate offset
        91: 1482,    # 48 C
        183: 5353,   # 53.53 V
        184: 95,
        186: 1519,
        187: 796,
        188: 123,
        190: 0xF8C3,  # -1853 raw -> -185.3 W (provisional)
        191: 0xFEA1,  # -351 raw -> -3.51 A
    },
    "no_pv": {
        183: 5322,
        184: 70,
        186: 0,
        187: 0,
        188: 0,
        190: 1000,
        191: 100,
    },
    "partial": {
        183: 5322,
        184: 70,
        # PV и ток отсутствуют — остаются null, не 0.0
    },
}


def demo_source_for(scenario: str) -> DemoTelemetrySource:
    """DEMO-источник для заданного сценария; unknown → ValueError."""
    if scenario not in DEMO_SCENARIOS:
        raise ValueError(f"unknown demo scenario: {scenario}")
    registers = {k: v for k, v in DEMO_SCENARIOS[scenario].items() if v is not None}
    return DemoTelemetrySource(registers)


__all__ = [
    "DemoTelemetrySource",
    "MockTelemetrySource",
    "ModbusTelemetrySource",
    "DEFAULT_DEMO_REGISTERS",
    "DEMO_SCENARIOS",
    "demo_source_for",
    "INVERTER_ID",
]
