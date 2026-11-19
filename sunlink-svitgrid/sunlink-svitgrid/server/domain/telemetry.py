"""
SunLink — единый формат телеметрии для всех источников данных (этап 16).

Demo, mock и физический Modbus-источник возвращают один и тот же тип.
Отсутствующее измерение остаётся None — никогда не 0.0. Каждое значение несёт
timestamp, quality и confidence; источник помещается явно.
"""

from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any, Dict, Optional

from .models import DataSource


def utc_now_iso() -> str:
    """Единый формат timestamp: ISO-8601 UTC."""
    return datetime.now(timezone.utc).isoformat()


@dataclass(frozen=True)
class TelemetryValue:
    """Одно измерение в едином формате."""

    parameter_name: str
    normalized_value: Optional[float]
    unit: str
    timestamp: str
    confidence: str
    quality: str
    raw_value: Optional[int] = None
    register_address: Optional[int] = None

    @property
    def present(self) -> bool:
        return self.normalized_value is not None

    def to_dict(self) -> Dict[str, Any]:
        return {
            "raw_value": self.raw_value,
            "normalized_value": self.normalized_value,
            "unit": self.unit,
            "confidence": self.confidence,
            "quality": self.quality,
            "address": self.register_address,
            "timestamp": self.timestamp,
        }


@dataclass(frozen=True)
class TelemetrySnapshot:
    """Снимок телеметрии от одного источника."""

    inverter_id: str
    source: DataSource
    timestamp: str
    connection_ok: bool
    values: Dict[str, TelemetryValue]
    error: Optional[str] = None
    # Последние достоверные данные, показанные после потери связи (этап 20).
    stale: bool = False

    def to_read_all_dict(self) -> Dict[str, Any]:
        """Формат совместим с deye_gateway.read_all_registers() и Android DTO."""
        return {name: value.to_dict() for name, value in self.values.items()}

    def present_values(self) -> Dict[str, TelemetryValue]:
        return {k: v for k, v in self.values.items() if v.present}

    def value_of(self, parameter_name: str) -> Optional[float]:
        """Единый доступ к значению параметра; None — отсутствует (не 0.0)."""
        value = self.values.get(parameter_name)
        return value.normalized_value if value is not None else None

    # Явные accessors для новых реальных параметров (этап 24).
    @property
    def pv1_power(self) -> Optional[float]:
        return self.value_of("pv1_power")

    @property
    def pv2_power(self) -> Optional[float]:
        return self.value_of("pv2_power")

    @property
    def pv3_power(self) -> Optional[float]:
        return self.value_of("pv3_power")

    @property
    def battery_voltage(self) -> Optional[float]:
        return self.value_of("battery_voltage")

    @property
    def battery_current(self) -> Optional[float]:
        return self.value_of("battery_current")

    @property
    def battery_power(self) -> Optional[float]:
        return self.value_of("battery_power")

    @property
    def temperature_90(self) -> Optional[float]:
        return self.value_of("dc_temperature")

    @property
    def temperature_91(self) -> Optional[float]:
        return self.value_of("ac_temperature")

    @property
    def provisional_parameters(self) -> list[str]:
        """Имена параметров с PROVISIONAL confidence — для UI-маркировки."""
        return sorted(
            name for name, value in self.values.items()
            if value.confidence == "provisional"
        )


class TelemetrySource(ABC):
    """Общий интерфейс источника телеметрии.

    Строго read-only: интерфейс намеренно НЕ содержит никаких write-методов.
    """

    @property
    @abstractmethod
    def source(self) -> DataSource:
        """Метка источника (demo_simulator / mock / deye_modbus)."""

    @abstractmethod
    def read(self) -> TelemetrySnapshot:
        """Прочитать один снимок. Ошибки источника не превращаются в нули."""

    @abstractmethod
    def close(self) -> None:
        """Закрыть соединение, если оно есть."""

    @property
    def connection_ok(self) -> bool:
        return False

    # Если источник сам пишет в Data Layer, gateway не должен дублировать запись.
    persists_itself: bool = False


__all__ = [
    "TelemetryValue",
    "TelemetrySnapshot",
    "TelemetrySource",
    "utc_now_iso",
]
