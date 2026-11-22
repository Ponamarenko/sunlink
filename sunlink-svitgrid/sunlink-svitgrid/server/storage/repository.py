"""
SunPortal Data Layer — storage interface.

The repository is independent of Home Assistant and does not require Redis.
"""

from __future__ import annotations

from abc import ABC, abstractmethod
from datetime import datetime
from typing import Any, Dict, List, Optional

from domain.models import Measurement


class MeasurementRepository(ABC):
    """Interface for persisting and querying measurement history."""

    @abstractmethod
    def save_measurement(self, measurement: Measurement) -> None:
        """Persist a single measurement."""

    @abstractmethod
    def save_measurements_batch(self, measurements: List[Measurement]) -> int:
        """Persist many measurements in one transaction. Returns count saved."""

    @abstractmethod
    def get_latest(
        self, inverter_id: str, parameter_name: str
    ) -> Optional[Measurement]:
        """Return the most recent measurement for a parameter."""

    @abstractmethod
    def get_history(
        self,
        inverter_id: str,
        parameter_name: str,
        start: datetime,
        end: datetime,
    ) -> List[Measurement]:
        """Return measurements for a parameter within [start, end] UTC."""

    def get_statistics(
        self,
        inverter_id: str,
        parameter_name: str,
        start: datetime,
        end: datetime,
    ) -> Dict[str, Any]:
        """Aggregate a parameter over [start, end]. Optional for implementations."""
        raise NotImplementedError("statistics are not supported by this repository")

    @abstractmethod
    def get_all_latest(self, inverter_id: str) -> List[Measurement]:
        """Return the latest measurement for every parameter of an inverter."""

    def prune_before(self, inverter_id: str, cutoff: datetime) -> int:
        """Delete measurements older than cutoff. Optional for implementations."""
        raise NotImplementedError("pruning is not supported by this repository")

    @abstractmethod
    def close(self) -> None:
        """Close the underlying connection."""

    @abstractmethod
    def __enter__(self) -> "MeasurementRepository":
        ...

    @abstractmethod
    def __exit__(self, exc_type, exc_val, exc_tb) -> None:
        ...
