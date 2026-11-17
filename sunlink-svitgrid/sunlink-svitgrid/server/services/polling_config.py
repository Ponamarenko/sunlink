"""
SunPortal Data Layer — polling configuration.

Settings for the Modbus polling service cycle: intervals, timeouts,
retries and backoff. All values are configurable; nothing is hardcoded
to production values.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class PollingConfig:
    """Configuration for the Modbus polling cycle."""

    # Inverter identifier used in every persisted measurement.
    inverter_id: str = "2507245705"

    # Time between polling cycles (seconds). 0 means poll once and stop.
    polling_interval: float = 5.0

    # Timeout for establishing the Modbus TCP connection (seconds).
    connect_timeout: float = 2.0

    # Timeout for reading a single register (seconds).
    read_timeout: float = 2.0

    # Maximum number of retries for a failed register read within one cycle.
    max_register_retries: int = 1

    # Base for exponential backoff between retry attempts (seconds).
    retry_backoff_base: float = 0.5

    # Maximum backoff between retry attempts (seconds).
    retry_backoff_max: float = 2.0

    # Maximum consecutive failures before the inverter is considered offline.
    max_consecutive_failures: int = 3

    # Data freshness threshold (seconds). Older data is considered stale.
    data_freshness_seconds: float = 30.0

    def __post_init__(self) -> None:
        if self.polling_interval < 0:
            raise ValueError("polling_interval must be >= 0")
        if self.connect_timeout <= 0:
            raise ValueError("connect_timeout must be > 0")
        if self.read_timeout <= 0:
            raise ValueError("read_timeout must be > 0")
        if self.max_register_retries < 0:
            raise ValueError("max_register_retries must be >= 0")
        if self.max_consecutive_failures < 1:
            raise ValueError("max_consecutive_failures must be >= 1")
        if self.data_freshness_seconds <= 0:
            raise ValueError("data_freshness_seconds must be > 0")
