"""
SunPortal Data Layer — inverter state model.

Tracks connection availability and data freshness independently.
A single failed register does not make the inverter offline; only
connection-level failures or a sustained loss of responses do.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Optional


class ConnectionStatus:
    """Inverter connection status constants."""

    UNKNOWN = "unknown"
    ONLINE = "online"
    OFFLINE = "offline"
    DEGRADED = "degraded"


@dataclass
class InverterState:
    """Current state of the inverter as observed by the polling service."""

    inverter_id: str
    connection_status: str = ConnectionStatus.UNKNOWN
    last_successful_poll: Optional[datetime] = None
    last_failed_poll: Optional[datetime] = None
    last_error: Optional[str] = None
    consecutive_failures: int = 0
    last_poll_duration_ms: Optional[float] = None
    last_successful_measurement: Optional[dict] = None
    data_freshness_seconds: float = 30.0

    def mark_successful_poll(
        self,
        duration_ms: float,
        measurement: Optional[dict] = None,
    ) -> None:
        """Record a successful poll cycle."""
        now = datetime.now(timezone.utc)
        self.connection_status = ConnectionStatus.ONLINE
        self.last_successful_poll = now
        self.last_error = None
        self.consecutive_failures = 0
        self.last_poll_duration_ms = duration_ms
        if measurement is not None:
            self.last_successful_measurement = measurement

    def mark_failed_poll(self, error: str) -> None:
        """Record a failed poll cycle."""
        now = datetime.now(timezone.utc)
        self.last_failed_poll = now
        self.last_error = error
        self.consecutive_failures += 1
        if self.consecutive_failures >= 3:
            self.connection_status = ConnectionStatus.OFFLINE
        else:
            self.connection_status = ConnectionStatus.DEGRADED

    def mark_degraded(self, reason: str) -> None:
        """Record partial success (some registers failed)."""
        now = datetime.now(timezone.utc)
        self.last_failed_poll = now
        self.last_error = reason
        if self.connection_status != ConnectionStatus.OFFLINE:
            self.connection_status = ConnectionStatus.DEGRADED

    def is_data_fresh(self) -> bool:
        """Return True if the last successful poll is within freshness threshold."""
        if self.last_successful_poll is None:
            return False
        age = (datetime.now(timezone.utc) - self.last_successful_poll).total_seconds()
        return age <= self.data_freshness_seconds

    def to_dict(self) -> dict:
        return {
            "inverter_id": self.inverter_id,
            "connection_status": self.connection_status,
            "last_successful_poll": (
                self.last_successful_poll.isoformat()
                if self.last_successful_poll is not None
                else None
            ),
            "last_failed_poll": (
                self.last_failed_poll.isoformat()
                if self.last_failed_poll is not None
                else None
            ),
            "last_error": self.last_error,
            "consecutive_failures": self.consecutive_failures,
            "last_poll_duration_ms": self.last_poll_duration_ms,
            "data_fresh": self.is_data_fresh(),
        }
