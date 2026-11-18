"""
SunPortal Data Layer — typed measurement models.

All timestamps are UTC. Raw values are preserved independently of normalized
values so that measurement provenance is never lost, even if the normalization
formula is later revised.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum
from typing import Optional


class MeasurementQuality(str, Enum):
    """Technical availability of the measurement (can we read the register?)."""

    VALID = "valid"
    UNAVAILABLE = "unavailable"
    STALE = "stale"
    UNVERIFIED = "unverified"
    ERROR = "error"


class ConfidenceLevel(str, Enum):
    """Semantic trustworthiness of the register mapping for this inverter model.

    This is distinct from technical availability: a register can be read fine
    (VALID) while its meaning for the AM3 firmware remains provisional.
    """

    VERIFIED = "verified"
    CORROBORATED = "corroborated"
    PROVISIONAL = "provisional"
    UNVERIFIED = "unverified"


class DataSource(str, Enum):
    MODBUS_REGISTER = "modbus_register"
    CALCULATED = "calculated"
    MANUAL = "manual"
    # Изолированный DEMO-источник (этап 12) — не реальные показания инвертора.
    # Значение передаётся в Android и должно явно отображаться как DEMO.
    DEMO_SIMULATOR = "demo_simulator"


@dataclass(frozen=True)
class Measurement:
    """One normalized measurement with full provenance.

    ``raw_value`` and ``normalized_value`` are stored independently so that the
    original Modbus word is never lost even if the normalization formula is
    later revised.
    """

    inverter_id: str
    register_address: Optional[int]
    parameter_name: str
    raw_value: Optional[int]
    normalized_value: Optional[float]
    unit: str
    timestamp: datetime
    quality: MeasurementQuality
    confidence: ConfidenceLevel
    source: DataSource
    description: str = ""

    def __post_init__(self) -> None:
        # Ensure timestamp is timezone-aware UTC.
        if self.timestamp.tzinfo is None:
            object.__setattr__(
                self, "timestamp", self.timestamp.replace(tzinfo=timezone.utc)
            )

    def to_dict(self) -> dict:
        return {
            "inverter_id": self.inverter_id,
            "register_address": self.register_address,
            "parameter_name": self.parameter_name,
            "raw_value": self.raw_value,
            "normalized_value": self.normalized_value,
            "unit": self.unit,
            "timestamp": self.timestamp.isoformat(),
            "quality": self.quality.value,
            "confidence": self.confidence.value,
            "source": self.source.value,
            "description": self.description,
        }

    @classmethod
    def from_dict(cls, d: dict) -> "Measurement":
        ts = d["timestamp"]
        if isinstance(ts, str):
            ts = datetime.fromisoformat(ts)
        return cls(
            inverter_id=d["inverter_id"],
            register_address=d.get("register_address"),
            parameter_name=d["parameter_name"],
            raw_value=d.get("raw_value"),
            normalized_value=d.get("normalized_value"),
            unit=d["unit"],
            timestamp=ts,
            quality=MeasurementQuality(d["quality"]),
            confidence=ConfidenceLevel(d["confidence"]),
            source=DataSource(d["source"]),
            description=d.get("description", ""),
        )


@dataclass(frozen=True)
class NormalizedReading:
    """Result of normalizing one raw register value."""

    register_address: int
    parameter_name: str
    raw_value: int
    normalized_value: float
    unit: str
    formula: str
    confidence: ConfidenceLevel
    quality: MeasurementQuality = MeasurementQuality.VALID
