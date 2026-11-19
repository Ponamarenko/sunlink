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

    def to_read_all_dict(self) -> Dict[str, Any]:
        """Формат совместим с deye_gateway.read_all_registers() и Android DTO."""
        return {name: value.to_dict() for name, value in self.values.items()}

    def present_values(self) -> Dict[str, TelemetryValue]:
        return {k: v for k, v in self.values.items() if v.present}


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


__all__ = [
    "TelemetryValue",
    "TelemetrySnapshot",
    "TelemetrySource",
    "utc_now_iso",
]
