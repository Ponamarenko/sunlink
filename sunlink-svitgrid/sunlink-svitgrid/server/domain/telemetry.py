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

    def to_full_dict(self) -> Dict[str, Any]:
        """Полная категориальная модель телеметрии для API/Android.

        Каждое отсутствующее значение остаётся None — никогда 0.0.
        Расчётные поля помечаются confidence='calculated' и не выдаются
        за измеренные.
        """
        categories: Dict[str, Dict[str, Optional[str]]] = {
            "pv": {
                "pv1_voltage": "pv1_voltage", "pv1_current": "pv1_current",
                "pv1_power": "pv1_power", "pv2_voltage": "pv2_voltage",
                "pv2_current": "pv2_current", "pv2_power": "pv2_power",
                "pv3_voltage": "pv3_voltage", "pv3_current": "pv3_current",
                "pv3_power": "pv3_power", "daily_energy": "daily_pv_energy",
                "total_energy": "total_pv_energy",
            },
            "battery": {
                "soc": "battery_soc", "voltage": "battery_voltage",
                "current": "battery_current", "power": "battery_power",
                "temperature": "battery_temperature",
                "daily_charge_energy": "daily_battery_charge",
                "daily_discharge_energy": "daily_battery_discharge",
            },
            "grid": {
                "power": "grid_power", "voltage_l1": "grid_voltage_l1",
                "voltage_l2": "grid_voltage_l2", "frequency": "grid_frequency",
                "import_energy": "grid_import_energy",
                "export_energy": "grid_export_energy",
                "connected": "grid_connected",
            },
            "load": {
                "power": "load_power", "voltage": "load_voltage",
                "current": "load_current", "energy": "load_energy",
            },
            "inverter": {
                "dc_temperature": "dc_temperature",
                "ac_temperature": "ac_temperature",
                "state": "inverter_state",
            },
            "energy": {
                "daily_generation": "daily_pv_energy",
                "total_generation": "total_pv_energy",
                "daily_consumption": "load_energy",
                "daily_grid_import": "grid_import_energy",
                "daily_grid_export": "grid_export_energy",
            },
        }

        def item(name: str) -> Optional[Dict[str, Any]]:
            value = self.values.get(name)
            return value.to_dict() if value is not None else None

        result: Dict[str, Any] = {
            "source": self.source.value,
            "timestamp": self.timestamp,
            "connection_status": self.connection_ok,
            "stale": self.stale,
            "inverter_id": self.inverter_id,
            "categories": {
                category: {out_name: item(param) for out_name, param in fields.items()}
                for category, fields in categories.items()
            },
        }
        # p_sun: только из подтверждённых PV-мощностей; если их нет — None.
        pv_fields = result["categories"]["pv"]
        powers = [
            pv_fields[k]["normalized_value"]
            for k in ("pv1_power", "pv2_power", "pv3_power")
            if pv_fields[k] is not None and pv_fields[k]["normalized_value"] is not None
        ]
        pv_total = float(sum(powers)) if powers else None
        result["categories"]["pv"]["total_power"] = (
            {
                "raw_value": None, "normalized_value": pv_total,
                "unit": "W", "confidence": "calculated", "quality": "valid",
                "address": None, "timestamp": self.timestamp,
            }
            if pv_total is not None else None
        )
        return result

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
